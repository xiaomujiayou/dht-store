package com.dht.store.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Field;

import java.util.Date;

@Data
//@Document(indexName = "magnet")
//@Mapping(mappingPath = "mapping/es-torrent.json")
public class Magnet {

    @Id
    private String hash;

    @Field
    private long hot;

    @Field
    private Date createTime;

    @Field
    private Date updateTime;

}
