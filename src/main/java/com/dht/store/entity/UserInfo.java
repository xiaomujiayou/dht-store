package com.dht.store.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
public class UserInfo {
    /**
     * 用户名
     */
    @Id
    private String userName;
    /**
     * 密码
     */
    private String password;
}
