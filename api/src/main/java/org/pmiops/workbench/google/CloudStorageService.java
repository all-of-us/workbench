package org.pmiops.workbench.google;

import com.google.auth.oauth2.ServiceAccountCredentials;
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

  ServiceAccountCredentials getGSuiteAdminCredentials() throws IOException;

  ServiceAccountCredentials getFireCloudAdminCredentials() throws IOException;

  ServiceAccountCredentials getCloudResourceManagerAdminCredentials() throws IOException;

  ServiceAccountCredentials getDefaultServiceAccountCredentials() throws IOException;

  ServiceAccountCredentials getGarbageCollectionServiceAccountCredentials(
      String garbageCollectionEmail) throws IOException;

  List<String> getSumoLogicApiKeys() throws IOException;

  Map<String, String> getMetadata(String bucketName, String objectPath);

  Blob getBlob(String bucketName, String objectPath);

  JSONObject readBlobAsJson(Blob blob);

  void deleteBlob(BlobId blobId);

  String getMoodleApiKey();
}
