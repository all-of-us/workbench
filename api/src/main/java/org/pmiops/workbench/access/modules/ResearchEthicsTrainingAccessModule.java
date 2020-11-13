package org.pmiops.workbench.access.modules;

import org.pmiops.workbench.access.modules.AccessModuleKey;
import org.pmiops.workbench.access.modules.MoodleBadgeAccessModule;
import org.pmiops.workbench.compliance.ComplianceService;
import org.pmiops.workbench.compliance.MoodleBadge;
import org.springframework.stereotype.Service;

@Service
public class ResearchEthicsTrainingAccessModule extends MoodleBadgeAccessModule {

  public ResearchEthicsTrainingAccessModule(
      ComplianceService complianceService) {
    super(complianceService);
  }

  @Override
  public AccessModuleKey getKey() {
    return AccessModuleKey.RESEARCH_ETHICS_TRAINING;
  }

  @Override
  public MoodleBadge getBadge() {
    return MoodleBadge.RESEARCH_ETHICS_TRAINING;
  }
}
