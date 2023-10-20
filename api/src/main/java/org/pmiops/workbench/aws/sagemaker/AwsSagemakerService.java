package org.pmiops.workbench.aws.sagemaker;

import bio.terra.workspace.model.AwsCredential;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import org.apache.http.client.utils.URIBuilder;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sagemaker.SageMakerClient;
import software.amazon.awssdk.services.sagemaker.model.*;

@Service
public class AwsSagemakerService {

  private static final Integer SAGEMAKER_SESSION_DURATION_SECONDS_MAX = 43200;

  public URL getAwsSageMakerProxyURL(String instanceName, AwsCredential awsCredential) {
    CreatePresignedNotebookInstanceUrlResponse createPresignedUrlResponse =
        getSageMakerClient(awsCredential)
            .createPresignedNotebookInstanceUrl(
                CreatePresignedNotebookInstanceUrlRequest.builder()
                    .notebookInstanceName(instanceName)
                    .sessionExpirationDurationInSeconds(SAGEMAKER_SESSION_DURATION_SECONDS_MAX)
                    .build());
    SdkHttpResponse httpResponse = createPresignedUrlResponse.sdkHttpResponse();
    if (!httpResponse.isSuccessful()) {
      throw new WorkbenchException(
          "Error creating presigned notebook instance url, "
              + httpResponse.statusText().orElse(String.valueOf(httpResponse.statusCode())));
    }
    try {
      return new URIBuilder(createPresignedUrlResponse.authorizedUrl())
          .addParameter("view", "lab")
          .build()
          .toURL();
    } catch (MalformedURLException | URISyntaxException e) {
      throw new WorkbenchException(e);
    }
  }

  public NotebookInstanceStatus getSageMakerNotebookInstanceStatus(
      String instanceName, AwsCredential awsCredential) {
    try {
      DescribeNotebookInstanceResponse describeResponse =
          getSageMakerClient(awsCredential)
              .describeNotebookInstance(
                  DescribeNotebookInstanceRequest.builder()
                      .notebookInstanceName(instanceName)
                      .build());
      SdkHttpResponse httpResponse = describeResponse.sdkHttpResponse();
      if (!httpResponse.isSuccessful()) {
        throw new WorkbenchException(
            "Error getting notebook instance, "
                + httpResponse.statusText().orElse(String.valueOf(httpResponse.statusCode())));
      }
      return describeResponse.notebookInstanceStatus();

    } catch (SdkException e) {
      checkException(e);
      throw new WorkbenchException("Error getting notebook instance", e);
    }
  }

  private SageMakerClient getSageMakerClient(AwsCredential awsCredential) {
    return SageMakerClient.builder()
        .region(Region.US_EAST_1)
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsSessionCredentials.create(
                    awsCredential.getAccessKeyId(),
                    awsCredential.getSecretAccessKey(),
                    awsCredential.getSessionToken())))
        .build();
  }

  private static void checkException(Exception ex) {
    if (ex instanceof SdkException) {
      String message = ex.getMessage();
      if (message.contains("not authorized to perform")) {
        throw new WorkbenchException(
            "Error performing notebook operation, check the instance name / permissions");
      } else if (message.contains("Unable to transition to")) {
        throw new WorkbenchException("Unable to perform notebook operation on cloud platform");
      }
    }
  }
}
