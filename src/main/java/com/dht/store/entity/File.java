package com.dht.store.entity;

import cn.hutool.core.io.unit.DataSizeUtil;
import lombok.Data;

import java.util.Objects;

@Data
public class File {
    public File(MateData.File file,String[] paths) {
        this.isFolder = file.getPath().size() - 1 > paths.length;
        if(!isFolder)
            this.length = DataSizeUtil.format(file.getLength());
        this.name = file.getPath().get(paths.length);
    }

    private String name;
    private String length;
    private boolean isFolder;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        File file = (File) o;
        return isFolder == file.isFolder && Objects.equals(name, file.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, isFolder);
    }
}
