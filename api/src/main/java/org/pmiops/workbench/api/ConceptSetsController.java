package org.pmiops.workbench.api;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.inject.Provider;
import javax.persistence.OptimisticLockException;
import org.pmiops.workbench.cdr.dao.ConceptDao;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.WorkspaceService;
import org.pmiops.workbench.db.model.Cohort;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.Workspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.CohortListResponse;
import org.pmiops.workbench.model.Concept;
import org.pmiops.workbench.model.ConceptSet;
import org.pmiops.workbench.model.ConceptSetListResponse;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.UpdateConceptSetRequest;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConceptSetsController implements ConceptSetsApiDelegate {

  private final WorkspaceService workspaceService;
  private final ConceptSetDao conceptSetDao;
  private final ConceptDao conceptDao;
  private final Provider<User> userProvider;
  private final Clock clock;

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
          dbConceptSet.setDescription(conceptSet.getDescription());
          dbConceptSet.setName(conceptSet.getName());
          return dbConceptSet;
        }
      };

  private static final Ordering<Concept> CONCEPT_NAME_ORDERING =
      Ordering.from(String.CASE_INSENSITIVE_ORDER).onResultOf(Concept::getConceptName);

  @Autowired
  ConceptSetsController(WorkspaceService workspaceService, ConceptSetDao conceptSetDao,
      ConceptDao conceptDao, Provider<User> userProvider, Clock clock) {
    this.workspaceService = workspaceService;
    this.conceptSetDao = conceptSetDao;
    this.conceptDao = conceptDao;
    this.userProvider = userProvider;
    this.clock = clock;
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
      // TODO: add recent resource entry for concept sets?
    } catch (DataIntegrityViolationException e) {
      throw new BadRequestException(String.format(
          "Concept set \"/%s/%s/%d\" already exists.",
          workspaceNamespace, workspaceId, dbConceptSet.getName()));
    }
    return ResponseEntity.ok(toClientConceptSet(dbConceptSet));
  }

  private ConceptSet toClientConceptSet(org.pmiops.workbench.db.model.ConceptSet conceptSet) {
    ConceptSet result = TO_CLIENT_CONCEPT_SET.apply(conceptSet);
    if (!conceptSet.getConceptIds().isEmpty()) {
      result.setConcepts(ImmutableList.copyOf(conceptDao.findAll(conceptSet.getConceptIds())).stream().map(
            ConceptsController.TO_CLIENT_CONCEPT).sorted(CONCEPT_NAME_ORDERING).collect(Collectors.toList()));
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
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(workspaceNamespace, workspaceId,
        WorkspaceAccessLevel.READER);

    Workspace workspace = workspaceService.getRequiredWithConceptSets(workspaceNamespace, workspaceId);
    ConceptSetListResponse response = new ConceptSetListResponse();
    Set<org.pmiops.workbench.db.model.ConceptSet> conceptSets = workspace.getConceptSets();
    if (conceptSets != null) {
      Set<Long> conceptIds = Sets.newHashSet();
      // Get all the concept IDs for all concept sets, and look them all up at once.
      for (org.pmiops.workbench.db.model.ConceptSet conceptSet : conceptSets) {
        conceptIds.addAll(conceptSet.getConceptIds());
      }
      final Map<Long, Concept> conceptMap;
      if (conceptIds.size() > 0) {
        conceptMap = ImmutableList.copyOf(conceptDao.findAll(conceptIds)).stream()
            .map(ConceptsController.TO_CLIENT_CONCEPT).collect(
                Collectors.toMap(Concept::getConceptId, Function.identity()));
      } else {
        conceptMap = new HashMap<>();
      }
      // Look up the concepts by ID in the map constructed above.
      for (org.pmiops.workbench.db.model.ConceptSet conceptSet : conceptSets) {
        ConceptSet clientConceptSet = TO_CLIENT_CONCEPT_SET.apply(conceptSet);
        if (!conceptSet.getConceptIds().isEmpty()) {
          clientConceptSet.setConcepts(
              conceptSet.getConceptIds().stream().map((conceptId -> conceptMap.get(conceptId)))
                  .collect(Collectors.toList()));
        }
        response.addItemsItem(clientConceptSet);
      }
    }
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
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    dbConceptSet.setLastModifiedTime(now);
    try {
      dbConceptSet = conceptSetDao.save(dbConceptSet);
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
      dbConceptSet.getConceptIds().addAll(request.getAddedIds());
    }
    if (request.getRemovedIds() != null) {
      dbConceptSet.getConceptIds().removeAll(request.getRemovedIds());
    }
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    dbConceptSet.setLastModifiedTime(now);
    try {
      dbConceptSet = conceptSetDao.save(dbConceptSet);
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
