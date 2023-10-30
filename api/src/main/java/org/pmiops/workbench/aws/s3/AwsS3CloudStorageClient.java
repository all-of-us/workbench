package org.pmiops.workbench.aws.s3;

import bio.terra.workspace.model.AwsCredential;
import bio.terra.workspace.model.AwsS3StorageFolderAttributes;
import java.util.ArrayList;
import java.util.List;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.wsm.WsmClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

@Service
public class AwsS3CloudStorageClient {

  private final WsmClient wsmClient;

  @Autowired
  public AwsS3CloudStorageClient(WsmClient wsmClient) {
    this.wsmClient = wsmClient;
  }

  public List<FileDetail> getFilesFromS3(
      AwsS3StorageFolderAttributes s3BucketAttributes, AwsCredential awsCredential) {

    List<FileDetail> files = new ArrayList<>();

    ListObjectsV2Request v2Request =
        ListObjectsV2Request.builder()
            .bucket(s3BucketAttributes.getBucketName())
            .prefix(s3BucketAttributes.getPrefix() + "/")
            .build();
    ListObjectsV2Response listObjectsV2Result =
        getS3StorageClient(awsCredential).listObjectsV2(v2Request);

    for (S3Object objectSummary : listObjectsV2Result.contents()) {
      if (!objectSummary.key().endsWith("ipynb")) {
        continue;
      }
      FileDetail fileDetail = new FileDetail();
      fileDetail.setName(objectSummary.key().substring(objectSummary.key().lastIndexOf("/") + 1));
      fileDetail.setLastModifiedTime(objectSummary.lastModified().toEpochMilli());
      files.add(fileDetail);
    }

    return files;
  }

  private S3Client getS3StorageClient(AwsCredential awsCredential) {
    S3Client s3Client =
        S3Client.builder()
            .region(Region.US_EAST_1)
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsSessionCredentials.create(
                        awsCredential.getAccessKeyId(),
                        awsCredential.getSecretAccessKey(),
                        awsCredential.getSessionToken())))
            .build();
    return s3Client;
  }
}
