package org.pmiops.workbench.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONObject;

/** Encapsulate Google APIs for interfacing with Google Cloud Storage. */
public interface CloudStorageService {

  String readInvitationKey();

  String readMandrillApiKey();

  String getImageUrl(String image_name);

  List<Blob> getBlobList(String bucketName);

  List<Blob> getBlobListForPrefix(String bucketName, String directory);

  Set<BlobId> blobsExist(List<BlobId> id);

  void writeFile(String bucketName, String fileName, byte[] bytes);

  void copyBlob(BlobId from, BlobId to);

  JSONObject getJiraCredentials();

  JSONObject getElasticCredentials();

  GoogleCredential getGSuiteAdminCredentials() throws IOException;

  GoogleCredential getFireCloudAdminCredentials() throws IOException;

  GoogleCredential getCloudResourceManagerAdminCredentials() throws IOException;

  GoogleCredential getDefaultServiceAccountCredentials() throws IOException;

  GoogleCredential getGarbageCollectionServiceAccountCredentials(String garbageCollectionEmail)
      throws IOException;

  JSONObject getFileAsJson(String bucketName, String fileName);

  Map<String, String> getMetadata(String bucketName, String objectPath);

  void deleteBlob(BlobId blobId);

  String getMoodleApiKey();
}
