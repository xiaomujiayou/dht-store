package com.dht.store.downloader.bo;

import lombok.Data;

/**
 * 任务列表
 */
@Data
public class Task {

    /**
     * 任务ID
     */
    private String id;

    /**
     * 任务名称
     */
    private String name;

    /**
     * 任务hash
     */
    private String hash;

    /**
     * 任务hashV2
     */
    private String hashV2;

    /**
     * 任务添加时间戳
     */
    private Long addTime;

    /**
     * 下载状态
     */
    private Integer status;
    /**
     * 下载状态描述
     */
    private String statusStr;

    /**
     * 下载速度
     */
    private Long speed;

    /**
     * 下载速度描述
     */
    private String speedStr;

    /**
     * 下载进度
     */
    private float progress;

    /**
     * 排序
     */
    private Integer sort;
}
