package org.pmiops.workbench.access;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.pmiops.workbench.compliance.ComplianceService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AccessConfiguration {
  @Bean
  Map<AccessModuleKey, AccessModuleService> getAccessModuleMap(ComplianceService complianceService) {
    return ImmutableMap.of(
        AccessModuleKey.DUA_TRAINING, new DuaTrainingAccessModule(complianceService),
        AccessModuleKey.RESEARCH_ETHICS_TRAINING, new ResearchEthicsTrainingAccessModule(complianceService));
  }
}
