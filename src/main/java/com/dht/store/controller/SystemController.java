package com.dht.store.controller;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.map.MapBuilder;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.dht.store.downloader.bo.Info;
import com.dht.store.entity.QueueStatus;
import com.dht.store.entity.StoreInfo;
import com.dht.store.entity.Torrent;
import com.dht.store.es.ESTorrentRepo;
import com.dht.store.message.DhtMessageReceiver;
import com.dht.store.utils.DLUtil;
import com.dht.store.utils.Msg;
import com.dht.store.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.RoundingMode;
import java.util.Comparator;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api")
public class SystemController {

    @Autowired
    private RabbitProperties rabbitProperties;
    @Autowired
    private ESTorrentRepo esTorrentRepo;
    @Value("${spring.rabbitmq.admin-port}")
    private Integer adminPort;
    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    private static final Date SYSTEM_RUN_TIME = new Date();

    /**
     * 下载器详情
     */
    @GetMapping("/system/store/info")
    public Msg<StoreInfo> storeInfo() {
        StoreInfo storeInfo = new StoreInfo();
        storeInfo.setCount(esTorrentRepo.count());
        //本次启动新增
        RangeQueryBuilder add = QueryBuilders
                .rangeQuery("magnets.createTime")
                .gte(SYSTEM_RUN_TIME.getTime());
        NativeSearchQuery addQuery = new NativeSearchQueryBuilder()
                .withQuery(add)
                .build();
        long addCount = elasticsearchRestTemplate.count(addQuery, Torrent.class);
        storeInfo.setAddCount(addCount);
        //违规资源统计
        storeInfo.setIllegalCount(DhtMessageReceiver.ILLEGAL_COUNT);
        //最近一天
        RangeQueryBuilder today = QueryBuilders
                .rangeQuery("magnets.createTime")
                .gte(DateUtil.beginOfDay(new Date()).getTime());
        NativeSearchQuery todayQuery = new NativeSearchQueryBuilder()
                .withQuery(today)
                .build();
        long toDayCount = elasticsearchRestTemplate.count(todayQuery, Torrent.class);
        storeInfo.setToday(toDayCount);
        //最近一小时
        RangeQueryBuilder hour = QueryBuilders
                .rangeQuery("magnets.createTime")
                .gte(DateUtil.offset(new Date(), DateField.HOUR, -1).getTime());
        NativeSearchQuery hourQuery = new NativeSearchQueryBuilder()
                .withQuery(hour)
                .build();
        long hourCount = elasticsearchRestTemplate.count(hourQuery, Torrent.class);
        storeInfo.setHour(hourCount);
        //最近一分钟
        RangeQueryBuilder tenMinutes = QueryBuilders
                .rangeQuery("magnets.createTime")
                .gte(DateUtil.offset(new Date(), DateField.MINUTE, -1).getTime());
        NativeSearchQuery tenMinutesQuery = new NativeSearchQueryBuilder()
                .withQuery(tenMinutes)
                .build();
        long tenMinutesCount = elasticsearchRestTemplate.count(tenMinutesQuery, Torrent.class);
        storeInfo.setTenMinutes(tenMinutesCount);
        return R.sucess(storeInfo);
    }

    /**
     * 下载器详情
     */
    @GetMapping("/system/downloader/info")
    public Msg<Info> downloaderInfo() {
        return R.sucess(DLUtil.getDownloader().getInfo());
    }

    /**
     * 队列详情
     */
    @GetMapping("/system/status/{queue}")
    public Msg<QueueStatus> systemStatus(@PathVariable String queue, Integer age, Integer incr) {
        String apiUrl = "http://" + rabbitProperties.getHost() + ":" + adminPort + "/api/queues/" + URLUtil.encodeAll(rabbitProperties.getVirtualHost() == null ? "/" : rabbitProperties.getVirtualHost()) + "/" + URLUtil.encodeAll(queue);
        HttpRequest req = HttpUtil.createGet(apiUrl)
                .header("Authorization", "Basic " + Base64.encode(rabbitProperties.getUsername() + ":" + rabbitProperties.getPassword()))
                .form(MapBuilder.<String, Object>create()
                        .put("lengths_age", age)
                        .put("lengths_incr", incr)
                        .put("msg_rates_age", age)
                        .put("msg_rates_incr", incr)
                        .put("data_rates_age", age)
                        .put("data_rates_incr", incr)
                        .build());
        HttpResponse res = req.execute();
        return R.sucess(toSystemStatus(res.body(), incr));
    }

    private QueueStatus toSystemStatus(String body, Integer incr) {
        JSONObject resJson = JSON.parseObject(body);
        JSONObject messageStats = resJson.getJSONObject("message_stats");
        if(messageStats == null){
            return new QueueStatus().empty();
        }
        QueueStatus queueStatus = new QueueStatus();
        queueStatus.setHeap(resJson.getInteger("messages_ready"));
        JSONObject publishDetails = messageStats.getJSONObject("publish_details");
        queueStatus.setIn(publishDetails.getInteger("rate"));
        queueStatus.setInRate(publishDetails.getInteger("avg_rate"));
        queueStatus.setInSample(publishDetails.getList("samples", QueueStatus.Sample.class));
        JSONObject deliverDetails = messageStats.getJSONObject("deliver_details");
        queueStatus.setOut(deliverDetails.getInteger("rate"));
        queueStatus.setOutRate(deliverDetails.getInteger("avg_rate"));
        queueStatus.setOutSample(deliverDetails.getList("samples", QueueStatus.Sample.class));
        //处理图表数据
        for (int i = 0; i < queueStatus.getInSample().size(); i++) {
            QueueStatus.Sample sample = queueStatus.getInSample().get(i);
            Integer value;
            if (i < queueStatus.getInSample().size() - 1) {
                value = queueStatus.getInSample().get(i).getSample() - queueStatus.getInSample().get(i + 1).getSample();
                value = (int) NumberUtil.div((double) value, incr.doubleValue(), 0, RoundingMode.HALF_UP);
            } else {
                value = null;
            }
            sample.setSample(value);
            sample.setDate(DateUtil.date(sample.getTimestamp()).toString("HH:mm:ss"));
        }
        queueStatus.setInSample(queueStatus.getInSample().stream().filter(o -> o.getSample() != null).sorted(Comparator.comparing(QueueStatus.Sample::getTimestamp)).collect(Collectors.toList()));

        for (int i = 0; i < queueStatus.getOutSample().size(); i++) {
            QueueStatus.Sample sample = queueStatus.getOutSample().get(i);
            Integer value;
            if (i < queueStatus.getOutSample().size() - 1) {
                value = queueStatus.getOutSample().get(i).getSample() - queueStatus.getOutSample().get(i + 1).getSample();
                value = (int) NumberUtil.div((double) value, incr.doubleValue(), 0, RoundingMode.HALF_UP);
            } else {
                value = null;
            }
            sample.setSample(value);
            sample.setDate(DateUtil.date(sample.getTimestamp()).toString("HH:mm:ss"));
        }
        queueStatus.setOutSample(queueStatus.getOutSample().stream().filter(o -> o.getSample() != null).sorted(Comparator.comparing(QueueStatus.Sample::getTimestamp)).collect(Collectors.toList()));
        return queueStatus;
    }
}
