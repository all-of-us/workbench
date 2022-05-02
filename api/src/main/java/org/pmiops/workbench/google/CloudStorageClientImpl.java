package org.pmiops.workbench.google;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.CopyWriter;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.CopyRequest;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.json.JSONObject;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.exceptions.NotFoundException;

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
  public String getMoodleApiKey() {
    return getCredentialsBucketString(configProvider.get().moodle.credentialsKeyV2);
  }

  @Override
  public String getImageUrl(String imageName) {
    return "http://storage.googleapis.com/" + getImagesBucketName() + "/" + imageName;
  }

  @Override
  public List<Blob> getBlobPage(String bucketName) {
    Iterable<Blob> blobList = storageProvider.get().get(bucketName).list().getValues();
    return ImmutableList.copyOf(blobList);
  }

  @Override
  public List<Blob> getBlobPageForPrefix(String bucketName, String directory) {
    Iterable<Blob> blobList =
        storageProvider
            .get()
            .get(bucketName)
            .list(Storage.BlobListOption.prefix(directory))
            .getValues();
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
}
