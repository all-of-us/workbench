package org.pmiops.workbench.google;

import com.google.storagetransfer.v1.proto.TransferTypes;

public interface StorageTransferClient {
  String startBucketTransfer(String sourceBucket, String destinationBucket, String projectId);

  TransferTypes.TransferJob getTransferJob(String projectId);
}
