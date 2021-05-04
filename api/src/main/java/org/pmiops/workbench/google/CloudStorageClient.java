package org.pmiops.workbench.google;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONObject;
import org.pmiops.workbench.model.FileDetail;

/** Encapsulate Google APIs for interfacing with Google Cloud Storage. */
public interface CloudStorageClient {

  String readMandrillApiKey();

  String getImageUrl(String image_name);

  /**
   * Get the first {@link com.google.api.gax.paging.Page} of results returned when listing the files
   * in a bucket
   *
   * @param bucketName the google bucket to list
   * @return the first Page of file Blobs, as a List
   */
  List<Blob> getBlobPage(String bucketName);

  /**
   * Get the first {@link com.google.api.gax.paging.Page} of results returned when listing the files
   * in a directory in a bucket
   *
   * @param bucketName the google bucket to list
   * @param directory the bucket directory to subset results to
   * @return the first Page of file Blobs, as a List
   */
  List<Blob> getBlobPageForPrefix(String bucketName, String directory);

  Set<BlobId> getExistingBlobIdsIn(List<BlobId> id);

  Blob writeFile(String bucketName, String fileName, byte[] bytes);

  void copyBlob(BlobId from, BlobId to);

  String getCredentialsBucketString(String objectPath);

  JSONObject getElasticCredentials();

  Map<String, String> getMetadata(String bucketName, String objectPath);

  Blob getBlob(String bucketName, String objectPath);

  JSONObject readBlobAsJson(Blob blob);

  void deleteBlob(BlobId blobId);

  String getMoodleApiKey();

  String getCaptchaServerKey();

  default FileDetail blobToFileDetail(Blob blob, String bucketName) {
    String[] parts = blob.getName().split("/");
    FileDetail fileDetail = new FileDetail();
    fileDetail.setName(parts[parts.length - 1]);
    fileDetail.setPath("gs://" + bucketName + "/" + blob.getName());
    fileDetail.setLastModifiedTime(blob.getUpdateTime());
    fileDetail.setSizeInBytes(blob.getSize());
    return fileDetail;
  }
}
