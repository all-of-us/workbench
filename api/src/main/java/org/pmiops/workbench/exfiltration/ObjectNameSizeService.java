package org.pmiops.workbench.exfiltration;

import com.google.cloud.storage.Blob;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.model.FileDetail;
import org.springframework.stereotype.Service;

@Service
public class ObjectNameSizeService {

  private static final Logger LOGGER = Logger.getLogger(ObjectNameSizeService.class.getName());

  final FireCloudService fireCloudService;
  final CloudStorageClient cloudStorageClient;

  public ObjectNameSizeService(
      FireCloudService fireCloudService, CloudStorageClient cloudStorageClient) {
    this.fireCloudService = fireCloudService;
    this.cloudStorageClient = cloudStorageClient;
  }

  public long calculateObjectNameLength(DbWorkspace workspace) {

    FirecloudWorkspaceResponse fsWorkspace =
        fireCloudService.getWorkspaceAsService(
            workspace.getWorkspaceNamespace(), workspace.getFirecloudName());

    final String bucketName = fsWorkspace.getWorkspace().getBucketName();
    final List<Blob> allBlobs = cloudStorageClient.getAllBlobs(bucketName);
    final List<FileDetail> fileDetailList =
        allBlobs.stream()
            .map(blob -> cloudStorageClient.blobToFileDetail(blob, bucketName))
            .collect(Collectors.toList());

    final long fileNameLengths = getFileNamesLengths(fileDetailList);

    return fileNameLengths;
  }

  private long getFileNamesLengths(List<FileDetail> fileDetailList) {
    long currentSumOfLengths = 0;
    for (FileDetail fileDetail : fileDetailList) {
      currentSumOfLengths += fileDetail.getName().length();
    }
    return currentSumOfLengths;
  }
}
