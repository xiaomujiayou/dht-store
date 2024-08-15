package com.dht.store.downloader;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.IterUtil;
import cn.hutool.core.io.unit.DataSizeUtil;
import cn.hutool.core.map.MapBuilder;
import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.Method;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.dht.store.downloader.bo.Config;
import com.dht.store.downloader.bo.Info;
import com.dht.store.downloader.bo.Task;
import com.dht.store.entity.Magnet;
import com.dht.store.entity.Torrent;
import com.dht.store.entity.UserConfig;
import com.dht.store.enums.DownloaderType;
import com.dht.store.enums.MsgEnum;
import com.dht.store.enums.QbTaskState;
import com.dht.store.es.ESTorrentRepo;
import com.dht.store.es.ESUserConfigRepo;
import com.dht.store.exception.ServiceException;
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
import java.net.HttpCookie;
import java.util.*;
import java.util.stream.Collectors;

/**
 * QBittorrent 下载器实现
 */
@Slf4j
@Service
public class QbService implements Downloader {
    private Config downloaderConfig;
    private List<HttpCookie> cookies;
    @Autowired
    private ESTorrentRepo esTorrentRepo;
    @Autowired
    private ESUserConfigRepo esUserConfigRepo;
    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Override
    public void init() {
        this.cookies = null;
        this.downloaderConfig = null;
        Iterable<UserConfig> allConfig = esUserConfigRepo.findAll();
        UserConfig userConfig = allConfig.iterator().next();
        if (CollUtil.isEmpty(userConfig.getDownloaderConfig()))
            return;
        downloaderConfig = userConfig.getDownloaderConfig().stream().filter(Config::getUse).filter(o -> o.getType() == DownloaderType.qBittorrent).findFirst().orElse(null);
        if (downloaderConfig != null)
            login();
    }

    @Override
    public Config getConfig() {
        return downloaderConfig;
    }

    @Override
    public Info getInfo() {
        String res = get("/api/v2/sync/maindata");
        JSONObject jsonObject = JSON.parseObject(res);
        JSONObject serverState = jsonObject.getJSONObject("server_state");
        if (ObjUtil.isEmpty(serverState))
            throw new ServiceException(MsgEnum.UNKNOWN_ERROR);
        List<Task> tasks = getTaskList(jsonObject.getJSONObject("torrents"));
        Info info = new Info();
        info.setDlTotalCount(tasks.size());
        info.setDlCount(tasks.stream().filter(o -> Objects.equals(o.getStatus(), QbTaskState.下载中.getStatus())).count());
        info.setDlCompleteCount(tasks.stream().filter(o -> Objects.equals(o.getStatus(), QbTaskState.下载完成.getStatus())).count());
        info.setFreeSpaceOnDisk(DataSizeUtil.format(serverState.getLong("free_space_on_disk")));
        Long dlInfoSpeed = serverState.getLong("dl_info_speed");
        info.setDlSpeed(dlInfoSpeed <= 0 ? "0 B/s": DataSizeUtil.format(serverState.getLong("dl_info_speed")) + "/s");
        info.setUpSpeed(DataSizeUtil.format(serverState.getLong("up_info_speed")) + "/s");
        return info;
    }

    @Scheduled(initialDelay = 5 * 1000, fixedDelay = 5 * 60 * 1000)
    @Override
    public void login() {
        cookies = null;
        if (downloaderConfig == null)
            return;
        cookies = getCookies(downloaderConfig);
    }

    @Override
    public boolean test(Config downloaderConfig) {
        getCookies(downloaderConfig);
        return true;
    }

    @Override
    public boolean getStatus() {
        return cookies != null && !cookies.isEmpty();
    }

    public List<HttpCookie> getCookies(Config downloaderConfig) {
        try {
            HttpRequest request = new HttpRequest(downloaderConfig.getHost() + "/api/v2/auth/login");
            HttpResponse response = request
                    .setMethod(Method.POST)
                    .form("username", downloaderConfig.getUserName())
                    .form("password", downloaderConfig.getPassword())
                    .execute();
            List<HttpCookie> cookies = response.getCookies();
            if (CollUtil.isEmpty(cookies) || cookies.stream().noneMatch(cookie -> cookie.getName().equals("SID"))) {
                String body = response.body();
                if (body != null && body.contains("IP")) {
                    log.error("下载器：{} {}", downloaderConfig.getName(), body);
                    throw new ServiceException(MsgEnum.CONF_ERROR, body);
                }
                log.error("下载器：{} 登录失败，请检查账号密码是否正确！", downloaderConfig.getName());
                throw new ServiceException(MsgEnum.CONF_ERROR, "登录失败，请检查账号密码是否正确！");
            }
            return cookies;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("下载器：{} 连接失败，请检查下载器配置是否正确！", downloaderConfig.getName());
            throw new ServiceException(MsgEnum.CONF_ERROR, "连接失败，请检查地址配置是否正确！");
        }
    }

    @Override
    public List<Torrent> getTorrentList(Integer status) {
        List<Task> taskList = getTaskList();
        if (status != null) {
            QbTaskState taskState = EnumUtil.getBy(QbTaskState::getStatus, status);
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

    public List<Task> getTaskList() {
        return getTaskList(null);
    }

    public List<Task> getTaskList(JSONObject torrents) {
        if (torrents == null) {
            String res = get("/api/v2/sync/maindata");
            torrents = JSON.parseObject(res).getJSONObject("torrents");
        }
        if (ObjUtil.isEmpty(torrents))
            return Collections.emptyList();
        return torrents.entrySet().stream().map(torrent -> {
            JSONObject json = (JSONObject) torrent.getValue();
            Task task = new Task();
            task.setId(json.getString("priority"));
            task.setName(json.getString("name"));
            task.setHash(json.getString("infohash_v1"));
            task.setHashV2(json.getString("infohash_v2"));
            task.setAddTime(json.getLong("added_on"));
            QbTaskState state = QbTaskState.parse(json.getString("state"));
            task.setStatus(state.getStatus());
            task.setStatusStr(state.name());
            task.setSpeed(json.getLong("dlspeed"));
            task.setSpeedStr(task.getSpeed() <= 0 ? "0 B/s" : DataSizeUtil.format(task.getSpeed()) + "/s");
            task.setProgress(BigDecimal.valueOf(json.getFloatValue("progress")).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP).floatValue());
            task.setSort(json.getInteger("priority"));
            return task;
        }).sorted(Comparator.comparing(Task::getAddTime).reversed()).collect(Collectors.toList());
    }

    public List<Torrent> listByHash(String... hash) {
        if (ObjUtil.isEmpty(hash))
            return Collections.emptyList();
        Iterable<Torrent> torrents = esTorrentRepo.findAllById(Arrays.asList(hash));
        return IterUtil.toList(torrents);
    }

    public void download(List<String> hashes) {
        String urls = hashes.stream().map(hash -> "magnet:?xt=urn:btih:" + hash).collect(Collectors.joining("\r\n"));
        post("/api/v2/torrents/add", MapBuilder.<String, Object>create()
                .put("urls", urls)
                .build());

    }

    public void start(List<String> keys) {
        if (ObjUtil.isEmpty(keys))
            return;
        post("/api/v2/torrents/resume", MapBuilder.<String, Object>create()
                .put("hashes", String.join("|", keys))
                .build());
    }

    public void stop(List<String> keys) {
        if (ObjUtil.isEmpty(keys))
            return;
        post("/api/v2/torrents/pause", MapBuilder.<String, Object>create()
                .put("hashes", String.join("|", keys))
                .build());
    }

    public void remove(List<String> keys) {
        if (ObjUtil.isEmpty(keys))
            return;
        post("/api/v2/torrents/delete", MapBuilder.<String, Object>create()
                .put("hashes", String.join("|", keys))
                .put("deleteFiles", true)
                .build());
    }

    @Override
    public void addTracker(String key, List<String> trackers) {
    }

    private String get(String api) {
        if (downloaderConfig == null)
            throw new ServiceException(MsgEnum.CONF_NOT_FOUND, "下载器尚未配置");
        if (cookies == null)
            throw new ServiceException(MsgEnum.CONF_ERROR, "下载器登录失败");
        HttpRequest request = new HttpRequest(downloaderConfig.getHost() + api);
        String res = request
                .setMethod(Method.GET)
                .setConnectionTimeout(1500)
                .setReadTimeout(3000)
                .cookie(cookies)
                .execute().body();
        if (res.contains("Forbidden")) {
            log.error("下载器：{} 登录失败，请检查下载器配置是否正确！", downloaderConfig.getName());
            throw new ServiceException(MsgEnum.CONF_ERROR, "下载器：{} 登录失败，请检查下载器配置是否正确！");
        }
        return res;
    }

    private String post(String api, Map<String, Object> params) {
        if (downloaderConfig == null)
            throw new ServiceException(MsgEnum.CONF_NOT_FOUND, "下载器尚未配置");
        if (cookies == null)
            throw new ServiceException(MsgEnum.CONF_ERROR, "下载器登录失败");
        HttpRequest request = new HttpRequest(downloaderConfig.getHost() + api);
        String res = request
                .setMethod(Method.POST)
                .setConnectionTimeout(1500)
                .setReadTimeout(3000)
                .cookie(cookies)
                .form(params)
                .execute().body();
        if (res.contains("Forbidden")) {
            log.error("下载器：{} 登录失败，请检查下载器配置是否正确！", downloaderConfig.getName());
            throw new ServiceException(MsgEnum.CONF_ERROR, "下载器：{} 登录失败，请检查下载器配置是否正确！");
        }
        return res;
    }
}
