package org.pmiops.workbench.utils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.storage.Blob;
import java.util.HashSet;
import java.util.Set;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.notebooks.NotebookUtils;

public class MockNotebook {
  public Blob blob;
  public FileDetail fileDetail;

  public MockNotebook(String path, String bucketName, CloudStorageClient mockCloudStorageClient) {
    blob = mock(Blob.class);
    fileDetail = new FileDetail();
    Set<String> workspaceUsersSet = new HashSet<>();

    String[] parts = path.split("/");
    String fileName = parts[parts.length - 1];
    fileDetail.setName(fileName);

    when(blob.getName()).thenReturn(path);
    when(mockCloudStorageClient.blobToFileDetail(blob, bucketName, workspaceUsersSet))
        .thenReturn(fileDetail);
  }

  public static MockNotebook mockWithPath(
      String filename, String bucketName, CloudStorageClient mockCloudStorageClient) {
    return new MockNotebook(
        NotebookUtils.withNotebookPath(filename), bucketName, mockCloudStorageClient);
  }

  public static MockNotebook mockWithPathAndJupyterExtension(
      String filename, String bucketName, CloudStorageClient mockCloudStorageClient) {
    return mockWithPath(
        NotebookUtils.withJupyterNotebookExtension(filename), bucketName, mockCloudStorageClient);
  }
}
