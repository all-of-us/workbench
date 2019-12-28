package org.pmiops.workbench.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.auth.oauth2.GoogleCredentials;
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

  Set<BlobId> getExistingBlobIdsIn(List<BlobId> id);

  void writeFile(String bucketName, String fileName, byte[] bytes);

  void copyBlob(BlobId from, BlobId to);

  JSONObject getJiraCredentials();

  JSONObject getElasticCredentials();

  GoogleCredentials getGSuiteAdminCredentials() throws IOException;

  GoogleCredentials getFireCloudAdminCredentials() throws IOException;

  GoogleCredentials getCloudResourceManagerAdminCredentials() throws IOException;

  GoogleCredentials getDefaultServiceAccountCredentials() throws IOException;

  GoogleCredentials getGarbageCollectionServiceAccountCredentials(String garbageCollectionEmail)
      throws IOException;

  Map<String, String> getMetadata(String bucketName, String objectPath);

  Blob getBlob(String bucketName, String objectPath);

  JSONObject readBlobAsJson(Blob blob);

  void deleteBlob(BlobId blobId);

  String getMoodleApiKey();
}
