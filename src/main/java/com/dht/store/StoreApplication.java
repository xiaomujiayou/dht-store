package com.dht.store;

import com.dht.store.downloader.bo.Config;
import com.dht.store.entity.UserConfig;
import com.dht.store.es.ESUserConfigRepo;
import com.dht.store.utils.DLUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Collections;
import java.util.List;

@EnableScheduling
@ComponentScan("com.dht")
@EnableElasticsearchRepositories("com.dht.store.es")
@SpringBootApplication
public class StoreApplication implements CommandLineRunner {

    @Autowired
    private ESUserConfigRepo esUserConfigRepo;
    @Value("${auth.userName}")
    private String userName;
    @Value("${auth.password}")
    private String password;

    public static void main(String[] args) {
        SpringApplication.run(StoreApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        Iterable<UserConfig> configs = esUserConfigRepo.findAll();
        if (!configs.iterator().hasNext()) {
            //配置不存在，创建默认配置
            UserConfig userConfig = new UserConfig();
            userConfig.setUserName(userName);
            userConfig.setPassword(password);
            userConfig.setDownloaderConfig(Collections.emptyList());
            esUserConfigRepo.save(userConfig);
        } else {
            try{
                UserConfig config = configs.iterator().next();
                List<Config> downloaderConfigs = config.getDownloaderConfig();
                Config downloaderConfig = downloaderConfigs.stream().filter(Config::getUse).findFirst().orElse(null);
                if (downloaderConfig != null)
                    DLUtil.updateDownloader(downloaderConfig);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
