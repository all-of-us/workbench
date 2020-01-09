package org.pmiops.workbench.google;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.CopyWriter;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.CopyRequest;
import com.google.cloud.storage.StorageOptions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.json.JSONObject;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CloudStorageServiceImpl implements CloudStorageService {

  final Provider<WorkbenchConfig> configProvider;

  @Autowired
  public CloudStorageServiceImpl(Provider<WorkbenchConfig> configProvider) {
    this.configProvider = configProvider;
  }

  @Override
  public String readInvitationKey() {
    return readCredentialsBucketString("invitation-key.txt");
  }

  @Override
  public String readMandrillApiKey() {
    JSONObject mandrillKeys = getCredentialsBucketJSON("mandrill-keys.json");
    return mandrillKeys.getString("api-key");
  }

  @Override
  public String getMoodleApiKey() {
    return readCredentialsBucketString("moodle-key.txt");
  }

  @Override
  public String getImageUrl(String image_name) {
    return "http://storage.googleapis.com/" + getImagesBucketName() + "/" + image_name;
  }

  @Override
  public List<Blob> getBlobList(String bucketName) {
    Storage storage = StorageOptions.getDefaultInstance().getService();
    Iterable<Blob> blobList = storage.get(bucketName).list().getValues();
    return ImmutableList.copyOf(blobList);
  }

  @Override
  public List<Blob> getBlobListForPrefix(String bucketName, String directory) {
    Storage storage = StorageOptions.getDefaultInstance().getService();
    Iterable<Blob> blobList =
        storage.get(bucketName).list(Storage.BlobListOption.prefix(directory)).getValues();
    return ImmutableList.copyOf(blobList);
  }

  private String getCredentialsBucketName() {
    return configProvider.get().googleCloudStorageService.credentialsBucketName;
  }

  String getImagesBucketName() {
    return configProvider.get().googleCloudStorageService.emailImagesBucketName;
  }

  // wrapper for storage.get() which throws NotFoundException instead of NullPointerException
  @Override
  public Blob getBlob(String bucketName, String objectPath) {
    Storage storage = StorageOptions.getDefaultInstance().getService();
    Blob result = storage.get(bucketName, objectPath);
    if (result == null) {
      throw new NotFoundException(String.format("Bucket %s, Object %s", bucketName, objectPath));
    }
    return result;
  }

  @Override
  public void copyBlob(BlobId from, BlobId to) {
    Storage storage = StorageOptions.getDefaultInstance().getService();
    // Clears user-defined metadata, e.g. locking information on notebooks.
    BlobInfo toInfo = BlobInfo.newBuilder(to).build();
    CopyWriter w = storage.copy(CopyRequest.newBuilder().setSource(from).setTarget(toInfo).build());
    while (!w.isDone()) {
      w.copyChunk();
    }
  }

  @Override
  public void writeFile(String bucketName, String fileName, byte[] bytes) {
    Storage storage = StorageOptions.getDefaultInstance().getService();
    BlobId blobId = BlobId.of(bucketName, fileName);
    BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("text/plain").build();
    storage.create(blobInfo, bytes);
  }

  private JSONObject getCredentialsBucketJSON(final String objectPath) {
    return new JSONObject(readCredentialsBucketString(objectPath));
  }

  @Override
  public JSONObject getJiraCredentials() {
    return getCredentialsBucketJSON("jira-login.json");
  }

  @Override
  public JSONObject getElasticCredentials() {
    return getCredentialsBucketJSON("elastic-cloud.json");
  }

  private String readBlobAsString(Blob blob) {
    return new String(blob.getContent()).trim();
  }

  private String readCredentialsBucketString(String objectPath) {
    return readBlobAsString(getBlob(getCredentialsBucketName(), objectPath));
  }

  private ServiceAccountCredentials getCredentials(final String objectPath) throws IOException {
    final String json = readCredentialsBucketString(objectPath);
    return ServiceAccountCredentials.fromStream(new ByteArrayInputStream(json.getBytes()));
  }

  @Override
  public ServiceAccountCredentials getGSuiteAdminCredentials() throws IOException {
    return getCredentials("gsuite-admin-sa.json");
  }

  @Override
  public ServiceAccountCredentials getFireCloudAdminCredentials() throws IOException {
    return getCredentials("firecloud-admin-sa.json");
  }

  @Override
  public ServiceAccountCredentials getCloudResourceManagerAdminCredentials() throws IOException {
    return getCredentials("cloud-resource-manager-admin-sa.json");
  }

  @Override
  public ServiceAccountCredentials getDefaultServiceAccountCredentials() throws IOException {
    return getCredentials("app-engine-default-sa.json");
  }

  @Override
  public ServiceAccountCredentials getGarbageCollectionServiceAccountCredentials(
      String garbageCollectionEmail) throws IOException {
    final String objectPath = String.format("garbage-collection/%s.json", garbageCollectionEmail);
    return getCredentials(objectPath);
  }

  @Override
  public JSONObject readBlobAsJson(Blob blob) {
    return new JSONObject(readBlobAsString(blob));
  }

  @Override
  public Map<String, String> getMetadata(String bucketName, String objectPath) {
    return getBlob(bucketName, objectPath).getMetadata();
  }

  @Override
  public void deleteBlob(BlobId blobId) {
    Storage storage = StorageOptions.getDefaultInstance().getService();
    storage.delete(blobId);
  }

  @Override
  public Set<BlobId> getExistingBlobIdsIn(List<BlobId> ids) {
    if (ids.isEmpty()) {
      return ImmutableSet.of();
    }
    return StorageOptions.getDefaultInstance().getService().get(ids).stream()
        .filter(Objects::nonNull)
        // Clear the "generation" of the blob ID for better symmetry to the input.
        .map(b -> BlobId.of(b.getBucket(), b.getName()))
        .collect(Collectors.toSet());
  }
}
