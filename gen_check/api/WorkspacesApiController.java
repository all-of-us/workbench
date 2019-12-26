package org.pmiops.workbench.api;

import org.pmiops.workbench.model.CloneWorkspaceRequest;
import org.pmiops.workbench.model.CloneWorkspaceResponse;
import org.pmiops.workbench.model.CopyRequest;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.ErrorResponse;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.model.NotebookLockingMetadataResponse;
import org.pmiops.workbench.model.NotebookRename;
import org.pmiops.workbench.model.ReadOnlyNotebookResponse;
import org.pmiops.workbench.model.RecentWorkspaceResponse;
import org.pmiops.workbench.model.ResearchPurposeReviewRequest;
import org.pmiops.workbench.model.ShareWorkspaceRequest;
import org.pmiops.workbench.model.UpdateWorkspaceRequest;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceListResponse;
import org.pmiops.workbench.model.WorkspaceResponse;
import org.pmiops.workbench.model.WorkspaceResponseListResponse;
import org.pmiops.workbench.model.WorkspaceUserRolesResponse;

import io.swagger.annotations.*;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import javax.validation.constraints.*;
import javax.validation.Valid;
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T14:42:01.876-06:00")

@Controller
public class WorkspacesApiController implements WorkspacesApi {
    private final WorkspacesApiDelegate delegate;

    @org.springframework.beans.factory.annotation.Autowired
    public WorkspacesApiController(WorkspacesApiDelegate delegate) {
        this.delegate = delegate;
    }


    public ResponseEntity<FileDetail> cloneNotebook(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "",required=true ) @PathVariable("notebookName") String notebookName) {
        // do some magic!
        return delegate.cloneNotebook(workspaceNamespace, workspaceId, notebookName);
    }

    public ResponseEntity<CloneWorkspaceResponse> cloneWorkspace(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = ""  )  @Valid @RequestBody CloneWorkspaceRequest body) {
        // do some magic!
        return delegate.cloneWorkspace(workspaceNamespace, workspaceId, body);
    }

    public ResponseEntity<FileDetail> copyNotebook(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "",required=true ) @PathVariable("notebookName") String notebookName,
        @ApiParam(value = "" ,required=true )  @Valid @RequestBody CopyRequest copyNotebookRequest) {
        // do some magic!
        return delegate.copyNotebook(workspaceNamespace, workspaceId, notebookName, copyNotebookRequest);
    }

    public ResponseEntity<Workspace> createWorkspace(@ApiParam(value = "workspace definition"  )  @Valid @RequestBody Workspace workspace) {
        // do some magic!
        return delegate.createWorkspace(workspace);
    }

    public ResponseEntity<EmptyResponse> deleteNotebook(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "",required=true ) @PathVariable("notebookName") String notebookName) {
        // do some magic!
        return delegate.deleteNotebook(workspaceNamespace, workspaceId, notebookName);
    }

    public ResponseEntity<EmptyResponse> deleteWorkspace(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId) {
        // do some magic!
        return delegate.deleteWorkspace(workspaceNamespace, workspaceId);
    }

    public ResponseEntity<WorkspaceUserRolesResponse> getFirecloudWorkspaceUserRoles(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId) {
        // do some magic!
        return delegate.getFirecloudWorkspaceUserRoles(workspaceNamespace, workspaceId);
    }

    public ResponseEntity<List<FileDetail>> getNoteBookList(@ApiParam(value = "workspaceNamespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "workspaceId",required=true ) @PathVariable("workspaceId") String workspaceId) {
        // do some magic!
        return delegate.getNoteBookList(workspaceNamespace, workspaceId);
    }

    public ResponseEntity<NotebookLockingMetadataResponse> getNotebookLockingMetadata(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "",required=true ) @PathVariable("notebookName") String notebookName) {
        // do some magic!
        return delegate.getNotebookLockingMetadata(workspaceNamespace, workspaceId, notebookName);
    }

    public ResponseEntity<WorkspaceResponseListResponse> getPublishedWorkspaces() {
        // do some magic!
        return delegate.getPublishedWorkspaces();
    }

    public ResponseEntity<RecentWorkspaceResponse> getUserRecentWorkspaces() {
        // do some magic!
        return delegate.getUserRecentWorkspaces();
    }

    public ResponseEntity<WorkspaceResponse> getWorkspace(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId) {
        // do some magic!
        return delegate.getWorkspace(workspaceNamespace, workspaceId);
    }

    public ResponseEntity<WorkspaceResponseListResponse> getWorkspaces() {
        // do some magic!
        return delegate.getWorkspaces();
    }

    public ResponseEntity<WorkspaceListResponse> getWorkspacesForReview() {
        // do some magic!
        return delegate.getWorkspacesForReview();
    }

    public ResponseEntity<EmptyResponse> publishWorkspace(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId) {
        // do some magic!
        return delegate.publishWorkspace(workspaceNamespace, workspaceId);
    }

    public ResponseEntity<ReadOnlyNotebookResponse> readOnlyNotebook(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "",required=true ) @PathVariable("notebookName") String notebookName) {
        // do some magic!
        return delegate.readOnlyNotebook(workspaceNamespace, workspaceId, notebookName);
    }

    public ResponseEntity<FileDetail> renameNotebook(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "new name for notebook" ,required=true )  @Valid @RequestBody NotebookRename notebookRename) {
        // do some magic!
        return delegate.renameNotebook(workspaceNamespace, workspaceId, notebookRename);
    }

    public ResponseEntity<EmptyResponse> reviewWorkspace(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "result of the research purpose review"  )  @Valid @RequestBody ResearchPurposeReviewRequest review) {
        // do some magic!
        return delegate.reviewWorkspace(workspaceNamespace, workspaceId, review);
    }

    public ResponseEntity<WorkspaceUserRolesResponse> shareWorkspace(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "users to share the workspace with"  )  @Valid @RequestBody ShareWorkspaceRequest body) {
        // do some magic!
        return delegate.shareWorkspace(workspaceNamespace, workspaceId, body);
    }

    public ResponseEntity<EmptyResponse> unpublishWorkspace(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId) {
        // do some magic!
        return delegate.unpublishWorkspace(workspaceNamespace, workspaceId);
    }

    public ResponseEntity<RecentWorkspaceResponse> updateRecentWorkspaces(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId) {
        // do some magic!
        return delegate.updateRecentWorkspaces(workspaceNamespace, workspaceId);
    }

    public ResponseEntity<Workspace> updateWorkspace(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "workspace definition"  )  @Valid @RequestBody UpdateWorkspaceRequest workspace) {
        // do some magic!
        return delegate.updateWorkspace(workspaceNamespace, workspaceId, workspace);
    }

}
