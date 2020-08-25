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
import org.pmiops.workbench.conceptset.mapper.ConceptSetMapper.ConceptSetContext;
import org.pmiops.workbench.dataset.BigQueryTableInfo;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.Concept;
import org.pmiops.workbench.model.ConceptSet;
import org.pmiops.workbench.model.CreateConceptSetRequest;
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

  public ConceptSet copyAndSave(
      Long fromConceptSetId, String newConceptSetName, DbUser creator, Long toWorkspaceId) {
    final DbConceptSet existingConceptSet =
        findDbConceptSet(fromConceptSetId)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        String.format("Concept set %s does not exist", fromConceptSetId)));
    final Timestamp now = Timestamp.from(clock.instant());
    ConceptSetContext conceptSetContext =
        new ConceptSetContext.Builder()
            .name(newConceptSetName)
            .creator(creator)
            .workspaceId(toWorkspaceId)
            .creationTime(now)
            .lastModifiedTime(now)
            .version(CONCEPT_SET_VERSION)
            .build();
    DbConceptSet dbConceptSetCopy =
        conceptSetMapper.dbModelToDbModel(existingConceptSet, conceptSetContext);

    try {
      return conceptSetMapper.dbModelToClient(conceptSetDao.save(dbConceptSetCopy));
    } catch (DataIntegrityViolationException e) {
      throw new ConflictException(
          String.format("Concept set %s already exists.", dbConceptSetCopy.getName()));
    }
  }

  public ConceptSet save(CreateConceptSetRequest request, DbUser creator, Long workspaceId) {
    DbConceptSet dbConceptSet =
        conceptSetMapper.clientToDbModel(request, workspaceId, creator, conceptBigQueryService);
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
                    new NotFoundException(
                        String.format("ConceptSet not found for conceptSetId: %d", conceptSetId)));
    if (dbConceptSet.getVersion() != Etags.toVersion(conceptSet.getEtag())) {
      throw new ConflictException("Attempted to modify outdated concept set version");
    }
    if (conceptSet.getName() != null) {
      dbConceptSet.setName(conceptSet.getName());
    }
    if (conceptSet.getDescription() != null) {
      dbConceptSet.setDescription(conceptSet.getDescription());
    }
    if (!conceptSet.getDomain().equals(dbConceptSet.getDomainEnum())) {
      throw new ConflictException(
          String.format(
              "Concept Set is not the same domain as: %s", conceptSet.getDomain().toString()));
    }
    dbConceptSet.setLastModifiedTime(Timestamp.from(clock.instant()));
    try {
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
                    new NotFoundException(
                        String.format("ConceptSet not found for concept id: %d", conceptSetId)));

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
      throw new ConflictException(
          String.format("Exceeded %d concept set limit", MAX_CONCEPTS_PER_SET));
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
                    new NotFoundException(
                        String.format("ConceptSet not found for concept id: %d", conceptSetId)));
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
      DbConceptSet dbConceptSet, DbWorkspace targetWorkspace, boolean cdrVersionChanged) {
    ConceptSetContext conceptSetContext =
        new ConceptSetContext.Builder()
            .name(dbConceptSet.getName())
            .creator(targetWorkspace.getCreator())
            .workspaceId(targetWorkspace.getWorkspaceId())
            .creationTime(targetWorkspace.getCreationTime())
            .lastModifiedTime(targetWorkspace.getLastModifiedTime())
            .version(CONCEPT_SET_VERSION)
            .build();

    DbConceptSet dbConceptSetClone =
        conceptSetMapper.dbModelToDbModel(dbConceptSet, conceptSetContext);
    if (cdrVersionChanged) {
      String omopTable = BigQueryTableInfo.getTableName(dbConceptSet.getDomainEnum());
      dbConceptSetClone.setParticipantCount(
          conceptBigQueryService.getParticipantCountForConcepts(
              dbConceptSet.getDomainEnum(), omopTable, dbConceptSet.getConceptIds()));
    }
    return conceptSetDao.save(dbConceptSetClone);
  }

  public List<DbConceptSet> getConceptSets(DbWorkspace workspace) {
    // Allows for fetching concept sets for a workspace once its collection is no longer
    // bound to a session.
    return conceptSetDao.findByWorkspaceId(workspace.getWorkspaceId());
  }
}
