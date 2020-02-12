package org.pmiops.workbench.conceptset;

import java.util.List;
import java.util.Optional;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConceptSetService {

  private static final int CONCEPT_SET_VERSION = 1;
  private ConceptSetDao conceptSetDao;
  private ConceptBigQueryService conceptBigQueryService;

  @Autowired
  public ConceptSetService(
      ConceptSetDao conceptSetDao, ConceptBigQueryService conceptBigQueryService) {
    this.conceptSetDao = conceptSetDao;
    this.conceptBigQueryService = conceptBigQueryService;
  }

  public DbConceptSet save(DbConceptSet dbConceptSet) {
    return conceptSetDao.save(dbConceptSet);
  }

  public void delete(Long conceptSetId) {
    conceptSetDao.delete(conceptSetId);
  }

  public Optional<DbConceptSet> findOne(Long conceptSetId, Long workspaceId) {
    return conceptSetDao.findOneByConceptSetIdAndWorkspaceId(conceptSetId, workspaceId);
  }

  public List<DbConceptSet> findByWorkspaceId(long workspaceId) {
    return conceptSetDao.findByWorkspaceId(workspaceId);
  }

  public List<DbConceptSet> findByWorkspaceIdAndSurvey(long workspaceId, short surveyId) {
    return conceptSetDao.findByWorkspaceIdAndSurvey(workspaceId, surveyId);
  }

  @Transactional
  public DbConceptSet cloneConceptSetAndConceptIds(
      DbConceptSet conceptSet, DbWorkspace targetWorkspace, boolean cdrVersionChanged) {
    DbConceptSet dbConceptSet = new DbConceptSet(conceptSet);
    if (cdrVersionChanged) {
      String omopTable = ConceptSetDao.DOMAIN_TO_TABLE_NAME.get(conceptSet.getDomainEnum());
      dbConceptSet.setParticipantCount(
          conceptBigQueryService.getParticipantCountForConcepts(
              conceptSet.getDomainEnum(), omopTable, conceptSet.getConceptIds()));
    }
    dbConceptSet.setWorkspaceId(targetWorkspace.getWorkspaceId());
    dbConceptSet.setCreator(targetWorkspace.getCreator());
    dbConceptSet.setLastModifiedTime(targetWorkspace.getLastModifiedTime());
    dbConceptSet.setCreationTime(targetWorkspace.getCreationTime());
    dbConceptSet.setVersion(CONCEPT_SET_VERSION);
    return conceptSetDao.save(dbConceptSet);
  }

  public List<DbConceptSet> getConceptSets(DbWorkspace workspace) {
    // Allows for fetching concept sets for a workspace once its collection is no longer
    // bound to a session.
    return conceptSetDao.findByWorkspaceId(workspace.getWorkspaceId());
  }
}
