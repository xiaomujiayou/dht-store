package com.dht.store.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SearchField {
    NAME("name"),
    FILES("files.path");

    private String fieldName;
}
