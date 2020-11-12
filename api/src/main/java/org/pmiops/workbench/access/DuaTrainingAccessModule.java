package org.pmiops.workbench.access;

import org.pmiops.workbench.compliance.ComplianceService;
import org.pmiops.workbench.compliance.MoodleBadge;
import org.pmiops.workbench.db.model.DbUser;
import org.springframework.stereotype.Service;

@Service
public class DuaTrainingAccessModule extends MoodleBadgeAccessModule {

  public DuaTrainingAccessModule(
      ComplianceService complianceService) {
    super(complianceService);
  }

  @Override
  public MoodleBadge getBadge() {
    return MoodleBadge.DATA_USE_AGREEMENT;
  }
}
