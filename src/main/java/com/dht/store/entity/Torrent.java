package com.dht.store.entity;

import com.dht.store.downloader.bo.Task;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.Mapping;

import java.util.Date;
import java.util.List;

@Data
@Document(indexName = "torrent")
@Mapping(mappingPath = "mapping/es-torrent.json")
public class Torrent {
    @Id
    private String hash;
    @Field
    private String name;
    @Field
    private Long length;
    @Field
    private byte[] torrent;
    @Field
    private Long hot;
    @Field
    private Date createTime;
    @Field
    private Date updateTime;
    @Field
    private List<MateData.File> files;
    @Field
    private List<Magnet> magnets;
    @Field(excludeFromSource = true)
    private Task task;
    @Field(excludeFromSource = true)
    private List<String> highlightFiles;
}
