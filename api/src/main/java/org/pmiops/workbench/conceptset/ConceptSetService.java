package org.pmiops.workbench.conceptset;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.persistence.OptimisticLockException;
import org.pmiops.workbench.api.Etags;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.cdr.dao.ConceptDao;
import org.pmiops.workbench.cohortbuilder.CohortBuilderService;
import org.pmiops.workbench.cohortbuilder.mapper.CohortBuilderMapper;
import org.pmiops.workbench.conceptset.mapper.ConceptSetMapper;
import org.pmiops.workbench.conceptset.mapper.ConceptSetMapper.ConceptSetContext;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbConceptSetConceptId;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.ConceptSet;
import org.pmiops.workbench.model.CreateConceptSetRequest;
import org.pmiops.workbench.model.Criteria;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.UpdateConceptSetRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConceptSetService {

  private static final int CONCEPT_SET_VERSION = 1;
  @VisibleForTesting public static int MAX_CONCEPTS_PER_SET = 1000;
  private static final ImmutableList<String> STANDARD_CONCEPTS = ImmutableList.of("S", "C");

  private final ConceptDao conceptDao;
  private final ConceptSetDao conceptSetDao;
  private final ConceptBigQueryService conceptBigQueryService;
  private final CohortBuilderService cohortBuilderService;
  private final ConceptSetMapper conceptSetMapper;
  private final Clock clock;
  private final CohortBuilderMapper cohortBuilderMapper;

  @Autowired
  public ConceptSetService(
      ConceptDao conceptDao,
      ConceptSetDao conceptSetDao,
      ConceptBigQueryService conceptBigQueryService,
      CohortBuilderService cohortBuilderService,
      CohortBuilderMapper cohortBuilderMapper,
      ConceptSetMapper conceptSetMapper,
      Clock clock) {
    this.conceptDao = conceptDao;
    this.conceptSetDao = conceptSetDao;
    this.conceptBigQueryService = conceptBigQueryService;
    this.cohortBuilderService = cohortBuilderService;
    this.cohortBuilderMapper = cohortBuilderMapper;
    this.conceptSetMapper = conceptSetMapper;
    this.clock = clock;
  }

  public ConceptSet copyAndSave(
      Long fromConceptSetId, String newConceptSetName, DbUser creator, Long toWorkspaceId) {
    final DbConceptSet existingConceptSet =
        Optional.ofNullable(conceptSetDao.findOne(fromConceptSetId))
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
      return toHydratedConcepts(
          conceptSetMapper.dbModelToClient(conceptSetDao.save(dbConceptSetCopy)));
    } catch (DataIntegrityViolationException e) {
      throw new ConflictException(
          String.format("Concept set %s already exists.", dbConceptSetCopy.getName()));
    }
  }

  public ConceptSet createConceptSet(
      CreateConceptSetRequest request, DbUser creator, Long workspaceId) {
    DbConceptSet dbConceptSet = conceptSetMapper.clientToDbModel(request, workspaceId, creator);
    try {
      return toHydratedConcepts(conceptSetMapper.dbModelToClient(conceptSetDao.save(dbConceptSet)));
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
      return toHydratedConcepts(conceptSetMapper.dbModelToClient(conceptSetDao.save(dbConceptSet)));
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

    if (request.getAddedConceptSetConceptIds() != null) {
      dbConceptSet
          .getConceptSetConceptIds()
          .addAll(
              request.getAddedConceptSetConceptIds().stream()
                  .map(
                      c ->
                          DbConceptSetConceptId.builder()
                              .addConceptId(c.getConceptId())
                              .addStandard(c.getStandard())
                              .build())
                  .collect(Collectors.toList()));
    }
    if (request.getRemovedConceptSetConceptIds() != null) {
      dbConceptSet
          .getConceptSetConceptIds()
          .removeAll(
              request.getRemovedConceptSetConceptIds().stream()
                  .map(
                      c ->
                          DbConceptSetConceptId.builder()
                              .addConceptId(c.getConceptId())
                              .addStandard(c.getStandard())
                              .build())
                  .collect(Collectors.toList()));
    }
    if (dbConceptSet.getConceptSetConceptIds().size() > MAX_CONCEPTS_PER_SET) {
      throw new ConflictException(
          String.format("Exceeded %d concept set limit", MAX_CONCEPTS_PER_SET));
    }

    dbConceptSet.setLastModifiedTime(new Timestamp(clock.instant().toEpochMilli()));
    try {
      return toHydratedConcepts(conceptSetMapper.dbModelToClient(conceptSetDao.save(dbConceptSet)));
    } catch (OptimisticLockException e) {
      throw new ConflictException("Failed due to concurrent concept set modification");
    }
  }

  public void delete(Long conceptSetId) {
    conceptSetDao.delete(conceptSetId);
  }

  public ConceptSet getConceptSet(Long conceptSetId) {
    DbConceptSet dbConceptSet =
        Optional.ofNullable(conceptSetDao.findOne(conceptSetId))
            .orElseThrow(
                () ->
                    new NotFoundException(
                        String.format("ConceptSet not found for concept id: %d", conceptSetId)));
    return toHydratedConcepts(
        conceptSetMapper.dbModelToClient(dbConceptSet, conceptBigQueryService));
  }

  public List<ConceptSet> findAll(List<Long> conceptSetIds) {
    return ((List<DbConceptSet>) conceptSetDao.findAll(conceptSetIds))
        .stream().map(conceptSetMapper::dbModelToClient).collect(Collectors.toList());
  }

  public List<ConceptSet> findByWorkspaceId(long workspaceId) {
    return conceptSetDao.findByWorkspaceId(workspaceId).stream()
        .map(conceptSet -> toHydratedConcepts(conceptSetMapper.dbModelToClient(conceptSet)))
        .collect(Collectors.toList());
  }

  public List<ConceptSet> findByWorkspaceIdAndSurvey(long workspaceId, short surveyId) {
    return conceptSetDao.findByWorkspaceIdAndSurvey(workspaceId, surveyId).stream()
        .map(conceptSetMapper::dbModelToClient)
        .collect(Collectors.toList());
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
    return conceptSetDao.save(dbConceptSetClone);
  }

  public List<DbConceptSet> getConceptSets(DbWorkspace workspace) {
    // Allows for fetching concept sets for a workspace once its collection is no longer
    // bound to a session.
    return conceptSetDao.findByWorkspaceId(workspace.getWorkspaceId());
  }

  private ConceptSet toHydratedConcepts(ConceptSet conceptSet) {
    Set<DbConceptSetConceptId> dbConceptSetConceptIds =
        conceptSetDao.findOne(conceptSet.getId()).getConceptSetConceptIds();
    List<Long> conceptIds =
        dbConceptSetConceptIds.stream()
            .map(DbConceptSetConceptId::getConceptId)
            .collect(Collectors.toList());
    List<Criteria> criteriaList;
    if (!conceptSet.getDomain().equals(Domain.PHYSICAL_MEASUREMENT)) {
      criteriaList =
          cohortBuilderService.findCriteriaByDomainIdAndConceptIds(
              conceptSet.getDomain().toString(), dbConceptSetConceptIds);
    } else {
      criteriaList =
          StreamSupport.stream(conceptDao.findAll(conceptIds).spliterator(), false)
              .map(
                  concept -> {
                    boolean isStandard = STANDARD_CONCEPTS.contains(concept.getStandardConcept());
                    return cohortBuilderMapper.dbModelToClient(
                        concept,
                        isStandard,
                        isStandard ? concept.getCountValue() : concept.getSourceCountValue());
                  })
              .collect(Collectors.toList());
    }
    return conceptSet.criteriums(criteriaList);
  }
}
