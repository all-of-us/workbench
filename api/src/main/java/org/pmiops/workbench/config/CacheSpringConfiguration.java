package org.pmiops.workbench.config;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.pmiops.workbench.db.dao.ConfigDao;
import org.pmiops.workbench.db.model.DbConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

@Configuration
public class CacheSpringConfiguration {

  private static final Map<String, Class<?>> CONFIG_CLASS_MAP = new HashMap<>();

  static {
    CONFIG_CLASS_MAP.put(DbConfig.MAIN_CONFIG_ID, WorkbenchConfig.class);
    CONFIG_CLASS_MAP.put(DbConfig.CDR_BIGQUERY_SCHEMA_CONFIG_ID, CdrBigQuerySchemaConfig.class);
    CONFIG_CLASS_MAP.put(DbConfig.FEATURED_WORKSPACES_CONFIG_ID, FeaturedWorkspacesConfig.class);
  }

  @Bean
  @Qualifier("configCache")
  LoadingCache<String, Object> getConfigCache(ConfigDao configDao) {
    // Cache configuration in memory for ten minutes.
    return CacheBuilder.newBuilder()
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build(
            new CacheLoader<String, Object>() {
              @Override
              public Object load(String key) {
                Class<?> configClass = CONFIG_CLASS_MAP.get(key);
                if (configClass == null) {
                  throw new IllegalArgumentException("Invalid config key: " + key);
                }
                DbConfig config = configDao.findOne(key);
                if (config == null) {
                  return null;
                }
                Gson gson = new Gson();
                return gson.fromJson(config.getConfiguration(), configClass);
              }
            });
  }

  public static WorkbenchConfig lookupWorkbenchConfig(LoadingCache<String, Object> configCache)
      throws ExecutionException {
    return (WorkbenchConfig) configCache.get(DbConfig.MAIN_CONFIG_ID);
  }

  public static CdrBigQuerySchemaConfig lookupBigQueryCdrSchemaConfig(
      LoadingCache<String, Object> configCache) throws ExecutionException {
    return (CdrBigQuerySchemaConfig) configCache.get(DbConfig.CDR_BIGQUERY_SCHEMA_CONFIG_ID);
  }

  public static FeaturedWorkspacesConfig lookupFeaturedWorkspacesConfig(
      LoadingCache<String, Object> configCache) throws ExecutionException {
    return (FeaturedWorkspacesConfig) configCache.get(DbConfig.FEATURED_WORKSPACES_CONFIG_ID);
  }

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  WorkbenchConfig getWorkbenchConfig(
      @Qualifier("configCache") LoadingCache<String, Object> configCache)
      throws ExecutionException {
    return lookupWorkbenchConfig(configCache);
  }

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  FeaturedWorkspacesConfig getFeaturedWorkspacesConfig(
      @Qualifier("configCache") LoadingCache<String, Object> configCache)
      throws ExecutionException {
    return lookupFeaturedWorkspacesConfig(configCache);
  }

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  CdrBigQuerySchemaConfig getCdrSchemaConfig(
      @Qualifier("configCache") LoadingCache<String, Object> configCache)
      throws ExecutionException {
    return lookupBigQueryCdrSchemaConfig(configCache);
  }
}
