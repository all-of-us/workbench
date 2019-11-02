package org.pmiops.workbench.conceptset;

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConceptSetService {

  // Note: Cannot use an @Autowired constructor with this version of Spring
  // Boot due to https://jira.spring.io/browse/SPR-15600. See RW-256.
  @Autowired private ConceptSetDao conceptSetDao;

  @Autowired private ConceptBigQueryService conceptBigQueryService;

  @Transactional
  public DbConceptSet cloneConceptSetAndConceptIds(
      DbConceptSet conceptSet, DbWorkspace targetWorkspace, boolean cdrVersionChanged) {
    DbConceptSet c = new DbConceptSet(conceptSet);
    if (cdrVersionChanged) {
      String omopTable = ConceptSetDao.DOMAIN_TO_TABLE_NAME.get(conceptSet.getDomainEnum());
      c.setParticipantCount(
          conceptBigQueryService.getParticipantCountForConcepts(
              omopTable, conceptSet.getConceptIds()));
    }
    c.setWorkspaceId(targetWorkspace.getWorkspaceId());
    c.setCreator(targetWorkspace.getCreator());
    c.setLastModifiedTime(targetWorkspace.getLastModifiedTime());
    c.setCreationTime(targetWorkspace.getCreationTime());
    c.setVersion(1);
    return conceptSetDao.save(c);
  }

  public List<DbConceptSet> getConceptSets(DbWorkspace workspace) {
    // Allows for fetching concept sets for a workspace once its collection is no longer
    // bound to a session.
    return conceptSetDao.findByWorkspaceId(workspace.getWorkspaceId());
  }

  @VisibleForTesting
  public void setConceptSetDao(ConceptSetDao conceptSetDao) {
    this.conceptSetDao = conceptSetDao;
  }
}
