package org.pmiops.workbench.config;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;
import org.pmiops.workbench.db.dao.ConfigDao;
import org.pmiops.workbench.db.model.Config;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Configuration
public class CacheSpringConfiguration {

  private static final Map<String, Class<?>> CONFIG_CLASS_MAP = new HashMap<>();

  static {
    CONFIG_CLASS_MAP.put(Config.MAIN_CONFIG_ID, WorkbenchConfig.class);
  }

  @Bean
  @Qualifier("configCache")
  LoadingCache<String, Object> getConfigCache(ConfigDao configDao) {
    // Cache configuration in memory for ten minutes.
    return CacheBuilder.<String, Config>newBuilder()
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build(new CacheLoader<String, Object>() {
          @Override
          public Object load(String key) {
            Class<?> configClass = CONFIG_CLASS_MAP.get(key);
            if (configClass == null) {
              throw new IllegalArgumentException("Invalid config key: " + key);
            }
            Config config = configDao.findOne(key);
            if (config == null) {
              return null;
            }
            Gson gson = new Gson();
            return gson.fromJson(config.getConfiguration(), configClass);
          }
        });
  }

  public static WorkbenchConfig lookupWorkbenchConfig(
          LoadingCache<String, Object> configCache) throws ExecutionException {
    return (WorkbenchConfig) configCache.get(Config.MAIN_CONFIG_ID);
  }

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  WorkbenchConfig getWorkbenchConfig(@Qualifier("configCache") LoadingCache<String, Object> configCache) throws ExecutionException {
    return lookupWorkbenchConfig(configCache);
  }
}
