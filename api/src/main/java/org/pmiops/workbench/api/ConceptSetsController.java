package org.pmiops.workbench.api;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.apache.commons.collections4.CollectionUtils;
import org.pmiops.workbench.cdr.CdrVersionContext;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.cdr.model.DbConcept;
import org.pmiops.workbench.concept.ConceptService;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.dataset.BigQueryTableInfo;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbStorageEnums;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConceptSetsController implements ConceptSetsApiDelegate {

  private static final int MAX_CONCEPTS_PER_SET = 1000;
  private static final String CONCEPT_CLASS_ID_QUESTION = "Question";
  private static final int INITIAL_VERSION = 1;

  private final WorkspaceService workspaceService;
  private final ConceptSetService conceptSetService;
  private final ConceptService conceptService;
  private final UserRecentResourceService userRecentResourceService;
  private final ConceptBigQueryService conceptBigQueryService;
  private final Clock clock;

  private final Provider<DbUser> userProvider;

  @VisibleForTesting int maxConceptsPerSet;

  @Autowired
  ConceptSetsController(
      WorkspaceService workspaceService,
      ConceptSetService conceptSetService,
      ConceptService conceptService,
      ConceptBigQueryService conceptBigQueryService,
      UserRecentResourceService userRecentResourceService,
      Provider<DbUser> userProvider,
      Clock clock) {
    this.workspaceService = workspaceService;
    this.conceptSetService = conceptSetService;
    this.conceptService = conceptService;
    this.conceptBigQueryService = conceptBigQueryService;
    this.userRecentResourceService = userRecentResourceService;
    this.userProvider = userProvider;
    this.clock = clock;
    this.maxConceptsPerSet = MAX_CONCEPTS_PER_SET;
  }

  @Override
  public ResponseEntity<ConceptSet> createConceptSet(
      String workspaceNamespace, String workspaceId, CreateConceptSetRequest request) {
    // Fail fast if request is not valid
    validateCreateConceptSetRequest(request);
    DbWorkspace workspace =
        workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);

    DbConceptSet dbConceptSet = fromClientConceptSet(request, workspace.getWorkspaceId());

    ConceptSet conceptSet = conceptSetService.save(dbConceptSet, );
    userRecentResourceService.updateConceptSetEntry(
        workspace.getWorkspaceId(), userProvider.get().getUserId(), conceptSet.getId());
    return ResponseEntity.ok(conceptSetService.toHydratedConcepts(conceptSet));
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteConceptSet(
      String workspaceNamespace, String workspaceId, Long conceptSetId) {
    conceptSetService.delete(conceptSetId);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<ConceptSet> getConceptSet(
      String workspaceNamespace, String workspaceId, Long conceptSetId) {
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);

    return ResponseEntity.ok(
        conceptSetService.toHydratedConcepts(conceptSetService.findOne(conceptSetId)));
  }

  @Override
  public ResponseEntity<ConceptSetListResponse> getConceptSetsInWorkspace(
      String workspaceNamespace, String workspaceId) {
    DbWorkspace workspace =
        workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);

    List<ConceptSet> conceptSets = conceptSetService.findByWorkspaceId(workspace.getWorkspaceId());
    ConceptSetListResponse response = new ConceptSetListResponse();
    // Concept sets in the list response will *not* have concepts under them, as this could be
    // a lot of data... you need to open up a concept set to see what concepts are within it.
    response.setItems(
        conceptSets.stream()
            .sorted(Comparator.comparing(ConceptSet::getName))
            .collect(Collectors.toList()));
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<ConceptSetListResponse> getSurveyConceptSetsInWorkspace(
      String workspaceNamespace, String workspaceId, String surveyName) {
    DbWorkspace workspace =
        workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);
    short surveyId = DbStorageEnums.surveysToStorage(Surveys.fromValue(surveyName.toUpperCase()));
    List<ConceptSet> conceptSets =
        conceptSetService.findByWorkspaceIdAndSurvey(workspace.getWorkspaceId(), surveyId);
    ConceptSetListResponse response =
        new ConceptSetListResponse()
            .items(
                conceptSets.stream()
                    .sorted(Comparator.comparing(ConceptSet::getName))
                    .collect(Collectors.toList()));
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<ConceptSet> updateConceptSet(
      String workspaceNamespace, String workspaceId, Long conceptSetId, ConceptSet conceptSet) {
    // Fail fast if etag isn't provided
    if (Strings.isNullOrEmpty(conceptSet.getEtag())) {
      throw new BadRequestException("missing required update field 'etag'");
    }
    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);

    validateAndUpdateDbConceptSet(dbConceptSet, conceptSet);
    return ResponseEntity.ok(
        conceptSetService.toHydratedConcepts(
            conceptSetService.updateConceptSet(conceptSetId, conceptSet)));
  }

  @Override
  public ResponseEntity<ConceptSet> updateConceptSetConcepts(
      String workspaceNamespace,
      String workspaceId,
      Long conceptSetId,
      UpdateConceptSetRequest request) {
    // Fail fast if etag isn't provided
    if (Strings.isNullOrEmpty(request.getEtag())) {
      throw new BadRequestException("missing required update field 'etag'");
    }

    workspaceService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
        workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);

    validateAndUpdateDbConceptSetConcept(dbConceptSet, request);
    return ResponseEntity.ok(
        conceptSetService.toHydratedConcepts(
            conceptSetService.updateConceptSetConcepts(conceptSetId, request)));
  }

  @Override
  // TODO: Refactor this -> https://precisionmedicineinitiative.atlassian.net/browse/RW-5428
  public ResponseEntity<ConceptSet> copyConceptSet(
      String fromWorkspaceNamespace,
      String fromWorkspaceId,
      String fromConceptSetId,
      CopyRequest copyRequest) {
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    workspaceService.enforceWorkspaceAccessLevelAndRegisteredAuthDomain(
        fromWorkspaceNamespace, fromWorkspaceId, WorkspaceAccessLevel.READER);
    DbWorkspace toWorkspace =
        workspaceService.get(
            copyRequest.getToWorkspaceNamespace(), copyRequest.getToWorkspaceName());
    DbWorkspace fromWorkspace = workspaceService.get(fromWorkspaceNamespace, fromWorkspaceId);
    workspaceService.enforceWorkspaceAccessLevelAndRegisteredAuthDomain(
        toWorkspace.getWorkspaceNamespace(),
        toWorkspace.getFirecloudName(),
        WorkspaceAccessLevel.WRITER);
    CdrVersionContext.setCdrVersionNoCheckAuthDomain(toWorkspace.getCdrVersion());
    if (toWorkspace.getCdrVersion().getCdrVersionId()
        != fromWorkspace.getCdrVersion().getCdrVersionId()) {
      throw new BadRequestException(
          "Target workspace does not have the same CDR version as current workspace");
    }
    final DbConceptSet existingConceptSet =
        conceptSetService
            .findDbConceptSet(Long.valueOf(fromConceptSetId))
            .orElseThrow(
                () ->
                    new NotFoundException(
                        String.format("Concept set %s does not exist", fromConceptSetId)));
    DbConceptSet newConceptSet = new DbConceptSet(existingConceptSet);

    newConceptSet.setName(copyRequest.getNewName());
    newConceptSet.setCreator(userProvider.get());
    newConceptSet.setWorkspaceId(toWorkspace.getWorkspaceId());
    newConceptSet.setCreationTime(now);
    newConceptSet.setLastModifiedTime(now);
    newConceptSet.setVersion(INITIAL_VERSION);

    ConceptSet conceptSet = conceptSetService.save(newConceptSet, );
    userRecentResourceService.updateConceptSetEntry(
        toWorkspace.getWorkspaceId(), userProvider.get().getUserId(), conceptSet.getId());
    return ResponseEntity.ok(conceptSetService.toHydratedConcepts(conceptSet));
  }

  private void addConceptsToSet(DbConceptSet dbConceptSet, List<Long> addedIds) {
    Domain domainEnum = dbConceptSet.getDomainEnum();
    Iterable<DbConcept> concepts = conceptService.findAll(addedIds);
    List<DbConcept> mismatchedConcepts =
        ImmutableList.copyOf(concepts).stream()
            .filter(concept -> !concept.getConceptClassId().equals(CONCEPT_CLASS_ID_QUESTION))
            .filter(
                concept -> {
                  Domain domain =
                      Domain.PHYSICALMEASUREMENT.equals(domainEnum)
                          ? Domain.PHYSICALMEASUREMENT
                          : DbStorageEnums.domainIdToDomain(concept.getDomainId());
                  return !domainEnum.equals(domain);
                })
            .collect(Collectors.toList());
    if (!mismatchedConcepts.isEmpty()) {
      String mismatchedConceptIds =
          Joiner.on(", ")
              .join(
                  mismatchedConcepts.stream()
                      .map(DbConcept::getConceptId)
                      .collect(Collectors.toList()));
      throw new BadRequestException(
          String.format("Concepts [%s] are not in domain %s", mismatchedConceptIds, domainEnum));
    }

    dbConceptSet.getConceptIds().addAll(addedIds);
  }

  private DbConceptSet fromClientConceptSet(CreateConceptSetRequest request, long workspaceId) {
    ConceptSet conceptSet = request.getConceptSet();
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());
    DbConceptSet dbConceptSet = new DbConceptSet();
    dbConceptSet.setDomainEnum(conceptSet.getDomain());
    if (conceptSet.getSurvey() != null) {
      dbConceptSet.setSurveysEnum(conceptSet.getSurvey());
    }
    if (dbConceptSet.getDomainEnum() == null) {
      throw new BadRequestException(
          "Domain " + conceptSet.getDomain() + " is not allowed for concept sets");
    }
    Optional.ofNullable(conceptSet.getEtag())
        .ifPresent(etag -> dbConceptSet.setVersion(Etags.toVersion(etag)));
    dbConceptSet.setDescription(conceptSet.getDescription());
    dbConceptSet.setName(conceptSet.getName());
    dbConceptSet.setCreator(userProvider.get());
    dbConceptSet.setWorkspaceId(workspaceId);
    dbConceptSet.setCreationTime(now);
    dbConceptSet.setLastModifiedTime(now);
    dbConceptSet.setVersion(INITIAL_VERSION);
    addConceptsToSet(dbConceptSet, request.getAddedIds());
    if (dbConceptSet.getConceptIds().size() > maxConceptsPerSet) {
      throw new BadRequestException("Exceeded " + maxConceptsPerSet + " in concept set");
    }
    String omopTable = BigQueryTableInfo.getTableName(request.getConceptSet().getDomain());
    dbConceptSet.setParticipantCount(
        conceptBigQueryService.getParticipantCountForConcepts(
            dbConceptSet.getDomainEnum(), omopTable, dbConceptSet.getConceptIds()));
    return dbConceptSet;
  }

  private void validateAndUpdateDbConceptSet(DbConceptSet dbConceptSet, ConceptSet conceptSet) {
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
    // In case of rename ConceptDet does not have concepts
    if (!CollectionUtils.isEmpty(conceptSet.getConcepts())) {
      validateCreateConceptSetRequest(
          dbConceptSet,
          conceptSet.getConcepts().stream()
              .map(Concept::getConceptId)
              .collect(Collectors.toList()));
    }
  }

  private void validateAndUpdateDbConceptSetConcept(
      DbConceptSet dbConceptSet, UpdateConceptSetRequest request) {
    Set<Long> allConceptSetIds = dbConceptSet.getConceptIds();
    if (request.getAddedIds() != null) {
      allConceptSetIds.addAll(request.getAddedIds());
    }
    int sizeOfAllConceptSetIds = allConceptSetIds.size();
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
    if (dbConceptSet.getConceptIds().isEmpty()) {
      dbConceptSet.setParticipantCount(0);
    } else {
      String omopTable = BigQueryTableInfo.getTableName(dbConceptSet.getDomainEnum());
      dbConceptSet.setParticipantCount(
          conceptBigQueryService.getParticipantCountForConcepts(
              dbConceptSet.getDomainEnum(), omopTable, dbConceptSet.getConceptIds()));
    }
    validateCreateConceptSetRequest(dbConceptSet, new ArrayList<>(dbConceptSet.getConceptIds()));
  }

  private void validateCreateConceptSetRequest(CreateConceptSetRequest request) {
    Optional.ofNullable(request.getConceptSet().getDomain())
        .orElseThrow(() -> new BadRequestException("Domain cannot be null"));
    if (CollectionUtils.isEmpty(request.getAddedIds())) {
      throw new BadRequestException("Cannot create a concept set with no concepts");
    }
  }
}
