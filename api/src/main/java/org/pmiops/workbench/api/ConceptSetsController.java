package org.pmiops.workbench.api;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import com.google.common.collect.Streams;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Provider;
import javax.persistence.OptimisticLockException;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.cdr.dao.ConceptDao;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.model.CommonStorageEnums;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.Concept;
import org.pmiops.workbench.model.ConceptSet;
import org.pmiops.workbench.model.ConceptSetListResponse;
import org.pmiops.workbench.model.CopyRequest;
import org.pmiops.workbench.model.CreateConceptSetRequest;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.Surveys;
import org.pmiops.workbench.model.UpdateConceptSetRequest;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConceptSetsController implements ConceptSetsApiDelegate {

  private static final int MAX_CONCEPTS_PER_SET = 1000;
  private static final String CONCEPT_CLASS_ID_QUESTION = "Question";
  private static final int INITIAL_VERSION = 1;

  private final WorkspaceService workspaceService;
  private final ConceptSetDao conceptSetDao;
  private final ConceptDao conceptDao;
  private final UserRecentResourceService userRecentResourceService;
  private final ConceptBigQueryService conceptBigQueryService;
  private final Clock clock;

  private Provider<DbUser> userProvider;

  @VisibleForTesting int maxConceptsPerSet;

  static final Function<DbConceptSet, ConceptSet> TO_CLIENT_CONCEPT_SET =
      new Function<DbConceptSet, ConceptSet>() {
        @Override
        public ConceptSet apply(DbConceptSet conceptSet) {
          ConceptSet result =
              new ConceptSet()
                  .etag(Etags.fromVersion(conceptSet.getVersion()))
                  .lastModifiedTime(conceptSet.getLastModifiedTime().getTime())
                  .creationTime(conceptSet.getCreationTime().getTime())
                  .description(conceptSet.getDescription())
                  .id(conceptSet.getConceptSetId())
                  .name(conceptSet.getName())
                  .domain(conceptSet.getDomainEnum())
                  .participantCount(conceptSet.getParticipantCount())
                  .survey(conceptSet.getSurveysEnum());
          if (conceptSet.getCreator() != null) {
            result.creator(conceptSet.getCreator().getEmail());
          }
          return result;
        }
      };

  private static final Function<ConceptSet, DbConceptSet> FROM_CLIENT_CONCEPT_SET =
      new Function<ConceptSet, DbConceptSet>() {
        @Override
        public DbConceptSet apply(ConceptSet conceptSet) {
          DbConceptSet dbConceptSet = new DbConceptSet();
          dbConceptSet.setDomainEnum(conceptSet.getDomain());
          if (conceptSet.getSurvey() != null) {
            dbConceptSet.setSurveysEnum(conceptSet.getSurvey());
          }
          if (dbConceptSet.getDomainEnum() == null) {
            throw new BadRequestException(
                "Domain " + conceptSet.getDomain() + " is not allowed for concept sets");
          }
          if (conceptSet.getEtag() != null) {
            dbConceptSet.setVersion(Etags.toVersion(conceptSet.getEtag()));
          }
          dbConceptSet.setDescription(conceptSet.getDescription());
          dbConceptSet.setName(conceptSet.getName());
          return dbConceptSet;
        }
      };

  private static final Ordering<Concept> CONCEPT_NAME_ORDERING =
      Ordering.from(String.CASE_INSENSITIVE_ORDER).onResultOf(Concept::getConceptName);

  @Autowired
  ConceptSetsController(
      WorkspaceService workspaceService,
      ConceptSetDao conceptSetDao,
      ConceptDao conceptDao,
      ConceptBigQueryService conceptBigQueryService,
      UserRecentResourceService userRecentResourceService,
      Provider<DbUser> userProvider,
      Clock clock) {
    this.workspaceService = workspaceService;
    this.conceptSetDao = conceptSetDao;
    this.conceptDao = conceptDao;
    this.conceptBigQueryService = conceptBigQueryService;
    this.userRecentResourceService = userRecentResourceService;
    this.userProvider = userProvider;
    this.clock = clock;
    this.maxConceptsPerSet = MAX_CONCEPTS_PER_SET;
  }

  @VisibleForTesting
  public void setUserProvider(Provider<DbUser> userProvider) {
    this.userProvider = userProvider;
  }

  @Override
  public ResponseEntity<ConceptSet> createConceptSet(
      String workspaceNamespace, String workspaceId, CreateConceptSetRequest request) {
    DbWorkspace workspace =
        workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);
    if (request.getAddedIds() == null || request.getAddedIds().size() == 0) {
      throw new BadRequestException("Cannot create a concept set with no concepts");
    }
    DbConceptSet dbConceptSet = FROM_CLIENT_CONCEPT_SET.apply(request.getConceptSet());
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    dbConceptSet.setCreator(userProvider.get());
    dbConceptSet.setWorkspaceId(workspace.getWorkspaceId());
    dbConceptSet.setCreationTime(now);
    dbConceptSet.setLastModifiedTime(now);
    dbConceptSet.setVersion(INITIAL_VERSION);
    dbConceptSet.setParticipantCount(0);
    if (request.getAddedIds() != null && !request.getAddedIds().isEmpty()) {
      addConceptsToSet(dbConceptSet, request.getAddedIds());
      if (dbConceptSet.getConceptIds().size() > maxConceptsPerSet) {
        throw new BadRequestException("Exceeded " + maxConceptsPerSet + " in concept set");
      }
      String omopTable = ConceptSetDao.DOMAIN_TO_TABLE_NAME.get(dbConceptSet.getDomainEnum());
      dbConceptSet.setParticipantCount(
          conceptBigQueryService.getParticipantCountForConcepts(
              omopTable, dbConceptSet.getConceptIds()));
    }

    try {
      dbConceptSet = conceptSetDao.save(dbConceptSet);
      userRecentResourceService.updateConceptSetEntry(
          workspace.getWorkspaceId(),
          userProvider.get().getUserId(),
          dbConceptSet.getConceptSetId());
    } catch (DataIntegrityViolationException e) {
      throw new BadRequestException(
          String.format(
              "Concept set \"/%s/%s/%s\" already exists.",
              workspaceNamespace, workspaceId, dbConceptSet.getName()));
    }
    return ResponseEntity.ok(toClientConceptSet(dbConceptSet));
  }

  private ConceptSet toClientConceptSet(DbConceptSet conceptSet) {
    ConceptSet result = TO_CLIENT_CONCEPT_SET.apply(conceptSet);
    if (!conceptSet.getConceptIds().isEmpty()) {
      Iterable<org.pmiops.workbench.cdr.model.Concept> concepts =
          conceptDao.findAll(conceptSet.getConceptIds());
      result.setConcepts(
          Streams.stream(concepts)
              .map(ConceptsController.TO_CLIENT_CONCEPT)
              .sorted(CONCEPT_NAME_ORDERING)
              .collect(Collectors.toList()));
    }
    return result;
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteConceptSet(
      String workspaceNamespace, String workspaceId, Long conceptSetId) {
    DbConceptSet conceptSet =
        getDbConceptSet(workspaceNamespace, workspaceId, conceptSetId, WorkspaceAccessLevel.WRITER);
    conceptSetDao.delete(conceptSet.getConceptSetId());
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<ConceptSet> getConceptSet(
      String workspaceNamespace, String workspaceId, Long conceptSetId) {
    DbConceptSet conceptSet =
        getDbConceptSet(workspaceNamespace, workspaceId, conceptSetId, WorkspaceAccessLevel.READER);
    return ResponseEntity.ok(toClientConceptSet(conceptSet));
  }

  @Override
  public ResponseEntity<ConceptSetListResponse> getConceptSetsInWorkspace(
      String workspaceNamespace, String workspaceId) {
    DbWorkspace workspace =
        workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);

    List<DbConceptSet> conceptSets = conceptSetDao.findByWorkspaceId(workspace.getWorkspaceId());
    ConceptSetListResponse response = new ConceptSetListResponse();
    // Concept sets in the list response will *not* have concepts under them, as this could be
    // a lot of data... you need to open up a concept set to see what concepts are within it.
    response.setItems(
        conceptSets.stream()
            .map(TO_CLIENT_CONCEPT_SET)
            .sorted(Comparator.comparing(c -> c.getName()))
            .collect(Collectors.toList()));
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<ConceptSetListResponse> getSurveyConceptSetsInWorkspace(
      String workspaceNamespace, String workspaceId, String surveyName) {
    DbWorkspace workspace =
        workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    short surveyId =
        CommonStorageEnums.surveysToStorage(Surveys.fromValue(surveyName.toUpperCase()));
    List<DbConceptSet> conceptSets =
        conceptSetDao.findByWorkspaceIdAndSurvey(workspace.getWorkspaceId(), surveyId);
    ConceptSetListResponse response = new ConceptSetListResponse();
    response.setItems(
        conceptSets.stream()
            .map(TO_CLIENT_CONCEPT_SET)
            .sorted(Comparator.comparing(c -> c.getName()))
            .collect(Collectors.toList()));
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<ConceptSet> updateConceptSet(
      String workspaceNamespace, String workspaceId, Long conceptSetId, ConceptSet conceptSet) {
    DbConceptSet dbConceptSet =
        getDbConceptSet(workspaceNamespace, workspaceId, conceptSetId, WorkspaceAccessLevel.WRITER);
    if (Strings.isNullOrEmpty(conceptSet.getEtag())) {
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

  private void addConceptsToSet(DbConceptSet dbConceptSet, List<Long> addedIds) {
    Domain domainEnum = dbConceptSet.getDomainEnum();
    Iterable<org.pmiops.workbench.cdr.model.Concept> concepts = conceptDao.findAll(addedIds);
    List<org.pmiops.workbench.cdr.model.Concept> mismatchedConcepts =
        ImmutableList.copyOf(concepts).stream()
            .filter(concept -> !concept.getConceptClassId().equals(CONCEPT_CLASS_ID_QUESTION))
            .filter(
                concept -> {
                  Domain domain = CommonStorageEnums.domainIdToDomain(concept.getDomainId());
                  return !domainEnum.equals(domain);
                })
            .collect(Collectors.toList());
    if (!mismatchedConcepts.isEmpty()) {
      String mismatchedConceptIds =
          Joiner.on(", ")
              .join(
                  mismatchedConcepts.stream()
                      .map(org.pmiops.workbench.cdr.model.Concept::getConceptId)
                      .collect(Collectors.toList()));
      throw new BadRequestException(
          String.format("Concepts [%s] are not in domain %s", mismatchedConceptIds, domainEnum));
    }

    dbConceptSet.getConceptIds().addAll(addedIds);
  }

  @Override
  public ResponseEntity<ConceptSet> updateConceptSetConcepts(
      String workspaceNamespace,
      String workspaceId,
      Long conceptSetId,
      UpdateConceptSetRequest request) {
    DbConceptSet dbConceptSet =
        getDbConceptSet(workspaceNamespace, workspaceId, conceptSetId, WorkspaceAccessLevel.WRITER);

    Set<Long> allConceptSetIds = dbConceptSet.getConceptIds();
    if (request.getAddedIds() != null) {
      allConceptSetIds.addAll(request.getAddedIds());
    }
    int sizeOfAllConceptSetIds = allConceptSetIds.stream().collect(Collectors.toSet()).size();
    if (request.getRemovedIds() != null
        && request.getRemovedIds().size() == sizeOfAllConceptSetIds) {
      throw new BadRequestException("Concept Set must have at least one concept");
    }

    if (Strings.isNullOrEmpty(request.getEtag())) {
      throw new BadRequestException("missing required update field 'etag'");
    }
    int version = Etags.toVersion(request.getEtag());
    if (dbConceptSet.getVersion() != version) {
      throw new ConflictException("Attempted to modify outdated concept set version");
    }

    if (request.getAddedIds() != null) {
      addConceptsToSet(dbConceptSet, request.getAddedIds());
    }
    if (request.getRemovedIds() != null) {
      dbConceptSet.getConceptIds().removeAll(request.getRemovedIds());
    }
    if (dbConceptSet.getConceptIds().size() > maxConceptsPerSet) {
      throw new BadRequestException("Exceeded " + maxConceptsPerSet + " in concept set");
    }
    if (dbConceptSet.getConceptIds().isEmpty()) {
      dbConceptSet.setParticipantCount(0);
    } else {
      String omopTable = ConceptSetDao.DOMAIN_TO_TABLE_NAME.get(dbConceptSet.getDomainEnum());
      dbConceptSet.setParticipantCount(
          conceptBigQueryService.getParticipantCountForConcepts(
              omopTable, dbConceptSet.getConceptIds()));
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
  public ResponseEntity<ConceptSet> copyConceptSet(
      String fromWorkspaceNamespace,
      String fromWorkspaceId,
      String fromConceptSetId,
      CopyRequest copyRequest) {
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    workspaceService.enforceWorkspaceAccessLevel(
        fromWorkspaceNamespace, fromWorkspaceId, WorkspaceAccessLevel.READER);
    DbWorkspace toWorkspace =
        workspaceService.get(
            copyRequest.getToWorkspaceNamespace(), copyRequest.getToWorkspaceName());
    DbWorkspace fromWorkspace = workspaceService.get(fromWorkspaceNamespace, fromWorkspaceId);
    workspaceService.enforceWorkspaceAccessLevel(
        toWorkspace.getWorkspaceNamespace(),
        toWorkspace.getFirecloudName(),
        WorkspaceAccessLevel.WRITER);
    CdrVersionContext.setCdrVersionNoCheckAuthDomain(toWorkspace.getCdrVersion());
    if (toWorkspace.getCdrVersion().getCdrVersionId()
        != fromWorkspace.getCdrVersion().getCdrVersionId()) {
      throw new BadRequestException(
          "Target workspace does not have the same CDR version as current workspace");
    }
    DbConceptSet conceptSet = conceptSetDao.findOne(Long.valueOf(fromConceptSetId));
    if (conceptSet == null) {
      throw new NotFoundException(
          String.format(
              "Concept set %s does not exist",
              createResourcePath(fromWorkspaceNamespace, fromWorkspaceId, fromConceptSetId)));
    }
    DbConceptSet newConceptSet = new DbConceptSet(conceptSet);

    newConceptSet.setName(copyRequest.getNewName());
    newConceptSet.setCreator(userProvider.get());
    newConceptSet.setWorkspaceId(toWorkspace.getWorkspaceId());
    newConceptSet.setCreationTime(now);
    newConceptSet.setLastModifiedTime(now);
    newConceptSet.setVersion(INITIAL_VERSION);

    try {
      newConceptSet = conceptSetDao.save(newConceptSet);
    } catch (DataIntegrityViolationException e) {
      throw new ConflictException(
          String.format(
              "Concept set %s already exists.",
              createResourcePath(
                  toWorkspace.getWorkspaceNamespace(),
                  toWorkspace.getFirecloudName(),
                  newConceptSet.getName())));
    }
    userRecentResourceService.updateConceptSetEntry(
        toWorkspace.getWorkspaceId(),
        userProvider.get().getUserId(),
        newConceptSet.getConceptSetId());
    return ResponseEntity.ok(toClientConceptSet(newConceptSet));
  }

  private String createResourcePath(
      String workspaceNamespace, String workspaceFirecloudName, String identifier) {
    return String.format("\"/%s/%s/%s\"", workspaceNamespace, workspaceFirecloudName, identifier);
  }

  private DbConceptSet getDbConceptSet(
      String workspaceNamespace,
      String workspaceId,
      Long conceptSetId,
      WorkspaceAccessLevel workspaceAccessLevel) {
    DbWorkspace workspace =
        workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, workspaceAccessLevel);

    DbConceptSet conceptSet = conceptSetDao.findOne(conceptSetId);
    if (conceptSet == null || workspace.getWorkspaceId() != conceptSet.getWorkspaceId()) {
      throw new NotFoundException(
          String.format(
              "No concept set with ID %s in workspace %s.",
              conceptSetId, workspace.getFirecloudName()));
    }
    return conceptSet;
  }
}
