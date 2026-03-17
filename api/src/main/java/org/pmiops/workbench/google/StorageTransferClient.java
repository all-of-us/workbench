package org.pmiops.workbench.google;

import com.google.storagetransfer.v1.proto.TransferTypes;
import java.util.List;

public interface StorageTransferClient {
  String startBucketTransfer(
      String sourceBucket,
      String destinationBucket,
      String projectId,
      List<String> folders,
      String serviceAccountEmail);

  TransferTypes.TransferJob getTransferJob(String projectId);
}
