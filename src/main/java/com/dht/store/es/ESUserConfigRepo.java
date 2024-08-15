package com.dht.store.es;

import com.dht.store.entity.UserConfig;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ESUserConfigRepo extends ElasticsearchRepository<UserConfig, String> {

}
