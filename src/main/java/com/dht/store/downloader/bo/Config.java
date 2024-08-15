package com.dht.store.downloader.bo;

import cn.hutool.core.util.EnumUtil;
import com.dht.store.enums.DownloaderType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Config {
    /**
     * 下载器名称
     */
    private String name;
    /**
     * 下载器类型
     */
    private DownloaderType type;
    /**
     * 下载器图标
     */
    private String icon;
    /**
     * 下载器地址(http://127.0.0.1:1234)
     */
    private String host;
    /**
     * 账号
     */
    private String userName;
    /**
     * 密码
     */
    private String password;
    /**
     * 是否启用
     */
    private Boolean use;
    /**
     * 当前下载器是否正常
     */
    private Boolean status;

    public String getIcon() {
        if (type == null)
            return null;
        return type.getIconSvg();
    }
}
