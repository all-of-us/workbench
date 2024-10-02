package org.pmiops.workbench.notebooks;

import com.google.cloud.storage.Blob;
import java.util.List;
import org.json.JSONObject;
import org.pmiops.workbench.model.AppType;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.model.KernelTypeEnum;

public interface NotebooksService {

  List<FileDetail> getNotebooks(String workspaceNamespace, String workspaceTerraName);

  /**
   * Retrieve all notebooks in the given cloud storage bucket. This method is authenticated as the
   * app engine service account, so authorization must be performed before calling this and the
   * input value should be trusted.
   */
  List<FileDetail> getNotebooksAsService(
      String bucketName, String workspaceNamespace, String workspaceTerraName);

  List<FileDetail> getAllNotebooksByAppType(
      String bucketName, String workspaceNamespace, String workspaceTerraName, AppType appType);

  List<FileDetail> getAllJupyterNotebooks(
      String bucketName, String workspaceNamespace, String workspaceTerraName);

  /**
   * Is this a notebook file which is managed (localized and delocalized) by the Workbench?
   *
   * @return TRUE only if it has a supported file extension and resides in the supported directory
   *     (and not a subdirectory)
   */
  boolean isManagedNotebookBlob(Blob blob);

  FileDetail copyNotebook(
      String fromWorkspaceNamespace,
      String fromWorkspaceTerraName,
      String fromNotebookNameWithExtension,
      String toWorkspaceNamespace,
      String toWorkspaceTerraName,
      String newNotebookNameWithExtension);

  FileDetail cloneNotebook(
      String workspaceNamespace, String workspaceTerraName, String notebookNameWithExtension);

  void deleteNotebook(String workspaceNamespace, String workspaceTerraName, String notebookName);

  FileDetail renameNotebook(
      String workspaceNamespace,
      String workspaceTerraName,
      String originalNameWithExtension,
      String newNameWithExtension);

  JSONObject getNotebookContents(String bucketName, String notebookName);

  KernelTypeEnum getNotebookKernel(JSONObject notebookFile);

  KernelTypeEnum getNotebookKernel(
      String workspaceNamespace, String workspaceTerraName, String notebookName);

  void saveNotebook(
      String bucketName, String notebookNameWithFileExtension, JSONObject notebookContents);

  String convertJupyterNotebookToHtml(byte[] notebook);

  String getReadOnlyHtml(String workspaceNamespace, String workspaceTerraName, String notebookName);

  String adminGetReadOnlyHtml(
      String workspaceNamespace, String workspaceTerraName, String notebookNameWithFileExtension);
}
