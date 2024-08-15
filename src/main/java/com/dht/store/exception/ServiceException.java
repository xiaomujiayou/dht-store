package com.dht.store.exception;

import com.dht.store.enums.MsgEnum;
import lombok.Data;

@Data
public class ServiceException extends RuntimeException {

    private MsgEnum msgEnum;

    public ServiceException(MsgEnum msgEnum) {
        super(msgEnum.getMsg());
        this.msgEnum = msgEnum;
    }

    public ServiceException(MsgEnum msgEnum, String msg) {
        super(msgEnum.getMsg());
        this.msgEnum = msgEnum;
        this.msgEnum.setMsg(msg);
    }

    @Override
    public String getMessage() {
        return msgEnum.getMsg();
    }
}
