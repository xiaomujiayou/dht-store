package com.dht.store.enums;

import cn.hutool.core.util.EnumUtil;
import lombok.Getter;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 下载状态枚举
 */
@Getter
public enum QbTaskState {
    下载中(Arrays.asList("downloading", "metaDL", "queuedDL", "checkingResumeData", "stalledDL"), 1),
    暂停下载(Arrays.asList("pausedDL", "error"), 2),
    下载完成(Arrays.asList("pausedUP", "stalledUP", "queuedUP", "missingFiles","moving"), 3),
    ;

    QbTaskState(List<String> codes, Integer status) {
        this.codes = codes;
        this.status = status;
    }

    private List<String> codes;
    private Integer status;

    public static QbTaskState parse(String state) {
        LinkedHashMap<String, QbTaskState> enumMap = EnumUtil.getEnumMap(QbTaskState.class);
        return enumMap.values().stream().filter(o -> o.getCodes().contains(state)).findFirst().get();
    }
}
