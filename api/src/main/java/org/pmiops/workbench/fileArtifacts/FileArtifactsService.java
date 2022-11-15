package org.pmiops.workbench.fileArtifacts;

import com.google.cloud.storage.Blob;
import java.util.List;
import org.json.JSONObject;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.model.KernelTypeEnum;

public interface FileArtifactsService {
  // The directory to hold file artifacts for jupyter and RStudio files.
  // TODO: Migrate to a common dir name
  String FILE_ARTIFACTS_WORKSPACE_DIRECTORY = "notebooks";
  String NOTEBOOK_EXTENSION = ".ipynb";
  String RSTUDIO_EXTENSION = ".rmd";

  static String withFileArtifactExtension(String fileArtifactName) {
    return fileArtifactName.endsWith(NOTEBOOK_EXTENSION)
        ? fileArtifactName
        : fileArtifactName.concat(NOTEBOOK_EXTENSION);
  }

  List<FileDetail> getFileArtifacts(String workspaceNamespace, String workspaceName);

  /**
   * Retrieve all fileArtifacts in the given cloud storage bucket. This method is authenticated as the
   * app engine service account, so authorization must be performed before calling this and the
   * input value should be trusted.
   */
  List<FileDetail> getFileArtifactsAsService(
      String bucketName, String workspaceNamespace, String workspaceName);

  boolean isFileArtifactBlob(Blob blob);

  FileDetail copyFileArtifact(
      String fromWorkspaceNamespace,
      String fromWorkspaceName,
      String fromFileArtifactName,
      String toWorkspaceNamespace,
      String toWorkspaceName,
      String newFileArtifactName);

  FileDetail cloneFileArtifact(String workspaceNamespace, String workspaceName, String fileArtifactName);

  void deleteFileArtifact(String workspaceNamespace, String workspaceName, String fileArtifactName);

  FileDetail renameFileArtifact(
      String workspaceNamespace, String workspaceName, String fileArtifactName, String newName);

  JSONObject getFileArtifactContents(String bucketName, String fileArtifactName);

  KernelTypeEnum getFileArtifactKernel(JSONObject fileArtifactFile);

  KernelTypeEnum getFileArtifactKernel(
      String workspaceNamespace, String workspaceName, String fileArtifactName);

  void saveFileArtifact(String bucketName, String fileArtifactName, JSONObject fileArtifactContents);

  public String convertFileArtifactToHtml(byte[] fileArtifact);

  String getReadOnlyHtml(String workspaceNamespace, String workspaceName, String fileArtifactName);

  String adminGetReadOnlyHtml(String workspaceNamespace, String workspaceName, String fileArtifactName);
}
