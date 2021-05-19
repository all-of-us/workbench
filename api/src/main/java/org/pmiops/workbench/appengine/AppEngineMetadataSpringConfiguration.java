package org.pmiops.workbench.appengine;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppEngineMetadataSpringConfiguration {

  @Bean
  AppEngineModuleService getModulesService() {
    return new AppEngineModuleService();
  }
}
