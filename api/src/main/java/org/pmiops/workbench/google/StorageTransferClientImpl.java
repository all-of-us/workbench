package org.pmiops.workbench.google;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.longrunning.Operation;
import com.google.storagetransfer.v1.proto.StorageTransferServiceClient;
import com.google.storagetransfer.v1.proto.TransferProto;
import com.google.storagetransfer.v1.proto.TransferTypes;
import com.google.storagetransfer.v1.proto.TransferTypes.TransferOperation;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class StorageTransferClientImpl implements StorageTransferClient {
  private static final Logger logger = LoggerFactory.getLogger(StorageTransferClientImpl.class);

  @Override
  public String createTransferJob(
      String sourceBucket,
      String destinationBucket,
      String workspaceNamespace,
      String projectId,
      List<String> folders,
      String serviceAccountEmail) {
    try {

      String jobName = "transferJobs/migration-" + workspaceNamespace;
      com.google.gson.JsonObject transferSpec = new com.google.gson.JsonObject();
      com.google.gson.JsonObject gcsSource = new com.google.gson.JsonObject();
      gcsSource.addProperty("bucketName", sourceBucket);
      com.google.gson.JsonObject gcsSink = new com.google.gson.JsonObject();
      gcsSink.addProperty("bucketName", destinationBucket);
      transferSpec.add("gcsDataSource", gcsSource);
      transferSpec.add("gcsDataSink", gcsSink);

      if (folders != null && !folders.isEmpty()) {
        com.google.gson.JsonObject objectConditions = new com.google.gson.JsonObject();
        com.google.gson.JsonArray prefixes = new com.google.gson.JsonArray();
        folders.forEach(prefixes::add);
        objectConditions.add("includePrefixes", prefixes);
        transferSpec.add("objectConditions", objectConditions);
      }

      com.google.gson.JsonObject requestBody = new com.google.gson.JsonObject();
      requestBody.addProperty("name", jobName);
      requestBody.addProperty("projectId", projectId);
      requestBody.addProperty("serviceAccount", serviceAccountEmail);
      requestBody.addProperty("status", "ENABLED");
      requestBody.add("transferSpec", transferSpec);

      GoogleCredentials credentials =
          GoogleCredentials.getApplicationDefault()
              .createScoped("https://www.googleapis.com/auth/cloud-platform");
      credentials.refreshIfExpired();
      String accessToken = credentials.getAccessToken().getTokenValue();

      java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
      java.net.http.HttpRequest request =
          java.net.http.HttpRequest.newBuilder()
              .uri(java.net.URI.create("https://storagetransfer.googleapis.com/v1/transferJobs"))
              .header("Authorization", "Bearer " + accessToken)
              .header("Content-Type", "application/json")
              .POST(java.net.http.HttpRequest.BodyPublishers.ofString(requestBody.toString()))
              .build();

      java.net.http.HttpResponse<String> response =
          httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() != 200) {
        throw new RuntimeException(
            "STS REST API failed: " + response.statusCode() + " " + response.body());
      }

      com.google.gson.JsonObject responseJson =
          com.google.gson.JsonParser.parseString(response.body()).getAsJsonObject();
      String createdJobName = responseJson.get("name").getAsString();
      logger.info("STS transfer job created successfully: {}", createdJobName);
      return createdJobName;

    } catch (Exception e) {
      logger.error("STS bucket transfer failed: {}", e.getMessage(), e);
      throw new RuntimeException("Failed to start STS bucket transfer", e);
    }
  }

  @Override
  public void runTransferJob(String projectId, String jobName) {
    try (StorageTransferServiceClient client = StorageTransferServiceClient.create()) {

      TransferProto.RunTransferJobRequest request =
          TransferProto.RunTransferJobRequest.newBuilder()
              .setJobName(jobName)
              .setProjectId(projectId)
              .build();
      client.runTransferJobAsync(request);

    } catch (IOException e) {
      throw new RuntimeException("Failed to run STS transfer job for project: " + projectId, e);
    }
  }

  @Override
  public void deleteTransferJob(String projectId, String jobName) {
    try (StorageTransferServiceClient client = StorageTransferServiceClient.create()) {

      TransferProto.DeleteTransferJobRequest request =
          TransferProto.DeleteTransferJobRequest.newBuilder()
              .setJobName(jobName)
              .setProjectId(projectId)
              .build();
      client.deleteTransferJob(request);

    } catch (IOException e) {
      throw new RuntimeException("Failed to delete STS transfer job for project: " + projectId, e);
    }
  }

  @Override
  public TransferOperation getTransferJobStatus(String projectId, String workspaceNamespace) {
    try (StorageTransferServiceClient client = StorageTransferServiceClient.create()) {
      TransferOperation transferOperation =
          TransferTypes.TransferOperation.newBuilder()
              .setStatus(TransferTypes.TransferOperation.Status.QUEUED)
              .build();

      String jobName = "transferJobs/migration-" + workspaceNamespace;

      TransferProto.GetTransferJobRequest request =
          TransferProto.GetTransferJobRequest.newBuilder()
              .setJobName(jobName)
              .setProjectId(projectId)
              .build();
      TransferTypes.TransferJob transferJob = client.getTransferJob(request);
      String latestOperationName = transferJob.getLatestOperationName();
      if (!latestOperationName.isEmpty()) {
        Operation operation = client.getOperationsClient().getOperation(latestOperationName);
        transferOperation =
            TransferTypes.TransferOperation.parseFrom(operation.getMetadata().getValue());
      }
      return transferOperation;
    } catch (IOException e) {
      throw new RuntimeException("Failed to get STS transfer job for project: " + projectId, e);
    }
  }
}
