package com.dht.store.utils;

import cn.hutool.extra.spring.SpringUtil;
import com.dht.store.downloader.Downloader;
import com.dht.store.downloader.bo.Config;
import com.dht.store.enums.MsgEnum;
import com.dht.store.exception.ServiceException;

public class DLUtil {
    private static Downloader downloader = null;

    public static void updateDownloader(Config downloaderConfig) {
        Downloader loader = SpringUtil.getBean(downloaderConfig.getType().getDownloaderClass());
        loader.init();
        downloader = loader;
    }

    public static Downloader getDownloader() {
        if (downloader == null)
            throw new ServiceException(MsgEnum.CONF_NOT_FOUND, "无可用的下载器");
        return downloader;
    }
    public static boolean test(Config downloaderConfig) {
        Downloader loader = SpringUtil.getBean(downloaderConfig.getType().getDownloaderClass());
        return loader.test(downloaderConfig);
    }
}
