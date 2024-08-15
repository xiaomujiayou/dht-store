package com.dht.store.entity;

import lombok.Data;

@Data
public class StoreInfo {

    /**
     * 种子数量
     */
    private long count;

    /**
     * 本次新增数量
     */
    private long addCount;

    /**
     * 违规资源数量
     */
    private long illegalCount;

    /**
     * 今日新增
     */
    private long today;

    /**
     * 最近一小时
     */
    private long hour;

    /**
     * 最近10分钟
     */
    private long tenMinutes;
}
