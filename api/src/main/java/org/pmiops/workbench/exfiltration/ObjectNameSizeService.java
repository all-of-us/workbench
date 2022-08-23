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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ObjectNameSizeService {

  private static final Logger LOGGER = Logger.getLogger(ObjectNameSizeService.class.getName());
  private static final long THRESHOLD = 100;

  @Autowired FireCloudService fireCloudService;
  @Autowired CloudStorageClient cloudStorageClient;

  public boolean objectNameSizeExceedsThreshold(DbWorkspace workspace) {

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

    if (fileNameLengths > THRESHOLD) {
      LOGGER.info(
          String.format(
              "An offending workspaces has been found for workspace ID: %d, bucket name: %s",
              workspace.getWorkspaceId(), bucketName));
      return true;
    }

    return false;
  }

  private long getFileNamesLengths(List<FileDetail> fileDetailList) {
    long currentSumOfLengths = 0;
    for (FileDetail fileDetail : fileDetailList) {
      currentSumOfLengths += fileDetail.getName().length();
    }
    return currentSumOfLengths;
  }
}
