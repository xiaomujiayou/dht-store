package com.dht.store.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import com.dht.store.downloader.Downloader;
import com.dht.store.downloader.bo.Config;
import com.dht.store.entity.EditPassword;
import com.dht.store.entity.Login;
import com.dht.store.entity.UserConfig;
import com.dht.store.enums.MsgEnum;
import com.dht.store.es.ESUserConfigRepo;
import com.dht.store.exception.ServiceException;
import com.dht.store.utils.DLUtil;
import com.dht.store.utils.Msg;
import com.dht.store.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private ESUserConfigRepo esUserConfigRepo;

    @PostMapping("/login")
    public Msg<?> login(@RequestBody Login login) {
        if (ObjUtil.isEmpty(login.getUserName()))
            throw new ServiceException(MsgEnum.PARAM_VALID_ERROR, "userName 不能为空");
        if (ObjUtil.isEmpty(login.getPassword()))
            throw new ServiceException(MsgEnum.PARAM_VALID_ERROR, "password 不能为空");
        UserConfig userConfig = esUserConfigRepo.findAll().iterator().next();
        if (userConfig.getUserName().equals(login.getUserName()) && userConfig.getPassword().equals(login.getPassword())) {
            StpUtil.login(login.getUserName() + ":" + login.getPassword());
            return R.sucess("登录成功！");
        }
        throw new ServiceException(MsgEnum.SYSTEM_LOGIN_ERROR);
    }

    @PostMapping("/logout")
    public Msg<?> logout() {
        StpUtil.logout();
        return R.sucess();
    }

    @PostMapping("/edit/password")
    public Msg<?> login(@RequestBody EditPassword editPassword) {
        if (ObjUtil.isEmpty(editPassword.getNewPassword()))
            throw new ServiceException(MsgEnum.PARAM_VALID_ERROR, "userName 不能为空");
        if (ObjUtil.isEmpty(editPassword.getPassword()))
            throw new ServiceException(MsgEnum.PARAM_VALID_ERROR, "password 不能为空");
        if (ObjUtil.isEmpty(editPassword.getNewPassword()))
            throw new ServiceException(MsgEnum.PARAM_VALID_ERROR, "newPassword 不能为空");
        UserConfig userConfig = esUserConfigRepo.findAll().iterator().next();
        if (userConfig.getUserName().equals(editPassword.getUserName()) && userConfig.getPassword().equals(editPassword.getPassword())) {
            userConfig.setPassword(editPassword.getNewPassword());
            esUserConfigRepo.save(userConfig);
            StpUtil.logout();
            return R.sucess("修改成功！");
        } else
            return R.error(MsgEnum.SYSTEM_LOGIN_ERROR, "账号或密码错误，修改失败！");
    }

    @GetMapping("/downloader")
    public Msg<?> downloader() {
        UserConfig userConfig = esUserConfigRepo.findAll().iterator().next();
        try {
            Downloader downloader = DLUtil.getDownloader();
            if (downloader == null || downloader.getConfig() == null)
                return R.sucess(userConfig.getDownloaderConfig());
            Config config = downloader.getConfig();
            userConfig.getDownloaderConfig().stream().filter(o -> o.getName().equals(config.getName())).findFirst().ifPresent(o -> o.setStatus(downloader.getStatus()));
            return R.sucess(userConfig.getDownloaderConfig());
        } catch (Exception e) {
            userConfig.getDownloaderConfig().stream().filter(Config::getUse).findFirst().ifPresent(o -> o.setStatus(false));
            return R.sucess(userConfig.getDownloaderConfig());
        }
    }

    @PostMapping("/del/downloader")
    public Msg<?> delDownloader(@RequestBody Config config) {
        UserConfig userConfig = esUserConfigRepo.findAll().iterator().next();
        List<Config> downloaderConfig = userConfig.getDownloaderConfig();
        if (downloaderConfig == null)
            return R.sucess();
        downloaderConfig.removeIf(o -> o.getName().equals(config.getName()));
        esUserConfigRepo.save(userConfig);
        return R.sucess();
    }

    @PostMapping("/add/downloader")
    public Msg<?> addDownloader(@RequestBody Config config) {
        if (ObjUtil.isEmpty(config.getName()))
            throw new ServiceException(MsgEnum.PARAM_VALID_ERROR, "name 不能为空");
        if (ObjUtil.isEmpty(config.getType()))
            throw new ServiceException(MsgEnum.PARAM_VALID_ERROR, "type 不能为空");
        if (ObjUtil.isEmpty(config.getHost()))
            throw new ServiceException(MsgEnum.PARAM_VALID_ERROR, "host 不能为空");
        if (ObjUtil.isEmpty(config.getUse()))
            throw new ServiceException(MsgEnum.PARAM_VALID_ERROR, "use 不能为空");
        UserConfig userConfig = esUserConfigRepo.findAll().iterator().next();
        List<Config> downloaderConfig = userConfig.getDownloaderConfig();
        if (downloaderConfig == null) {
            downloaderConfig = new ArrayList<>();
            userConfig.setDownloaderConfig(downloaderConfig);
        }
        if (config.getUse()) {
            for (Config conf : downloaderConfig) {
                conf.setUse(false);
            }
        }
        if (downloaderConfig.stream().anyMatch(o -> o.getName().equals(config.getName())))
            throw new ServiceException(MsgEnum.DATA_ALREADY_EXISTS, "名称已存在！");
        if (config.getUse())
            DLUtil.updateDownloader(config);
        downloaderConfig.add(config);
        esUserConfigRepo.save(userConfig);
        DLUtil.updateDownloader(config);
        return R.sucess();
    }

    @PostMapping("/edit/downloader")
    public Msg<?> editDownloader(@RequestBody Config config) {
        if (ObjUtil.isEmpty(config.getName()))
            throw new ServiceException(MsgEnum.PARAM_VALID_ERROR, "name 不能为空");
        if (ObjUtil.isEmpty(config.getType()))
            throw new ServiceException(MsgEnum.PARAM_VALID_ERROR, "type 不能为空");
        if (ObjUtil.isEmpty(config.getHost()))
            throw new ServiceException(MsgEnum.PARAM_VALID_ERROR, "host 不能为空");
        if (ObjUtil.isEmpty(config.getUse()))
            throw new ServiceException(MsgEnum.PARAM_VALID_ERROR, "use 不能为空");
        UserConfig userConfig = esUserConfigRepo.findAll().iterator().next();
        List<Config> downloaderConfig = userConfig.getDownloaderConfig();
        if (downloaderConfig == null) {
            downloaderConfig = new ArrayList<>();
            userConfig.setDownloaderConfig(downloaderConfig);
        }
        if (config.getUse()) {
            for (Config conf : downloaderConfig) {
                conf.setUse(false);
            }
        }
        Config old = downloaderConfig.stream().filter(o -> o.getName().equals(config.getName())).findFirst().orElse(null);
        if (old == null) {
            old = config;
            downloaderConfig.add(old);
        } else {
            BeanUtil.copyProperties(config, old);
        }
        esUserConfigRepo.save(userConfig);
        DLUtil.updateDownloader(config);
        return R.sucess();
    }

    @PostMapping("/change/use")
    public Msg<?> changeUse(@RequestBody Config config) {
        if (ObjUtil.isEmpty(config.getName()))
            throw new ServiceException(MsgEnum.PARAM_VALID_ERROR, "name 不能为空");
        if (ObjUtil.isEmpty(config.getUse()))
            throw new ServiceException(MsgEnum.PARAM_VALID_ERROR, "use 不能为空");
        UserConfig userConfig = esUserConfigRepo.findAll().iterator().next();
        List<Config> downloaderConfig = userConfig.getDownloaderConfig();
        if (downloaderConfig == null)
            throw new ServiceException(MsgEnum.CONF_NOT_FOUND);
        Config old = downloaderConfig.stream().filter(o -> o.getName().equals(config.getName())).findFirst().orElse(null);
        if (old == null)
            throw new ServiceException(MsgEnum.CONF_NOT_FOUND);
        if (config.getUse()) {
            for (Config conf : downloaderConfig) {
                conf.setUse(false);
            }
        }
        old.setUse(config.getUse());
//        if (config.getUse())
        esUserConfigRepo.save(userConfig);
        DLUtil.updateDownloader(config);
        return R.sucess();
    }

    @PostMapping("/downloader/test")
    public Msg<?> downloaderTest(@RequestBody Config config) {
        if (ObjUtil.isEmpty(config.getHost()))
            throw new ServiceException(MsgEnum.PARAM_VALID_ERROR, "host 不能为空");
        return R.sucess(DLUtil.test(config));
    }
}