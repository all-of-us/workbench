package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.ConceptSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConceptSetService {

  // Note: Cannot use an @Autowired constructor with this version of Spring
  // Boot due to https://jira.spring.io/browse/SPR-15600. See RW-256.
  @Autowired private ConceptSetDao conceptSetDao;
  
  @Transactional
  public ConceptSet cloneConceptSetAndConceptIds(ConceptSet conceptSet) {
    ConceptSet c = conceptSet.makeClone();
    ConceptSet saved = conceptSetDao.save(c);
    conceptSetDao.bulkCopyConceptIds(conceptSet.getConceptSetId(), saved.getConceptSetId());
    return saved;
  }
}
