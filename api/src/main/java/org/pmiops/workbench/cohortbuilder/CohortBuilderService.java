package org.pmiops.workbench.cohortbuilder;

import org.pmiops.workbench.model.AgeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CohortBuilderService {

  private CohortBuilderStorageManager storageManager;

  @Autowired
  public CohortBuilderService(CohortBuilderStorageManager storageManager) {
    this.storageManager = storageManager;
  }

  public long countAgesByType(AgeType ageType, int startAge, int endAge) {
    return storageManager.countAgesByType(ageType, startAge, endAge);
  }
}
