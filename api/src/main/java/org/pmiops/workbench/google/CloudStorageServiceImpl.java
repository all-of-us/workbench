package org.pmiops.workbench.google;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.CopyWriter;
import com.google.cloud.storage.Storage.CopyRequest;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.inject.Provider;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.json.JSONObject;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CloudStorageServiceImpl implements CloudStorageService {

  final Provider<WorkbenchConfig> configProvider;

  @Autowired
  public CloudStorageServiceImpl(Provider<WorkbenchConfig> configProvider) {
    this.configProvider = configProvider;
  }

  public String readInvitationKey() {
    return readToString(getCredentialsBucketName(), "invitation-key.txt");
  }

  public String readMandrillApiKey() {
    JSONObject mandrillKeys = new JSONObject(readToString(getCredentialsBucketName(), "mandrill-keys.json"));
    return mandrillKeys.getString("api-key");
  }

  public void copyAllDemoNotebooks(String workspaceBucket)  {
    Storage storage = StorageOptions.getDefaultInstance().getService();
    Bucket demoBucket = storage.get(getDemosBucketName());
    StreamSupport
        .stream(demoBucket.list().getValues().spliterator(), false)
        .filter(blob -> blob.getBlobId().getName().endsWith(".ipynb"))
        .map(blob -> ImmutablePair
            .of(blob.getBlobId(), BlobId.of(workspaceBucket, "notebooks/" + blob.getBlobId().getName())))
        .forEach(pair -> copyBlob(pair.getLeft(), pair.getRight()));
  }

  public List<JSONObject> readAllDemoCohorts() {
    Storage storage = StorageOptions.getDefaultInstance().getService();
    Bucket demoBucket = storage.get(getDemosBucketName());

    return StreamSupport
        .stream(demoBucket.list().getValues().spliterator(), false)
        .filter(blob -> blob.getBlobId().getName().endsWith(".json"))
        .map(blob -> new JSONObject(new String(blob.getContent()).trim()))
        .filter(jsonObject -> jsonObject.getString("type").equalsIgnoreCase("cohort"))
        .map(jsonObject -> jsonObject.getJSONObject("cohort"))
        .collect(Collectors.toList());
  }

  @Override
  public List<Blob> getBlobList(String bucketName, String directory) {
    Storage storage = StorageOptions.getDefaultInstance().getService();
    Iterable<Blob> blobList = storage.get(bucketName)
        .list(Storage.BlobListOption.prefix(directory)).getValues();
    return ImmutableList.copyOf(blobList);
  }

  private String getCredentialsBucketName() {
    return configProvider.get().googleCloudStorageService.credentialsBucketName;
  }

  private String getDemosBucketName() {
    return configProvider.get().googleCloudStorageService.demosBucketName;
  }

  private String readToString(String bucketName, String objectPath) {
    Storage storage = StorageOptions.getDefaultInstance().getService();
    return new String(storage.get(bucketName, objectPath).getContent()).trim();
  }

  @Override
  public void copyBlob(BlobId from, BlobId to) {
    Storage storage = StorageOptions.getDefaultInstance().getService();
    CopyWriter w = storage.copy(CopyRequest.newBuilder()
        .setSource(from)
        .setTarget(to)
        .build());
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

  @Override
  public JSONObject getJiraCredentials() {
    return new JSONObject(readToString(getCredentialsBucketName(), "jira-login.json"));
  }
}
