package org.pmiops.workbench.google;

import com.google.storagetransfer.v1.proto.TransferTypes.TransferOperation;
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

  TransferOperation.Status getTransferJobStatus(String projectId, String workspaceNamespace);
}
