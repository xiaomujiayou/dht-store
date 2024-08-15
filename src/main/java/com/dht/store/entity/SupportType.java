package com.dht.store.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 支持的下载器
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SupportType {
    private String name;
    private String icon;
}
