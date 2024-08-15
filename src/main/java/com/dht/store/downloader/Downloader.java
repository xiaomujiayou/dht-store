package com.dht.store.downloader;

import com.dht.store.downloader.bo.Config;
import com.dht.store.downloader.bo.Info;
import com.dht.store.downloader.bo.Task;
import com.dht.store.entity.Torrent;

import java.util.List;

public interface Downloader {

    /**
     * 初始化下载器
     * 下载器更新、切换时触发
     */
    void init();

    Config getConfig();

    Info getInfo();

    /**
     * 测试下载器是否可用
     */
    boolean test(Config downloaderConfig);

    /**
     * 获取下载器状态
     * @return
     */
    boolean getStatus();
    /**
     * 登录
     */
    void login();

    /**
     * 下载列表
     *
     * @return
     */
    List<Torrent> getTorrentList(Integer status);

    /**
     * 下载列表
     *
     * @return
     */
    List<Task> getTaskList();

    /**
     * 根据hash查询列表
     *
     * @param hash
     * @return
     */
    List<Torrent> listByHash(String... hash);

    /**
     * 下载
     *
     * @param hashes
     */
    void download(List<String> hashes);

    /**
     * 开始任务
     *
     * @param keys
     */
    void start(List<String> keys);

    /**
     * 暂停任务
     *
     * @param keys
     */
    void stop(List<String> keys);

    /**
     * 移除任务
     *
     * @param keys
     */
    void remove(List<String> keys);

    /**
     * 添加tracker服务器
     *
     * @param key
     * @param trackers
     */
    void addTracker(String key, List<String> trackers);


}
