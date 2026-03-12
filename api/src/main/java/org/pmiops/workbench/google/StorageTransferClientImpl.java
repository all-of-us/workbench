package org.pmiops.workbench.google;

import com.google.storagetransfer.v1.proto.StorageTransferServiceClient;
import com.google.storagetransfer.v1.proto.TransferProto;
import com.google.storagetransfer.v1.proto.TransferProto.CreateTransferJobRequest;
import com.google.storagetransfer.v1.proto.TransferTypes;
import com.google.storagetransfer.v1.proto.TransferTypes.GcsData;
import com.google.storagetransfer.v1.proto.TransferTypes.TransferJob;
import com.google.storagetransfer.v1.proto.TransferTypes.TransferSpec;
import java.io.IOException;
import org.springframework.stereotype.Service;

@Service
public class StorageTransferClientImpl implements StorageTransferClient {

  @Override
  public String startBucketTransfer(
      String sourceBucket, String destinationBucket, String projectId) {
    try (StorageTransferServiceClient client = StorageTransferServiceClient.create()) {

      String jobName = "transferJobs/migration-" + projectId;

      TransferSpec transferSpec =
          TransferSpec.newBuilder()
              .setGcsDataSource(GcsData.newBuilder().setBucketName(sourceBucket).build())
              .setGcsDataSink(GcsData.newBuilder().setBucketName(destinationBucket).build())
              .build();
      TransferJob transferJob =
          TransferJob.newBuilder()
              .setName(jobName)
              .setProjectId(projectId)
              .setTransferSpec(transferSpec)
              .setStatus(TransferJob.Status.ENABLED)
              .build();
      CreateTransferJobRequest request =
          CreateTransferJobRequest.newBuilder().setTransferJob(transferJob).build();

      TransferJob createdJob = client.createTransferJob(request);
      return createdJob.getName();

    } catch (IOException e) {
      throw new RuntimeException("Failed to start STS bucket transfer", e);
    }
  }

  @Override
  public TransferTypes.TransferJob getTransferJob(String projectId) {
    try (StorageTransferServiceClient client = StorageTransferServiceClient.create()) {

      String jobName = "transferJobs/migration-" + projectId;

      TransferProto.GetTransferJobRequest request =
          TransferProto.GetTransferJobRequest.newBuilder()
              .setJobName(jobName)
              .setProjectId(projectId)
              .build();
      return client.getTransferJob(request);

    } catch (IOException e) {
      throw new RuntimeException("Failed to get STS transfer job for project: " + projectId, e);
    }
  }
}
