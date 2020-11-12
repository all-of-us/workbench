package org.pmiops.workbench.access;

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
  public MoodleBadge getBadge() {
    return MoodleBadge.RESEARCH_ETHICS_TRAINING;
  }
}
