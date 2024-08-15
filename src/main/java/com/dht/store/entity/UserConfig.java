package com.dht.store.entity;

import com.dht.store.downloader.bo.Config;
import lombok.Data;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Mapping;

import java.util.List;

@Data
@Document(indexName = "user-config")
@Mapping(mappingPath = "mapping/es-config.json")
public class UserConfig extends UserInfo{

    /**
     * 下载器配置
     */
    private List<Config> downloaderConfig;
}
