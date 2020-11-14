package org.pmiops.workbench.access.modules;

import org.pmiops.workbench.compliance.ComplianceService;
import org.pmiops.workbench.compliance.MoodleBadge;
import org.springframework.stereotype.Service;

@Service
public class DuaTrainingAccessModule extends MoodleBadgeAccessModule {

  public DuaTrainingAccessModule(ComplianceService complianceService) {
    super(complianceService);
  }

  @Override
  public AccessModuleKey getKey() {
    return AccessModuleKey.DUA_TRAINING;
  }

  @Override
  public MoodleBadge getBadge() {
    return MoodleBadge.DATA_USE_AGREEMENT;
  }
}
