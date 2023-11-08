package org.pmiops.workbench.google;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.CopyWriter;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.CopyRequest;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.json.JSONObject;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.FileDetail;
import org.pmiops.workbench.notebooks.NotebookLockingUtils;

public class CloudStorageClientImpl implements CloudStorageClient {

  final Provider<WorkbenchConfig> configProvider;
  final Provider<Storage> storageProvider;

  public CloudStorageClientImpl(
      Provider<Storage> storageProvider, Provider<WorkbenchConfig> configProvider) {
    this.configProvider = configProvider;
    this.storageProvider = storageProvider;
  }

  @Override
  public String readMandrillApiKey() {
    JSONObject mandrillKeys = getCredentialsBucketJSON("mandrill-keys.json");
    return mandrillKeys.getString("api-key");
  }

  @Override
  public JSONObject getAbsorbCredentials() {
    // Our Absorb credentials are for an "Admin" user, as outlined here:
    // https://support.absorblms.com/hc/en-us/articles/360053227673-Admin-Roles-Permissions
    // However, since the user is not a "System Admin" we cannot view "portal settings" such as SSO
    // configuration.
    return getCredentialsBucketJSON("absorb-credentials.json");
  }

  @Override
  public String getMoodleApiKey() {
    return getCredentialsBucketString(configProvider.get().moodle.credentialsKeyV2);
  }

  @Override
  public String getImageUrl(String imageName) {
    return "http://storage.googleapis.com/" + getImagesBucketName() + "/" + imageName;
  }

  @Override
  public List<Blob> getBlobPage(String bucketName) {
    return storageProvider.get().get(bucketName).list().streamValues().collect(Collectors.toList());
  }

  @Override
  public List<Blob> getBlobPageForPrefix(String bucketName, String directory) {
    return storageProvider
        .get()
        .get(bucketName)
        .list(Storage.BlobListOption.prefix(directory))
        .streamValues()
        .collect(Collectors.toList());
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
    Blob result = storageProvider.get().get(bucketName, objectPath);
    if (result == null) {
      throw new NotFoundException(String.format("Bucket %s, Object %s", bucketName, objectPath));
    }
    return result;
  }

  @Override
  public void copyBlob(BlobId from, BlobId to) {
    // Clears user-defined metadata, e.g. locking information on notebooks.
    BlobInfo toInfo = BlobInfo.newBuilder(to).build();
    CopyWriter w =
        storageProvider
            .get()
            .copy(CopyRequest.newBuilder().setSource(from).setTarget(toInfo).build());
    while (!w.isDone()) {
      w.copyChunk();
    }
  }

  @Override
  public Blob writeFile(String bucketName, String fileName, byte[] bytes) {
    BlobId blobId = BlobId.of(bucketName, fileName);
    BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("text/plain").build();
    return storageProvider.get().create(blobInfo, bytes);
  }

  @Override
  public String getCredentialsBucketString(String objectPath) {
    return readBlobAsString(getBlob(getCredentialsBucketName(), objectPath));
  }

  private JSONObject getCredentialsBucketJSON(final String objectPath) {
    return new JSONObject(getCredentialsBucketString(objectPath));
  }

  private String readBlobAsString(Blob blob) {
    return new String(blob.getContent()).trim();
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
    storageProvider.get().delete(blobId);
  }

  @Override
  public Set<BlobId> getExistingBlobIdsIn(List<BlobId> ids) {
    if (ids.isEmpty()) {
      return ImmutableSet.of();
    }
    return storageProvider.get().get(ids).stream()
        .filter(Objects::nonNull)
        // Clear the "generation" of the blob ID for better symmetry to the input.
        .map(b -> BlobId.of(b.getBucket(), b.getName()))
        .collect(Collectors.toSet());
  }

  @Override
  public String getCaptchaServerKey() {
    return getCredentialsBucketString("captcha-server-key.txt");
  }

  private FileDetail blobToFileDetail(Blob blob, String bucketName) {
    String[] parts = blob.getName().split("/");
    FileDetail fileDetail = new FileDetail();
    fileDetail.setName(parts[parts.length - 1]);
    fileDetail.setPath("gs://" + bucketName + "/" + blob.getName());
    fileDetail.setLastModifiedTime(blob.getUpdateTime());
    fileDetail.setSizeInBytes(blob.getSize());

    return fileDetail;
  }

  @Override
  public FileDetail blobToFileDetail(Blob blob, String bucketName, Set<String> workspaceUsers) {
    FileDetail fileDetail = blobToFileDetail(blob, bucketName);
    Map<String, String> fileMetadata = blob.getMetadata();
    if (null != fileMetadata) {
      String hash = fileMetadata.getOrDefault("lastLockedBy", null);
      if (hash != null) {
        String userName = NotebookLockingUtils.findHashedUser(bucketName, workspaceUsers, hash);
        fileDetail.setLastModifiedBy(userName);
      }
    }

    return fileDetail;
  }

  @Override
  public String getGoogleOAuthClientSecret() {
    return getCredentialsBucketString("google-oauth-client-secret.txt");
  }

  @Override
  public String getNotebookLastModifiedBy(String notebookUri, Set<String> workspaceUsers) {
    String notebookDetails = notebookUri.replaceFirst("gs://", "");
    String[] notebookPath = notebookDetails.split("/");
    final String name =
        Joiner.on('/').join(Arrays.copyOfRange(notebookPath, 1, notebookPath.length));
    String bucketName = notebookPath[0];
    Blob blob = getBlob(bucketName, name);
    FileDetail fileDetail = blobToFileDetail(blob, bucketName, workspaceUsers);
    return fileDetail.getLastModifiedBy();
  }
}
