package org.pmiops.workbench.cohortbuilder;

import org.pmiops.workbench.cdr.dao.PersonDao;
import org.pmiops.workbench.model.AgeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CohortBuilderStorageManager {

  private PersonDao personDao;

  @Autowired
  public CohortBuilderStorageManager(PersonDao personDao) {
    this.personDao = personDao;
  }

  public long countAgesByType(AgeType ageType, int startAge, int endAge) {
    return personDao.countAgesByType(ageType, startAge, endAge);
  }
}
