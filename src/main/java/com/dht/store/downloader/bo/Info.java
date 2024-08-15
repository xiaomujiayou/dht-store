package com.dht.store.downloader.bo;

import lombok.Data;


/**
 * downloader info
 */
@Data
public class Info {

    /**
     * 下载任务总数
     */
    private long dlTotalCount;

    /**
     * 正在下载任务数量
     */
    private long dlCount;

    /**
     * 下载完成任务数量
     */
    private long dlCompleteCount;

    /**
     * 磁盘剩余空间
     */
    private String freeSpaceOnDisk;

    /**
     * 下载速度
     */
    private String dlSpeed;

    /**
     * 上传速度
     */
    private String upSpeed;
}
