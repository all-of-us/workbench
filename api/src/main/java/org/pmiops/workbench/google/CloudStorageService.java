package org.pmiops.workbench.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.json.JSONObject;
import org.pmiops.workbench.model.Cohort;

/**
 * Encapsulate Google APIs for interfacing with Google Cloud Storage.
 */
public interface CloudStorageService {

  String readInvitationKey();

  String readMandrillApiKey();

  String getImageUrl(String image_name);

  void copyAllDemoNotebooks(String workspaceBucket);

  List<Cohort> readAllDemoCohorts();

  List<JSONObject> readAllDemoConceptSets();

  List<Blob> getBlobList(String bucketName, String directory);

  Set<BlobId> blobsExist(List<BlobId> id);

  void writeFile(String bucketName, String fileName, byte[] bytes);

  void copyBlob(BlobId from, BlobId to);

  JSONObject getJiraCredentials();

  JSONObject getElasticCredentials();

  GoogleCredential getGSuiteAdminCredentials() throws IOException;

  GoogleCredential getFireCloudAdminCredentials() throws IOException;

  GoogleCredential getDefaultServiceAccountCredentials() throws IOException;

  void deleteBlob(BlobId blobId);

  String getMoodleApiKey();

}
