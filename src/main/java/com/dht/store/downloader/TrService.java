package com.dht.store.downloader;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.IterUtil;
import cn.hutool.core.io.unit.DataSizeUtil;
import cn.hutool.core.map.MapBuilder;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.dht.store.downloader.bo.Config;
import com.dht.store.downloader.bo.Info;
import com.dht.store.downloader.bo.Task;
import com.dht.store.entity.Magnet;
import com.dht.store.entity.Torrent;
import com.dht.store.entity.UserConfig;
import com.dht.store.enums.DownloaderType;
import com.dht.store.enums.MsgEnum;
import com.dht.store.enums.TrTaskState;
import com.dht.store.es.ESTorrentRepo;
import com.dht.store.es.ESUserConfigRepo;
import com.dht.store.exception.ServiceException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHitSupport;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Transmission 下载器实现
 */
@Slf4j
@Service
public class TrService implements Downloader {
    private Config downloaderConfig;
    private String sessionId = null;
    @Autowired
    private ESTorrentRepo esTorrentRepo;
    @Autowired
    private ESUserConfigRepo esUserConfigRepo;
    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Override
    public void init() {
        this.sessionId = null;
        this.downloaderConfig = null;
        Iterable<UserConfig> allConfig = esUserConfigRepo.findAll();
        UserConfig userConfig = allConfig.iterator().next();
        if (CollUtil.isEmpty(userConfig.getDownloaderConfig()))
            return;
        downloaderConfig = userConfig.getDownloaderConfig().stream().filter(Config::getUse).filter(o -> o.getType() == DownloaderType.Transmission).findFirst().orElse(null);
        if (downloaderConfig != null)
            login();
    }

    @Override
    public Config getConfig() {
        return downloaderConfig;
    }

    @Override
    public Info getInfo() {
        Res sessionGet = post(new Req<>("session-get", MapUtil.empty()));
        Res sessionStats = post(new Req<>("session-stats", MapUtil.empty()));
        Info info = new Info();
        info.setDlTotalCount(sessionStats.arguments.getInteger("torrentCount"));
        info.setDlCount(sessionStats.arguments.getInteger("activeTorrentCount"));
        info.setDlCompleteCount(info.getDlTotalCount() - info.getDlCount());
        info.setFreeSpaceOnDisk(DataSizeUtil.format(sessionGet.getArguments().getLong("download-dir-free-space")));
        Long dlInfoSpeed = sessionStats.arguments.getLong("downloadSpeed");
        info.setDlSpeed(dlInfoSpeed <= 0 ? "0 B/s" : DataSizeUtil.format(dlInfoSpeed) + "/s");
        Long uploadSpeed = sessionStats.arguments.getLong("uploadSpeed");
        info.setUpSpeed(uploadSpeed <= 0 ? "0 B/s" : DataSizeUtil.format(uploadSpeed) + "/s");
        return info;
    }

    @Override
    public boolean test(Config downloaderConfig) {
        try {
            HttpResponse res = HttpUtil.createPost(downloaderConfig.getHost() + "/transmission/rpc")
                    .basicAuth(downloaderConfig.getUserName(), downloaderConfig.getPassword())
                    .execute();
            if (res.getStatus() != 409) {
                log.error("下载器：{} 登录失败，请检查账号密码是否正确！", downloaderConfig.getName());
                throw new ServiceException(MsgEnum.CONF_ERROR, "登录失败，请检查账号密码是否正确！");
            }
        } catch (Exception e) {
            log.error("下载器：{} 连接失败，请检查下载器配置是否正确！", downloaderConfig.getName());
            throw new ServiceException(MsgEnum.CONF_ERROR, "连接失败，请检查地址配置是否正确！");
        }
        return true;
    }

    @Override
    public boolean getStatus() {
        return this.sessionId != null;
    }

    @Scheduled(initialDelay = 5 * 1000, fixedDelay = 5 * 60 * 1000)
    @Override
    public void login() {
        this.sessionId = null;
        if (downloaderConfig == null)
            return;
        try {
            HttpResponse res = HttpUtil.createPost(downloaderConfig.getHost() + "/transmission/rpc")
                    .basicAuth(downloaderConfig.getUserName(), downloaderConfig.getPassword())
                    .execute();
            if (res.getStatus() != 409) {
                log.error("下载器：{} 登录失败，请检查账号密码是否正确！", downloaderConfig.getName());
                throw new ServiceException(MsgEnum.CONF_ERROR, "登录失败，请检查账号密码是否正确！");
            }
            this.sessionId = res.header("X-Transmission-Session-Id");
        } catch (Exception e) {
            log.error("下载器：{} 连接失败，请检查下载器配置是否正确！", downloaderConfig.getName());
            throw new ServiceException(MsgEnum.CONF_ERROR, "连接失败，请检查地址配置是否正确！");
        }
    }

    @Override
    public List<Torrent> getTorrentList(Integer status) {
        List<Task> taskList = getTaskList();
        if (status != null) {
            TrTaskState taskState = EnumUtil.getBy(TrTaskState::getStatus, status);
            if (taskState != null)
                taskList = taskList.stream().filter(task -> task.getStatus().equals(taskState.getStatus())).collect(Collectors.toList());
        }
        Set<String> collect = taskList.stream().map(Task::getHash).collect(Collectors.toSet());
        Criteria criteria = new Criteria("magnets.hash").in(collect);
        CriteriaQuery query = new CriteriaQuery(criteria);
        SearchHits<Torrent> search = elasticsearchRestTemplate.search(query, Torrent.class);
        List<Torrent> torrents = (List<Torrent>) SearchHitSupport.unwrapSearchHits(search);
        for (Torrent torrent : torrents) {
            mark:
            for (Magnet magnet : torrent.getMagnets()) {
                for (Task task : taskList) {
                    if (task.getHash().equals(magnet.getHash())) {
                        torrent.setTask(task);
                        torrent.setCreateTime(new Date(task.getAddTime() * 1000));
                        continue mark;
                    }
                }
            }
        }
        return torrents.stream().sorted(Comparator.<Torrent, Long>comparing(o -> o.getTask().getAddTime()).reversed()).collect(Collectors.toList());
    }

    @Override
    public List<Task> getTaskList() {
        Res res = post(new Req<>("torrent-get", MapUtil.builder().put("fields", Arrays.asList("id", "name", "status", "hashString", "totalSize", "percentDone", "addedDate", "trackerStats", "leftUntilDone", "rateDownload", "rateUpload", "recheckProgress", "rateDownload", "rateUpload", "peersGettingFromUs", "peersSendingToUs", "uploadRatio", "uploadedEver", "downloadedEver", "downloadDir", "error", "errorString", "doneDate", "queuePosition", "activityDate")).build()));
        JSONArray torrents = res.arguments.getJSONArray("torrents");
        if (ObjUtil.isEmpty(torrents))
            return Collections.emptyList();
        return torrents.stream().map(torrent -> {
            JSONObject json = (JSONObject) torrent;
            Task task = new Task();
            task.setId(json.getString("id"));
            task.setName(json.getString("name"));
            task.setHash(json.getString("hashString"));
//            task.setHashV2(json.getString("infohash_v2"));
            task.setAddTime(json.getLong("addedDate"));
            TrTaskState state = TrTaskState.parse(json.getString("status"));
            task.setStatus(state.getStatus());
            task.setStatusStr(state.name());
            task.setSpeed(json.getLong("rateDownload"));
            task.setSpeedStr(task.getSpeed() <= 0 ? "0 B/s" : DataSizeUtil.format(task.getSpeed()) + "/s");
            task.setProgress(BigDecimal.valueOf(json.getFloatValue("percentDone")).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP).floatValue());
            task.setSort(json.getInteger("id"));
            return task;
        }).sorted(Comparator.comparing(Task::getAddTime).reversed()).collect(Collectors.toList());
    }

    @Override
    public List<Torrent> listByHash(String... hash) {
        if (ObjUtil.isEmpty(hash))
            return Collections.emptyList();
        Iterable<Torrent> torrents = esTorrentRepo.findAllById(Arrays.asList(hash));
        return IterUtil.toList(torrents);
    }

    @Override
    public void download(List<String> hashes) {
        hashes.stream().map(hash -> "magnet:?xt=urn:btih:" + hash).forEach(magnet -> {
            post(new Req<>("torrent-add", MapBuilder.create()
                    .put("filename", magnet)
                    .put("paused", false)
                    .build()));
        });
    }

    @Override
    public void start(List<String> keys) {
        post(new Req<>("torrent-start", MapBuilder.create()
                .put("ids", keys)
                .build()));
    }

    @Override
    public void stop(List<String> keys) {
        post(new Req<>("torrent-stop", MapBuilder.create()
                .put("ids", keys)
                .build()));
    }

    @Override
    public void remove(List<String> keys) {
        post(new Req<>("torrent-remove", MapBuilder.create()
                .put("ids", keys)
                .put("delete-local-data", true)
                .build()));
    }

    @Override
    public void addTracker(String key, List<String> trackers) {

    }

    private Res post(Req<?> req) {
        if (downloaderConfig == null)
            throw new ServiceException(MsgEnum.CONF_NOT_FOUND, "下载器尚未配置");
        if (this.sessionId == null)
            throw new ServiceException(MsgEnum.CONF_ERROR, "下载器登录失败");
        HttpRequest request = HttpUtil
                .createPost(downloaderConfig.getHost() + "/transmission/rpc")
                .basicAuth(downloaderConfig.getUserName(), downloaderConfig.getPassword())
                .header("X-Transmission-Session-Id", sessionId)
                .body(req.toString());
        HttpResponse response = request.execute();
        return JSON.parseObject(response.body(), Res.class);
    }

    @Data
    @AllArgsConstructor
    static class Req<T> {
        private String method;
        private T arguments;
        private String tag;

        public Req(String method, T arguments) {
            this.method = method;
            this.arguments = arguments;
        }

        @Override
        public String toString() {
            return JSON.toJSONString(this);
        }


    }

    @Data
    static class Res {
        private JSONObject arguments;
        private String result;
    }
}
