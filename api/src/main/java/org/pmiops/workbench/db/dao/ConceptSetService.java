package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.ConceptSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConceptSetService {

  private final ConceptSetDao conceptSetDao;

  @Autowired
  ConceptSetService(ConceptSetDao conceptSetDao) {
    this.conceptSetDao = conceptSetDao;
  }


  @Transactional
  public ConceptSet cloneConceptSetAndConceptIds(ConceptSet conceptSet) {
    ConceptSet c = conceptSet.makeClone();
    ConceptSet saved = conceptSetDao.save(c);
    conceptSetDao.bulkCopyConceptIds(conceptSet.getConceptSetId(), saved.getConceptSetId());
    return saved;
  }
}
