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
public enum TrTaskState {
    下载中(Arrays.asList("4"), 1),
    暂停下载(Arrays.asList("0", "1", "2", "3"), 2),
    下载完成(Arrays.asList("5", "6"), 3),
    ;

    TrTaskState(List<String> codes, Integer status) {
        this.codes = codes;
        this.status = status;
    }

    private List<String> codes;
    private Integer status;

    public static TrTaskState parse(String state) {
        LinkedHashMap<String, TrTaskState> enumMap = EnumUtil.getEnumMap(TrTaskState.class);
        return enumMap.values().stream().filter(o -> o.getCodes().contains(state)).findFirst().get();
    }
}
