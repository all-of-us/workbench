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
  @Bean
  @Qualifier("AccessModule")
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  Supplier<List<DbAccessModule>> getCacheAccessModule(AccessModuleDao accessModuleDao) {
    // Cache configuration in memory for ten minutes.
    return Suppliers.memoizeWithExpiration(
        accessModuleDao::findAll, 10, TimeUnit.MINUTES);
  }
}
