package org.pmiops.workbench.appengine;

import com.google.appengine.api.modules.ModulesService;
import com.google.appengine.api.modules.ModulesServiceFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppEngineMetadataSpringConfiguration {

  @Bean
  ModulesService getModulesService() {
    return ModulesServiceFactory.getModulesService();
  }
}
