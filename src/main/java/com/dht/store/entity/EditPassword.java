package com.dht.store.entity;

import lombok.Data;

@Data
public class EditPassword {
    private String userName;
    private String password;
    private String newPassword;
}
