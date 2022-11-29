package org.pmiops.workbench.conceptset;

import com.google.common.annotations.VisibleForTesting;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.inject.Provider;
import javax.persistence.OptimisticLockException;
import org.pmiops.workbench.api.Etags;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.cohortbuilder.CohortBuilderService;
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
  private final CohortBuilderService cohortBuilderService;
  private final ConceptSetMapper conceptSetMapper;
  private final Clock clock;
  private final Provider<DbUser> userProvider;

  @Autowired
  public ConceptSetService(
      ConceptSetDao conceptSetDao,
      ConceptBigQueryService conceptBigQueryService,
      CohortBuilderService cohortBuilderService,
      ConceptSetMapper conceptSetMapper,
      Clock clock,
      Provider<DbUser> userProvider) {
    this.conceptSetDao = conceptSetDao;
    this.conceptBigQueryService = conceptBigQueryService;
    this.cohortBuilderService = cohortBuilderService;
    this.conceptSetMapper = conceptSetMapper;
    this.clock = clock;
    this.userProvider = userProvider;
  }

  public ConceptSet copyAndSave(
      Long fromConceptSetId,
      Long fromWorkspaceId,
      String newConceptSetName,
      DbUser creator,
      Long toWorkspaceId) {
    final DbConceptSet existingConceptSet = getDbConceptSet(fromWorkspaceId, fromConceptSetId);
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

    return saveDbConceptSet(dbConceptSetCopy);
  }

  public ConceptSet createConceptSet(
      CreateConceptSetRequest request, DbUser creator, Long workspaceId) {
    DbConceptSet dbConceptSet = conceptSetMapper.clientToDbModel(request, workspaceId, creator);
    if (dbConceptSet.getConceptSetConceptIds().size() > MAX_CONCEPTS_PER_SET) {
      throw new ConflictException(
          String.format("Exceeded %d concept set limit", MAX_CONCEPTS_PER_SET));
    }
    return saveDbConceptSet(dbConceptSet);
  }

  public ConceptSet updateConceptSet(Long workspaceId, Long conceptSetId, ConceptSet conceptSet) {
    DbConceptSet dbConceptSet = getDbConceptSet(workspaceId, conceptSetId);
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
    return saveDbConceptSet(dbConceptSet);
  }

  public ConceptSet updateConceptSetConcepts(
      Long workspaceId, Long conceptSetId, UpdateConceptSetRequest request) {
    DbConceptSet dbConceptSet = getDbConceptSet(workspaceId, conceptSetId);

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

    dbConceptSet.setLastModifiedTime(Timestamp.from(clock.instant()));
    return saveDbConceptSet(dbConceptSet);
  }

  public void delete(Long conceptSetId) {
    conceptSetDao.deleteById(conceptSetId);
  }

  /** Read concept-sets with hydrated collection of concept ids. */
  public ConceptSet getConceptSet(Long workspaceId, Long conceptSetId) {
    DbConceptSet dbConceptSet = getDbConceptSet(workspaceId, conceptSetId);
    return toHydratedConcepts(
        conceptSetMapper.dbModelToClient(dbConceptSet, conceptBigQueryService));
  }

  public Optional<DbConceptSet> findById(Long id){
    return conceptSetDao.findById(id);
  }

  public List<ConceptSet> findAll(List<Long> conceptSetIds) {
    return StreamSupport.stream(conceptSetDao.findAllById(conceptSetIds).spliterator(), false)
        .map(conceptSetMapper::dbModelToClient)
        .collect(Collectors.toList());
  }

  public List<DbConceptSet> findAllByConceptSetIdIn(Collection<Long> conceptSetIds) {
    return conceptSetDao.findAllByConceptSetIdIn(conceptSetIds);
  }

  /**
   * Read concept-sets with empty collection of concept ids. If the collection of concept ids needs
   * to be populated please use: {@link ConceptSetService#getConceptSet(Long, Long)}. In most cases,
   * there is not a need to load all the concept ids, so this call should be sufficient.
   */
  public List<ConceptSet> findByWorkspaceId(long workspaceId) {
    return conceptSetDao.findByWorkspaceId(workspaceId).stream()
        .map(conceptSetMapper::dbModelToClient)
        .collect(Collectors.toList());
  }

  public Integer countConceptsInConceptSet(Long conceptSetId) {
    return conceptSetDao.countByConceptSetId(conceptSetId);
  }

  @Transactional
  public DbConceptSet cloneConceptSetAndConceptIds(
      DbConceptSet dbConceptSet, DbWorkspace targetWorkspace) {
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
    dbConceptSetClone.setLastModifiedBy(userProvider.get().getUsername());
    return conceptSetDao.save(dbConceptSetClone);
  }

  public List<DbConceptSet> getConceptSets(DbWorkspace workspace) {
    // Allows for fetching concept sets for a workspace once its collection is no longer
    // bound to a session.
    return conceptSetDao.findByWorkspaceId(workspace.getWorkspaceId());
  }

  public Optional<DbConceptSet> maybeGetDbConceptSet(Long workspaceId, Long conceptSetId) {
    return conceptSetDao.findByWorkspaceIdAndConceptSetId(workspaceId, conceptSetId);
  }

  public DbConceptSet getDbConceptSet(Long workspaceId, Long conceptSetId) {
    return maybeGetDbConceptSet(workspaceId, conceptSetId)
        .orElseThrow(
            () ->
                new NotFoundException(
                    String.format(
                        "ConceptSet not found for workspaceId: %d and conceptSetId: %d",
                        workspaceId, conceptSetId)));
  }

  private ConceptSet saveDbConceptSet(DbConceptSet dbConceptSet) {
    dbConceptSet.setLastModifiedBy(userProvider.get().getUsername());
    try {
      return toHydratedConcepts(conceptSetMapper.dbModelToClient(conceptSetDao.save(dbConceptSet)));
    } catch (DataIntegrityViolationException e) {
      throw new ConflictException(
          String.format("Concept set %s already exists.", dbConceptSet.getName()));
    } catch (OptimisticLockException e) {
      throw new ConflictException("Failed due to concurrent concept set modification");
    }
  }

  private ConceptSet toHydratedConcepts(ConceptSet conceptSet) {
    Set<DbConceptSetConceptId> dbConceptSetConceptIds =
        conceptSetDao
            .findById(conceptSet.getId())
            .orElseThrow(
                () ->
                    new NotFoundException(
                        String.format("Concept set %s does not exist", conceptSet.getId())))
            .getConceptSetConceptIds();
    List<Criteria> criteriaList =
        cohortBuilderService.findCriteriaByDomainIdAndConceptIds(
            conceptSet.getDomain().toString(), dbConceptSetConceptIds);
    return conceptSet.criteriums(criteriaList);
  }
}
