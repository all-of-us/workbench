package org.pmiops.workbench.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.json.JSONObject;

/**
 * Encapsulate Googe APIs for interfacing with Google Cloud Storage.
 */
public interface CloudStorageService {

  public String readInvitationKey();
  public String readMandrillApiKey();
  public String getImageUrl(String image_name);
  public void copyAllDemoNotebooks(String workspaceBucket);
  public List<JSONObject> readAllDemoCohorts();
  public List<JSONObject> readAllDemoConceptSets();
  public List<Blob> getBlobList(String bucketName, String directory);
  public Set<BlobId> blobsExist(List<BlobId> id);
  public void writeFile(String bucketName, String fileName, byte[] bytes);
  public void copyBlob(BlobId from, BlobId to);
  public JSONObject getJiraCredentials();
  public GoogleCredential getGSuiteAdminCredentials() throws IOException;

  public void deleteBlob(BlobId blobId);
  public String getMoodleApiKey();
}
