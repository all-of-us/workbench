package org.pmiops.workbench.api;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Ordering;
import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableList;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Provider;
import javax.persistence.OptimisticLockException;
import org.pmiops.workbench.cdr.dao.ConceptDao;
import org.pmiops.workbench.cdr.dao.ConceptService;
import org.pmiops.workbench.cdr.dao.ConceptSynonymDao;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.WorkspaceService;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.Concept;
import org.pmiops.workbench.model.ConceptSet;
import org.pmiops.workbench.model.ConceptSetListResponse;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.UpdateConceptSetRequest;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConceptSetsController implements ConceptSetsApiDelegate {

  private static final int MAX_CONCEPTS_PER_SET = 1000;

  private final WorkspaceService workspaceService;
  private final ConceptSetDao conceptSetDao;
  private final ConceptDao conceptDao;
  private final ConceptService conceptService;
  private final ConceptSynonymDao conceptSynonymDao;
  private final Clock clock;

  private Provider<User> userProvider;

  @VisibleForTesting
  int maxConceptsPerSet;

  private static final Function<org.pmiops.workbench.db.model.ConceptSet, ConceptSet> TO_CLIENT_CONCEPT_SET =
      new Function<org.pmiops.workbench.db.model.ConceptSet, ConceptSet>() {
        @Override
        public ConceptSet apply(org.pmiops.workbench.db.model.ConceptSet conceptSet) {
          ConceptSet result = new ConceptSet()
              .etag(Etags.fromVersion(conceptSet.getVersion()))
              .lastModifiedTime(conceptSet.getLastModifiedTime().getTime())
              .creationTime(conceptSet.getCreationTime().getTime())
              .description(conceptSet.getDescription())
              .id(conceptSet.getConceptSetId())
              .name(conceptSet.getName())
              .domain(conceptSet.getDomainEnum());
          if (conceptSet.getCreator() != null) {
            result.creator(conceptSet.getCreator().getEmail());
          }
          return result;
        }
      };

  private static final Function<ConceptSet, org.pmiops.workbench.db.model.ConceptSet> FROM_CLIENT_CONCEPT_SET =
      new Function<ConceptSet, org.pmiops.workbench.db.model.ConceptSet>() {
        @Override
        public org.pmiops.workbench.db.model.ConceptSet apply(ConceptSet conceptSet) {
          org.pmiops.workbench.db.model.ConceptSet dbConceptSet =
              new org.pmiops.workbench.db.model.ConceptSet();
          dbConceptSet.setDomainEnum(conceptSet.getDomain());
          if (dbConceptSet.getDomainEnum() == null) {
            throw new BadRequestException(
                "Domain " + conceptSet.getDomain() + " is not allowed for concept sets");
          }
          dbConceptSet.setDescription(conceptSet.getDescription());
          dbConceptSet.setName(conceptSet.getName());
          return dbConceptSet;
        }
      };

  private static final Ordering<Concept> CONCEPT_NAME_ORDERING =
      Ordering.from(String.CASE_INSENSITIVE_ORDER).onResultOf(Concept::getConceptName);

  @Autowired
  ConceptSetsController(WorkspaceService workspaceService, ConceptSetDao conceptSetDao,
      ConceptDao conceptDao, ConceptSynonymDao conceptSynonymDao, ConceptService conceptService, Provider<User> userProvider, Clock clock) {
    this.workspaceService = workspaceService;
    this.conceptSetDao = conceptSetDao;
    this.conceptDao = conceptDao;
    this.conceptService = conceptService;
    this.conceptSynonymDao = conceptSynonymDao;
    this.userProvider = userProvider;
    this.clock = clock;
    this.maxConceptsPerSet = MAX_CONCEPTS_PER_SET;
  }

  @VisibleForTesting
  public void setUserProvider(Provider<User> userProvider) {
    this.userProvider = userProvider;
  }

  @Override
  public ResponseEntity<ConceptSet> createConceptSet(String workspaceNamespace, String workspaceId,
      ConceptSet conceptSet) {
    Workspace workspace = workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);
    org.pmiops.workbench.db.model.ConceptSet dbConceptSet = FROM_CLIENT_CONCEPT_SET.apply(conceptSet);
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    dbConceptSet.setCreator(userProvider.get());
    dbConceptSet.setWorkspaceId(workspace.getWorkspaceId());
    dbConceptSet.setCreationTime(now);
    dbConceptSet.setLastModifiedTime(now);
    dbConceptSet.setVersion(1);
    try {
      dbConceptSet = conceptSetDao.save(dbConceptSet);
      // TODO: add recent resource entry for concept sets [RW-1129]
    } catch (DataIntegrityViolationException e) {
      throw new BadRequestException(String.format(
          "Concept set \"/%s/%s/%s\" already exists.",
          workspaceNamespace, workspaceId, dbConceptSet.getName()));
    }
    return ResponseEntity.ok(toClientConceptSet(dbConceptSet));
  }

  private ConceptSet toClientConceptSet(org.pmiops.workbench.db.model.ConceptSet conceptSet) {
    ConceptSet result = TO_CLIENT_CONCEPT_SET.apply(conceptSet);
    if (!conceptSet.getConceptIds().isEmpty()) {
      Iterable<org.pmiops.workbench.cdr.model.Concept> concepts = conceptDao.findAll(conceptSet.getConceptIds());
      List<org.pmiops.workbench.cdr.model.Concept> conceptList = conceptService.fetchConceptSynonyms(Lists.newArrayList(concepts));
      result.setConcepts(conceptList.stream()
              .map(ConceptsController.TO_CLIENT_CONCEPT)
              .sorted(CONCEPT_NAME_ORDERING)
              .collect(Collectors.toList()));
    }
    return result;
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteConceptSet(String workspaceNamespace,
      String workspaceId, Long conceptSetId) {
    org.pmiops.workbench.db.model.ConceptSet conceptSet =
        getDbConceptSet(workspaceNamespace, workspaceId, conceptSetId, WorkspaceAccessLevel.WRITER);
    conceptSetDao.delete(conceptSet.getConceptSetId());
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<ConceptSet> getConceptSet(String workspaceNamespace, String workspaceId,
      Long conceptSetId) {
    org.pmiops.workbench.db.model.ConceptSet conceptSet =
        getDbConceptSet(workspaceNamespace, workspaceId, conceptSetId, WorkspaceAccessLevel.READER);
    return ResponseEntity.ok(toClientConceptSet(conceptSet));
  }

  @Override
  public ResponseEntity<ConceptSetListResponse> getConceptSetsInWorkspace(String workspaceNamespace,
      String workspaceId) {
    Workspace workspace =
        workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(workspaceNamespace, workspaceId,
          WorkspaceAccessLevel.READER);

    List<org.pmiops.workbench.db.model.ConceptSet> conceptSets =
        conceptSetDao.findByWorkspaceId(workspace.getWorkspaceId());
    ConceptSetListResponse response = new ConceptSetListResponse();
    // Concept sets in the list response will *not* have concepts under them, as this could be
    // a lot of data... you need to open up a concept set to see what concepts are within it.
    response.setItems(conceptSets.stream()
        .map(TO_CLIENT_CONCEPT_SET)
        .sorted(Comparator.comparing(c -> c.getName()))
        .collect(Collectors.toList()));
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<ConceptSet> updateConceptSet(String workspaceNamespace, String workspaceId,
      Long conceptSetId, ConceptSet conceptSet) {
    org.pmiops.workbench.db.model.ConceptSet dbConceptSet = getDbConceptSet(
        workspaceNamespace, workspaceId, conceptSetId, WorkspaceAccessLevel.WRITER);
    if(Strings.isNullOrEmpty(conceptSet.getEtag())) {
      throw new BadRequestException("missing required update field 'etag'");
    }
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
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    dbConceptSet.setLastModifiedTime(now);
    try {
      dbConceptSet = conceptSetDao.save(dbConceptSet);
      // TODO: add recent resource entry for concept sets [RW-1129]
    } catch (OptimisticLockException e) {
      throw new ConflictException("Failed due to concurrent concept set modification");
    }
    return ResponseEntity.ok(toClientConceptSet(dbConceptSet));
  }

  @Override
  public ResponseEntity<ConceptSet> updateConceptSetConcepts(String workspaceNamespace,
      String workspaceId, Long conceptSetId, UpdateConceptSetRequest request) {
    org.pmiops.workbench.db.model.ConceptSet dbConceptSet = getDbConceptSet(
        workspaceNamespace, workspaceId, conceptSetId, WorkspaceAccessLevel.WRITER);
    if(Strings.isNullOrEmpty(request.getEtag())) {
      throw new BadRequestException("missing required update field 'etag'");
    }
    int version = Etags.toVersion(request.getEtag());
    if (dbConceptSet.getVersion() != version) {
      throw new ConflictException("Attempted to modify outdated concept set version");
    }

    if (request.getAddedIds() != null) {
      final Domain domainEnum = dbConceptSet.getDomainEnum();
      Iterable<org.pmiops.workbench.cdr.model.Concept> concepts = conceptDao.findAll(request.getAddedIds());
      conceptService.fetchConceptSynonyms(Lists.newArrayList(concepts));
      List<org.pmiops.workbench.cdr.model.Concept> mismatchedConcepts =
          ImmutableList.copyOf(concepts).stream().filter(concept -> {
          Collection<Domain> domain = ConceptsController.DOMAIN_MAP.inverse().get(concept.getDomainId());
          return domain == null || !domain.contains(domainEnum);
        }).collect(Collectors.toList());
      if (!mismatchedConcepts.isEmpty()) {
        String mismatchedConceptIds = Joiner.on(", ").join(mismatchedConcepts.stream()
            .map(org.pmiops.workbench.cdr.model.Concept::getConceptId).collect(Collectors.toList()));
        throw new BadRequestException(
            String.format("Concepts [%s] are not in domain %s", mismatchedConceptIds, domainEnum));
      }

      dbConceptSet.getConceptIds().addAll(request.getAddedIds());
    }
    if (request.getRemovedIds() != null) {
      dbConceptSet.getConceptIds().removeAll(request.getRemovedIds());
    }
    if (dbConceptSet.getConceptIds().size() > maxConceptsPerSet) {
      throw new BadRequestException("Exceeded " + maxConceptsPerSet + " in concept set");
    }

    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    dbConceptSet.setLastModifiedTime(now);
    try {
      dbConceptSet = conceptSetDao.save(dbConceptSet);
      // TODO: add recent resource entry for concept sets [RW-1129]
    } catch (OptimisticLockException e) {
      throw new ConflictException("Failed due to concurrent concept set modification");
    }
    return ResponseEntity.ok(toClientConceptSet(dbConceptSet));
  }

  private org.pmiops.workbench.db.model.ConceptSet getDbConceptSet(String workspaceNamespace,
      String workspaceId, Long conceptSetId, WorkspaceAccessLevel workspaceAccessLevel) {
    Workspace workspace = workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, workspaceAccessLevel);

    org.pmiops.workbench.db.model.ConceptSet conceptSet =
        conceptSetDao.findOne(conceptSetId);
    if (conceptSet == null || workspace.getWorkspaceId() != conceptSet.getWorkspaceId()) {
      throw new NotFoundException(String.format(
          "No concept set with ID %s in workspace %s.", conceptSetId, workspace.getFirecloudName()));
    }
    return conceptSet;
  }
}
