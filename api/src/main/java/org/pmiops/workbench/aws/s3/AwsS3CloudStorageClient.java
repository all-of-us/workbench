package org.pmiops.workbench.aws.s3;

import java.util.ArrayList;
import java.util.List;
import org.pmiops.workbench.model.FileDetail;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

@Service
public class AwsS3CloudStorageClient {

  public List<FileDetail> getFilesFromS3(String bucketName, String prefix) {

    // awsResourceApiProvider.get().getAwsS3StorageFolderCredential();

    List<FileDetail> files = new ArrayList<>();

    ListObjectsV2Request v2Request =
        ListObjectsV2Request.builder().bucket(bucketName).prefix(prefix + "/").build();
    // .requestCredentialsProvider(new AWSStaticCredentialsProvider(awsCreds));
    ListObjectsV2Response listObjectsV2Result = getS3StorageClient().listObjectsV2(v2Request);

    for (S3Object objectSummary : listObjectsV2Result.contents()) {
      if (!objectSummary.key().endsWith("ipynb")) continue;
      FileDetail fileDetail = new FileDetail();
      fileDetail.setName(objectSummary.key().substring(objectSummary.key().lastIndexOf("/") + 1));
      fileDetail.setLastModifiedTime(objectSummary.lastModified().toEpochMilli());
      files.add(fileDetail);
    }

    return files;
  }

  private S3Client getS3StorageClient() {
    S3Client s3Client = S3Client.builder().region(Region.US_EAST_1).build();
    return s3Client;
  }
}
