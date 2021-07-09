package org.pmiops.workbench.config;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.pmiops.workbench.db.dao.AccessModuleDao;
import org.pmiops.workbench.db.model.DbAccessModule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

@Configuration
public class CacheAccessModuleConfiguration {
  private static final String CAHCE_ACCESS_MODULE_BEAN_NAME = "accessModuleCache";

  @Bean
  @Qualifier(CAHCE_ACCESS_MODULE_BEAN_NAME)
  Supplier<List<DbAccessModule>> getCacheAccessModule(AccessModuleDao accessModuleDao) {
    // Cache configuration in memory for ten minutes.
    return Suppliers.memoizeWithExpiration(accessModuleDao::findAll, 10, TimeUnit.MINUTES);
  }

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  List<DbAccessModule> getDbAccessModules(
      @Qualifier(CAHCE_ACCESS_MODULE_BEAN_NAME)
          Supplier<List<DbAccessModule>> cachedAccessModules) {
    return cachedAccessModules.get();
  }
}
