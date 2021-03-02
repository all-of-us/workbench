package org.pmiops.workbench.config;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.pmiops.workbench.cdr.cache.MySQLStopWords;
import org.pmiops.workbench.cdr.dao.CBCriteriaDao;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

@Configuration
public class CacheMySqlStopWordConfig {
  public static final String MYSQL_STOP_WORDS = "mysql_stop_words";

  private static final Map<String, Class<?>> CACHE_MAP = new HashMap<>();

  static {
    CACHE_MAP.put(MYSQL_STOP_WORDS, MySQLStopWords.class);
  }

  @Bean
  @Qualifier("cacheMySqlStopWordConfig")
  LoadingCache<String, Object> getCacheMySqlStopWordConfig(CBCriteriaDao cbCriteriaDao) {
    // Cache configuration in memory for 24 hours.
    return CacheBuilder.newBuilder()
        .expireAfterWrite(24, TimeUnit.HOURS)
        .build(
            new CacheLoader<String, Object>() {
              @Override
              public Object load(String key) {
                Class<?> mySQLStopWords = CACHE_MAP.get(key);
                if (mySQLStopWords == null) {
                  throw new IllegalArgumentException("Invalid config key: " + key);
                }
                return new MySQLStopWords(cbCriteriaDao.findMySQLStopWords());
              }
            });
  }

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  MySQLStopWords getMySQLStopWords(
      @Qualifier("cacheMySqlStopWordConfig") LoadingCache<String, Object> cacheMySqlStopWordConfig)
      throws ExecutionException {
    return (MySQLStopWords) cacheMySqlStopWordConfig.get(MYSQL_STOP_WORDS);
  }
}
