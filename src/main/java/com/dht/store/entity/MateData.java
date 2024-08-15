package com.dht.store.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;

@Data
public class MateData {
    private String infoHash;
    private String name;
    private Long length;
    private List<File> files;
    private String torrent;
    private boolean isIllegal;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MateData mateData = (MateData) o;
        return Objects.equals(infoHash, mateData.infoHash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(infoHash);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class File{
        private List<String> path;
        private Long length;
    }
}
