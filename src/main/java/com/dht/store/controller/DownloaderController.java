package com.dht.store.controller;

import cn.hutool.core.util.EnumUtil;
import cn.hutool.core.util.StrUtil;
import com.dht.store.downloader.bo.Task;
import com.dht.store.entity.SupportType;
import com.dht.store.entity.Torrent;
import com.dht.store.enums.DownloaderType;
import com.dht.store.exception.ServiceException;
import com.dht.store.utils.DLUtil;
import com.dht.store.utils.Msg;
import com.dht.store.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/downloader")
public class DownloaderController {

    /**
     * 支持的下载器类型
     */
    @GetMapping("/support")
    public Msg<List<SupportType>> support() {
        return R.sucess(EnumUtil.getEnumMap(DownloaderType.class).values().stream().map(
                downloaderType -> new SupportType(downloaderType.name(), downloaderType.getIconSvg())
        ).collect(Collectors.toList()));
    }

    /**
     * 下载种子列表
     *
     * @param status 下载状态
     * @return
     */
    @GetMapping("/torrents")
    public Msg<List<Torrent>> torrents(Integer status) {
        return R.sucess(DLUtil.getDownloader().getTorrentList(status));
    }

    /**
     * 下载任务列表
     */
    @GetMapping("/list")
    public Msg<List<Task>> list() {
        try {
            return R.sucess(DLUtil.getDownloader().getTaskList());
        } catch (ServiceException e) {
            return R.sucess(Collections.emptyList());
        }
    }

    /**
     * 根据Hash查询下载任务
     *
     * @param hash 多个“,”号分隔
     * @return
     */
    @GetMapping("/listByHash")
    public Msg<List<Torrent>> listByHash(String hash) {
        if (StrUtil.isBlank(hash))
            return R.sucess(Collections.emptyList());
        return R.sucess(DLUtil.getDownloader().listByHash(hash.split(",")));
    }

    /**
     * 添加下载任务
     *
     * @param hashes 多个“,”号分隔
     * @return
     */
    @GetMapping("/download")
    public Msg<?> download(String hashes) {
        if (StrUtil.isBlank(hashes))
            return R.sucess();
        DLUtil.getDownloader().download(Arrays.asList(hashes.split(",")));
        return R.sucess();
    }

    /**
     * 启动下载任务
     *
     * @param hashes 多个“,”号分隔
     * @return
     */
    @GetMapping("/start")
    public Msg<?> start(String hashes) {
        if (StrUtil.isBlank(hashes))
            return R.sucess();
        DLUtil.getDownloader().start(Arrays.asList(hashes.split(",")));
        return R.sucess();
    }

    /**
     * 暂停下载任务
     *
     * @param hashes 多个“,”号分隔
     * @return
     */
    @GetMapping("/stop")
    public Msg<?> stop(String hashes) {
        if (StrUtil.isBlank(hashes))
            return R.sucess();
        DLUtil.getDownloader().stop(Arrays.asList(hashes.split(",")));
        return R.sucess();
    }

    /**
     * 删除下载任务
     *
     * @param hashes 多个“,”号分隔
     * @return
     */
    @GetMapping("/remove")
    public Msg<?> remove(String hashes) {
        if (StrUtil.isBlank(hashes))
            return R.sucess();
        DLUtil.getDownloader().remove(Arrays.asList(hashes.split(",")));
        return R.sucess();
    }
}
