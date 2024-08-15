package com.dht.store.controller;

import cn.hutool.core.util.ObjUtil;
import com.dht.store.entity.File;
import com.dht.store.entity.Torrent;
import com.dht.store.enums.MsgEnum;
import com.dht.store.es.ESTorrentRepo;
import com.dht.store.es.ESTorrentService;
import com.dht.store.utils.Msg;
import com.dht.store.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api")
public class SearchController {

    @Autowired
    private ESTorrentService esTorrentService;
    @Autowired
    private ESTorrentRepo esTorrentRepo;

    /**
     * 资源搜索
     *
     * @param searchModel 搜索模式
     * @param searchField 搜索类型
     * @param keywords    关键字
     * @param pageNum
     * @param pageSize
     * @return
     */
    @GetMapping("/search")
    public Msg<Page<Torrent>> searchByName(String searchModel, String searchField, String keywords, Integer pageNum, Integer pageSize) {
        PageRequest page = PageRequest.of(pageNum == null ? 0 : pageNum, pageSize == null ? 10 : pageSize);
        Page<Torrent> res = esTorrentService.search(searchModel, searchField, keywords, page);
        return R.sucess(res);
    }

    /**
     * 获取文件列表
     *
     * @param hash
     * @param path     文件路径
     * @param pageNum
     * @param pageSize
     * @return
     */
    @GetMapping("/fileList")
    public Msg<Page<File>> fileList(String hash, String path, Integer pageNum, Integer pageSize) {
        if (ObjUtil.hasEmpty(hash))
            return R.error(MsgEnum.PARAM_VALID_ERROR, "hash 不能为空！");
        Optional<Torrent> optionalTorrent = esTorrentRepo.findById(hash);
        if (!optionalTorrent.isPresent())
            return R.error(MsgEnum.PARAM_VALID_ERROR, "hash 无效！");
        pageNum = pageNum == null ? 0 : pageNum;
        pageSize = pageSize == null ? 10 : pageSize;
        path = path == null ? "" : path;
        Page<File> page = esTorrentService.fileList(path, pageNum, pageSize, optionalTorrent.get().getFiles());
        return R.sucess(page);
    }

}
