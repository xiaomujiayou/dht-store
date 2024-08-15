package com.dht.store.exception;

import cn.dev33.satoken.exception.NotLoginException;
import com.dht.store.exception.ServiceException;
import com.dht.store.utils.Msg;
import com.dht.store.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

import static com.dht.store.enums.MsgEnum.SYSTEM_TOKEN_ERROR;

@Slf4j
@ControllerAdvice
public class ExceptionProcess {
    @ResponseBody
    @ExceptionHandler(value = ServiceException.class)
    public Msg<?> exceptionHandler(ServiceException e) {
        log.warn(e.getMessage());
        return R.error(e.getMsgEnum());
    }

    @ResponseBody
    @ExceptionHandler(value = NotLoginException.class)
    public Msg<?> exceptionHandler(NotLoginException e, HttpServletRequest request) {
        log.warn("登录异常:{} api:{} ip:{}", e.getMessage(),request.getRequestURI(),request.getRemoteHost());
        return R.error(SYSTEM_TOKEN_ERROR);
    }
}

