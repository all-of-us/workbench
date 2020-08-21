package org.pmiops.workbench.conceptset;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Ordering;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.persistence.OptimisticLockException;
import org.pmiops.workbench.api.Etags;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.concept.ConceptService;
import org.pmiops.workbench.conceptset.mapper.ConceptSetMapper;
import org.pmiops.workbench.dataset.BigQueryTableInfo;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.Concept;
import org.pmiops.workbench.model.ConceptSet;
import org.pmiops.workbench.model.UpdateConceptSetRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConceptSetService {

  private static final int CONCEPT_SET_VERSION = 1;
  @VisibleForTesting public static int MAX_CONCEPTS_PER_SET = 1000;
  private final ConceptSetDao conceptSetDao;
  private final ConceptBigQueryService conceptBigQueryService;
  private final ConceptService conceptService;
  private final ConceptSetMapper conceptSetMapper;
  private final Clock clock;

  @Autowired
  public ConceptSetService(
      ConceptSetDao conceptSetDao,
      ConceptBigQueryService conceptBigQueryService,
      ConceptService conceptService,
      ConceptSetMapper conceptSetMapper,
      Clock clock) {
    this.conceptSetDao = conceptSetDao;
    this.conceptBigQueryService = conceptBigQueryService;
    this.conceptService = conceptService;
    this.conceptSetMapper = conceptSetMapper;
    this.clock = clock;
  }

  public ConceptSet save(DbConceptSet dbConceptSet) {
    try {
      return conceptSetMapper.dbModelToClient(conceptSetDao.save(dbConceptSet));
    } catch (DataIntegrityViolationException e) {
      throw new ConflictException(
          String.format("Concept set %s already exists.", dbConceptSet.getName()));
    }
  }

  public ConceptSet updateConceptSet(Long conceptSetId, ConceptSet conceptSet) {
    DbConceptSet dbConceptSet =
        Optional.ofNullable(conceptSetDao.findOne(conceptSetId))
            .orElseThrow(
                () ->
                    new NotFoundException("ConceptSet not found for concept id: " + conceptSetId));
    int version = Etags.toVersion(conceptSet.getEtag());
    if (dbConceptSet.getVersion() != version) {
      throw new ConflictException("Attempted to modify outdated concept set version");
    }
    if (conceptSet.getName() != null) {
      dbConceptSet.setName(conceptSet.getName());
    }
    if (conceptSet.getDescription() != null) {
      dbConceptSet.setDescription(conceptSet.getDescription());
    }
    if (conceptSet.getDomain() != null && conceptSet.getDomain() != dbConceptSet.getDomainEnum()) {
      throw new BadRequestException("Cannot modify the domain of an existing concept set");
    }
    final Timestamp now = Timestamp.from(clock.instant());
    dbConceptSet.setLastModifiedTime(now);
    try {
      // TODO: add recent resource entry for concept sets [RW-1129]
      return conceptSetMapper.dbModelToClient(conceptSetDao.save(dbConceptSet));
    } catch (OptimisticLockException e) {
      throw new ConflictException("Failed due to concurrent concept set modification");
    }
  }

  public ConceptSet updateConceptSetConcepts(Long conceptSetId, UpdateConceptSetRequest request) {
    DbConceptSet dbConceptSet =
        Optional.ofNullable(conceptSetDao.findOne(conceptSetId))
            .orElseThrow(
                () ->
                    new NotFoundException("ConceptSet not found for concept id: " + conceptSetId));

    int version = Etags.toVersion(request.getEtag());
    if (dbConceptSet.getVersion() != version) {
      throw new ConflictException("Attempted to modify outdated concept set version");
    }

    if (request.getAddedIds() != null) {
      dbConceptSet.getConceptIds().addAll(request.getAddedIds());
    }
    if (request.getRemovedIds() != null) {
      dbConceptSet.getConceptIds().removeAll(request.getRemovedIds());
    }
    if (dbConceptSet.getConceptIds().size() > MAX_CONCEPTS_PER_SET) {
      throw new ConflictException("Exceeded " + MAX_CONCEPTS_PER_SET + " in concept set");
    }
    if (dbConceptSet.getConceptIds().isEmpty()) {
      dbConceptSet.setParticipantCount(0);
    } else {
      dbConceptSet.setParticipantCount(
          conceptBigQueryService.getParticipantCountForConcepts(
              dbConceptSet.getDomainEnum(),
              BigQueryTableInfo.getTableName(dbConceptSet.getDomainEnum()),
              dbConceptSet.getConceptIds()));
    }

    dbConceptSet.setLastModifiedTime(new Timestamp(clock.instant().toEpochMilli()));
    try {
      return conceptSetMapper.dbModelToClient(conceptSetDao.save(dbConceptSet));
      // TODO: add recent resource entry for concept sets [RW-1129]
    } catch (OptimisticLockException e) {
      throw new ConflictException("Failed due to concurrent concept set modification");
    }
  }

  public void delete(Long conceptSetId) {
    conceptSetDao.delete(conceptSetId);
  }

  public ConceptSet findOne(Long conceptSetId) {
    DbConceptSet dbConceptSet =
        Optional.ofNullable(conceptSetDao.findOne(conceptSetId))
            .orElseThrow(
                () ->
                    new NotFoundException("ConceptSet not found for concept id: " + conceptSetId));
    return conceptSetMapper.dbModelToClient(dbConceptSet);
  }

  public List<ConceptSet> findAll(List<Long> conceptSetIds) {
    return ((List<DbConceptSet>) conceptSetDao.findAll(conceptSetIds))
        .stream().map(conceptSetMapper::dbModelToClient).collect(Collectors.toList());
  }

  public Optional<DbConceptSet> findDbConceptSet(Long conceptSetId) {
    return Optional.of(conceptSetDao.findOne(conceptSetId));
  }

  public List<ConceptSet> findByWorkspaceId(long workspaceId) {
    return conceptSetDao.findByWorkspaceId(workspaceId).stream()
        .map(conceptSetMapper::dbModelToClient)
        .collect(Collectors.toList());
  }

  public List<ConceptSet> findByWorkspaceIdAndSurvey(long workspaceId, short surveyId) {
    return conceptSetDao.findByWorkspaceIdAndSurvey(workspaceId, surveyId).stream()
        .map(conceptSetMapper::dbModelToClient)
        .collect(Collectors.toList());
  }

  public ConceptSet toHydratedConcepts(ConceptSet conceptSet) {
    return conceptSet.concepts(
        conceptService.findAll(
            conceptSetDao.findOne(conceptSet.getId()).getConceptIds(),
            Ordering.from(String.CASE_INSENSITIVE_ORDER).onResultOf(Concept::getConceptName)));
  }

  @Transactional
  public DbConceptSet cloneConceptSetAndConceptIds(
      DbConceptSet conceptSet, DbWorkspace targetWorkspace, boolean cdrVersionChanged) {
    DbConceptSet dbConceptSet = new DbConceptSet(conceptSet);
    if (cdrVersionChanged) {
      String omopTable = BigQueryTableInfo.getTableName(conceptSet.getDomainEnum());
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
