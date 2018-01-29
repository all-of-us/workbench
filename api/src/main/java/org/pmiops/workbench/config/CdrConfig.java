package org.pmiops.workbench.config;

import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

@Configuration
public class CdrConfig {

  @Bean(name = "defaultCdr")
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public CdrVersion getDefaultCdrVersion(WorkbenchConfig workbenchConfig,
      CdrVersionDao cdrVersionDao) {
    CdrVersion cdrVersion = cdrVersionDao.findByName(workbenchConfig.cdr.defaultCdrVersion);
    if (cdrVersion == null) {
      throw new ServerErrorException("No default CDR version found");
    }
    return cdrVersion;
  }
}
