package org.pmiops.workbench.access.modules;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AccessModuleConfiguration {
  @Bean
  Map<AccessModuleKey, AccessModuleService> getAccessModuleMap(
      Set<AccessModuleService> accessModules) {
    return accessModules.stream()
        .collect(ImmutableMap.toImmutableMap(AccessModuleService::getKey, Function.identity()));
  }
}
