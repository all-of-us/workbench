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
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * A delegate to be called by the {@link WorkspacesApiController}}.
 * Should be implemented as a controller but without the {@link org.springframework.stereotype.Controller} annotation.
 * Instead, use spring to autowire this class into the {@link WorkspacesApiController}.
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T11:49:26.055-05:00")

public interface WorkspacesApiDelegate {

    /**
     * @see WorkspacesApi#cloneNotebook
     */
    ResponseEntity<FileDetail> cloneNotebook(String workspaceNamespace,
        String workspaceId,
        String notebookName);

    /**
     * @see WorkspacesApi#cloneWorkspace
     */
    ResponseEntity<CloneWorkspaceResponse> cloneWorkspace(String workspaceNamespace,
        String workspaceId,
        CloneWorkspaceRequest body);

    /**
     * @see WorkspacesApi#copyNotebook
     */
    ResponseEntity<FileDetail> copyNotebook(String workspaceNamespace,
        String workspaceId,
        String notebookName,
        CopyRequest copyNotebookRequest);

    /**
     * @see WorkspacesApi#createWorkspace
     */
    ResponseEntity<Workspace> createWorkspace(Workspace workspace);

    /**
     * @see WorkspacesApi#deleteNotebook
     */
    ResponseEntity<EmptyResponse> deleteNotebook(String workspaceNamespace,
        String workspaceId,
        String notebookName);

    /**
     * @see WorkspacesApi#deleteWorkspace
     */
    ResponseEntity<EmptyResponse> deleteWorkspace(String workspaceNamespace,
        String workspaceId);

    /**
     * @see WorkspacesApi#getFirecloudWorkspaceUserRoles
     */
    ResponseEntity<WorkspaceUserRolesResponse> getFirecloudWorkspaceUserRoles(String workspaceNamespace,
        String workspaceId);

    /**
     * @see WorkspacesApi#getNoteBookList
     */
    ResponseEntity<List<FileDetail>> getNoteBookList(String workspaceNamespace,
        String workspaceId);

    /**
     * @see WorkspacesApi#getNotebookLockingMetadata
     */
    ResponseEntity<NotebookLockingMetadataResponse> getNotebookLockingMetadata(String workspaceNamespace,
        String workspaceId,
        String notebookName);

    /**
     * @see WorkspacesApi#getPublishedWorkspaces
     */
    ResponseEntity<WorkspaceResponseListResponse> getPublishedWorkspaces();

    /**
     * @see WorkspacesApi#getUserRecentWorkspaces
     */
    ResponseEntity<RecentWorkspaceResponse> getUserRecentWorkspaces();

    /**
     * @see WorkspacesApi#getWorkspace
     */
    ResponseEntity<WorkspaceResponse> getWorkspace(String workspaceNamespace,
        String workspaceId);

    /**
     * @see WorkspacesApi#getWorkspaces
     */
    ResponseEntity<WorkspaceResponseListResponse> getWorkspaces();

    /**
     * @see WorkspacesApi#getWorkspacesForReview
     */
    ResponseEntity<WorkspaceListResponse> getWorkspacesForReview();

    /**
     * @see WorkspacesApi#publishWorkspace
     */
    ResponseEntity<EmptyResponse> publishWorkspace(String workspaceNamespace,
        String workspaceId);

    /**
     * @see WorkspacesApi#readOnlyNotebook
     */
    ResponseEntity<ReadOnlyNotebookResponse> readOnlyNotebook(String workspaceNamespace,
        String workspaceId,
        String notebookName);

    /**
     * @see WorkspacesApi#renameNotebook
     */
    ResponseEntity<FileDetail> renameNotebook(String workspaceNamespace,
        String workspaceId,
        NotebookRename notebookRename);

    /**
     * @see WorkspacesApi#reviewWorkspace
     */
    ResponseEntity<EmptyResponse> reviewWorkspace(String workspaceNamespace,
        String workspaceId,
        ResearchPurposeReviewRequest review);

    /**
     * @see WorkspacesApi#shareWorkspace
     */
    ResponseEntity<WorkspaceUserRolesResponse> shareWorkspace(String workspaceNamespace,
        String workspaceId,
        ShareWorkspaceRequest body);

    /**
     * @see WorkspacesApi#unpublishWorkspace
     */
    ResponseEntity<EmptyResponse> unpublishWorkspace(String workspaceNamespace,
        String workspaceId);

    /**
     * @see WorkspacesApi#updateRecentWorkspaces
     */
    ResponseEntity<RecentWorkspaceResponse> updateRecentWorkspaces(String workspaceNamespace,
        String workspaceId);

    /**
     * @see WorkspacesApi#updateWorkspace
     */
    ResponseEntity<Workspace> updateWorkspace(String workspaceNamespace,
        String workspaceId,
        UpdateWorkspaceRequest workspace);

}
