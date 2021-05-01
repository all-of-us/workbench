package org.pmiops.workbench.api;

import com.google.common.base.Strings;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.apache.commons.collections4.CollectionUtils;
import org.pmiops.workbench.conceptset.ConceptSetService;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.ConceptSet;
import org.pmiops.workbench.model.ConceptSetListResponse;
import org.pmiops.workbench.model.CopyRequest;
import org.pmiops.workbench.model.CreateConceptSetRequest;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.UpdateConceptSetRequest;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConceptSetsController implements ConceptSetsApiDelegate {

  private final WorkspaceAuthService workspaceAuthService;
  private final ConceptSetService conceptSetService;
  private final UserRecentResourceService userRecentResourceService;
  private final Provider<DbUser> userProvider;

  @Autowired
  ConceptSetsController(
      WorkspaceAuthService workspaceAuthService,
      ConceptSetService conceptSetService,
      UserRecentResourceService userRecentResourceService,
      Provider<DbUser> userProvider) {
    this.workspaceAuthService = workspaceAuthService;
    this.conceptSetService = conceptSetService;
    this.userRecentResourceService = userRecentResourceService;
    this.userProvider = userProvider;
  }

  @Override
  public ResponseEntity<ConceptSet> createConceptSet(
      String workspaceNamespace, String workspaceId, CreateConceptSetRequest request) {
    // Fail fast if request is not valid
    validateCreateConceptSetRequest(request);
    DbWorkspace workspace =
        workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);

    ConceptSet conceptSet =
        conceptSetService.createConceptSet(request, userProvider.get(), workspace.getWorkspaceId());
    userRecentResourceService.updateConceptSetEntry(
        workspace.getWorkspaceId(), userProvider.get().getUserId(), conceptSet.getId());
    return ResponseEntity.ok(conceptSet);
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteConceptSet(
      String workspaceNamespace, String workspaceId, Long conceptSetId) {
    DbWorkspace dbWorkspace =
        workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);
    // This method will throw a NotFoundException if no conceptSet exists for specified
    // conceptSetId and workspaceId
    ConceptSet conceptSet =
        conceptSetService.getConceptSet(dbWorkspace.getWorkspaceId(), conceptSetId);
    conceptSetService.delete(conceptSet.getId());
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<ConceptSet> getConceptSet(
      String workspaceNamespace, String workspaceId, Long conceptSetId) {
    DbWorkspace dbWorkspace =
        workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);

    return ResponseEntity.ok(
        conceptSetService.getConceptSet(dbWorkspace.getWorkspaceId(), conceptSetId));
  }

  @Override
  public ResponseEntity<ConceptSetListResponse> getConceptSetsInWorkspace(
      String workspaceNamespace, String workspaceId) {
    DbWorkspace workspace =
        workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, WorkspaceAccessLevel.READER);

    List<ConceptSet> conceptSets =
        conceptSetService.findByWorkspaceId(workspace.getWorkspaceId()).stream()
            .sorted(Comparator.comparing(ConceptSet::getName))
            .collect(Collectors.toList());
    return ResponseEntity.ok(new ConceptSetListResponse().items(conceptSets));
  }

  @Override
  public ResponseEntity<ConceptSet> updateConceptSet(
      String workspaceNamespace, String workspaceId, Long conceptSetId, ConceptSet conceptSet) {
    // Fail fast if etag isn't provided
    validateUpdateConceptSet(conceptSet);
    DbWorkspace dbWorkspace =
        workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);

    return ResponseEntity.ok(
        conceptSetService.updateConceptSet(dbWorkspace.getWorkspaceId(), conceptSetId, conceptSet));
  }

  @Override
  public ResponseEntity<ConceptSet> updateConceptSetConcepts(
      String workspaceNamespace,
      String workspaceId,
      Long conceptSetId,
      UpdateConceptSetRequest request) {
    // Fail fast if request isn't valid
    validateUpdateConceptSetConcepts(request);

    DbWorkspace dbWorkspace =
        workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            workspaceNamespace, workspaceId, WorkspaceAccessLevel.WRITER);

    return ResponseEntity.ok(
        conceptSetService.updateConceptSetConcepts(
            dbWorkspace.getWorkspaceId(), conceptSetId, request));
  }

  @Override
  public ResponseEntity<ConceptSet> copyConceptSet(
      String fromWorkspaceNamespace,
      String fromWorkspaceId,
      String fromConceptSetId,
      CopyRequest copyRequest) {
    DbWorkspace fromWorkspace =
        workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            fromWorkspaceNamespace, fromWorkspaceId, WorkspaceAccessLevel.READER);
    DbWorkspace toWorkspace =
        workspaceAuthService.getWorkspaceEnforceAccessLevelAndSetCdrVersion(
            copyRequest.getToWorkspaceNamespace(),
            copyRequest.getToWorkspaceName(),
            WorkspaceAccessLevel.WRITER);
    if (toWorkspace.getCdrVersion().getCdrVersionId()
        != fromWorkspace.getCdrVersion().getCdrVersionId()) {
      throw new BadRequestException(
          "Target workspace does not have the same CDR version as current workspace");
    }

    ConceptSet conceptSet =
        conceptSetService.copyAndSave(
            Long.valueOf(fromConceptSetId),
            fromWorkspace.getWorkspaceId(),
            copyRequest.getNewName(),
            userProvider.get(),
            toWorkspace.getWorkspaceId());
    userRecentResourceService.updateConceptSetEntry(
        toWorkspace.getWorkspaceId(), userProvider.get().getUserId(), conceptSet.getId());
    return ResponseEntity.ok(conceptSet);
  }

  private void validateCreateConceptSetRequest(CreateConceptSetRequest request) {
    Optional.ofNullable(request.getConceptSet().getDomain())
        .orElseThrow(() -> new BadRequestException("Domain cannot be null"));
    if (CollectionUtils.isEmpty(request.getAddedConceptSetConceptIds())) {
      throw new BadRequestException("Cannot create a concept set with no concepts");
    }
  }

  private void validateUpdateConceptSet(ConceptSet conceptSet) {
    if (Strings.isNullOrEmpty(conceptSet.getEtag())) {
      throw new BadRequestException("missing required update field 'etag'");
    }
    Optional.ofNullable(conceptSet.getDomain())
        .orElseThrow(() -> new BadRequestException("Domain cannot be null"));
  }

  private void validateUpdateConceptSetConcepts(UpdateConceptSetRequest request) {
    if (Strings.isNullOrEmpty(request.getEtag())) {
      throw new BadRequestException("missing required update field 'etag'");
    }
  }
}
