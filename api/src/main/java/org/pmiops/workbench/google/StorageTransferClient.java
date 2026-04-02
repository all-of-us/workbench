package org.pmiops.workbench.google;

import com.google.storagetransfer.v1.proto.TransferTypes;
import java.util.List;

public interface StorageTransferClient {
  String createTransferJob(
      String sourceBucket,
      String destinationBucket,
      String workspaceNamespace,
      String projectId,
      List<String> folders,
      String serviceAccountEmail);

  void runTransferJob(String projectId, String jobName);

  void deleteTransferJob(String projectId, String jobName);

  TransferTypes.TransferOperation getTransferJobStatus(String projectId, String workspaceNamespace);
}
