package org.pmiops.workbench.config;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.pmiops.workbench.db.dao.ConfigDao;
import org.pmiops.workbench.db.model.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

@Configuration
public class CacheConfig {

  @Bean
  LoadingCache<String, Config> getConfigCache(ConfigDao configDao) {
    // Cache configuration in memory for ten minutes.
    return CacheBuilder.<String, Config>newBuilder()
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build(new CacheLoader<String, Config>() {
          @Override
          public Config load(String key) {
            return configDao.findOne(key);
          }
        });
  }

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  Config getMainConfig(LoadingCache<String, Config> configCache) throws ExecutionException {
    return configCache.get(Config.MAIN_CONFIG_ID);
  }
}
