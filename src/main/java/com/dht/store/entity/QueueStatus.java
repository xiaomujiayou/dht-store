package com.dht.store.entity;

import cn.hutool.core.date.DateUtil;
import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Data
public class QueueStatus {

    /**
     * 生产速度
     */
    private int in;
    /**
     * 平均生产速度
     */
    private int inRate;
    /**
     * 消费速度
     */
    private int out;
    /**
     * 平均消费速度
     */
    private int outRate;
    /**
     * 消息堆积
     */
    private int heap;
    /**
     * 历史生产数据
     */
    private List<Sample> inSample;
    /**
     * 历史消费数据
     */
    private List<Sample> outSample;

    public QueueStatus empty() {
        inSample = new ArrayList<>();
        outSample = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Sample sample = new Sample();
            sample.setSample(0);
            sample.setTimestamp(System.currentTimeMillis() - (i * 60 * 1000));
            sample.setDate(DateUtil.date(sample.getTimestamp()).toString("HH:mm:ss"));
            inSample.add(sample);
            outSample.add(sample);
        }
        return this;
    }

    @Data
    public static class Sample {
        private Integer sample;
        private long timestamp;
        private String date;
    }
}
