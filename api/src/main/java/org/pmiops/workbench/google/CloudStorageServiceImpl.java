package org.pmiops.workbench.google;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.CopyWriter;
import com.google.cloud.storage.Storage.CopyRequest;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Provider;

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
    Page<Blob> allBlobs = demoBucket.list();
    List<Blob> notebookFiles = new ArrayList<>();
    notebookFiles.addAll(getFileTypeFromPage(allBlobs, ".ipynb"));
    while (allBlobs.hasNextPage()) {
      allBlobs = allBlobs.getNextPage();
      notebookFiles.addAll(getFileTypeFromPage(allBlobs, ".ipynb"));
    }
    notebookFiles.forEach((Blob blob) -> {
      BlobId demoNotebookId = blob.getBlobId();
      BlobId targetLocation = BlobId.of(workspaceBucket, "notebooks/" + demoNotebookId.getName());
      copyBlob(demoNotebookId, targetLocation);
    });
  }

  private List<Blob> getFileTypeFromPage(Page<Blob> page, String extension) {
    List<Blob> fileList = new ArrayList<>();
    Iterable<Blob> iter = page.iterateAll();
    iter.forEach((Blob blob) -> {
      if (blob.getBlobId().getName().endsWith(extension)) {
        fileList.add(blob);
      }
    });
    return fileList;
  }

  private List<JSONObject> getJsonAndFilterByTypeFromPage(Page<Blob> page, String type) {
    List<JSONObject> fileList = new ArrayList<>();
    Iterable<Blob> iter = page.iterateAll();
    iter.forEach((Blob blob) -> {
      if (blob.getBlobId().getName().endsWith(".json")) {
        JSONObject unknownObject = new JSONObject(new String(blob.getContent()).trim());
        String objectType = unknownObject.getString("type");
        if (objectType.equalsIgnoreCase("cohort")) {
          fileList.add(unknownObject.getJSONObject(objectType));
        }
      }
    });
    return fileList;
  }

  public List<JSONObject> readAllDemoCohorts() {
    Storage storage = StorageOptions.getDefaultInstance().getService();
    Bucket demoBucket = storage.get(getDemosBucketName());
    Page<Blob> allBlobs = demoBucket.list();
    List<JSONObject> cohortFiles = new ArrayList<>();
    cohortFiles.addAll(getJsonAndFilterByTypeFromPage(allBlobs, "cohort"));
    while (allBlobs.hasNextPage()) {
      allBlobs = allBlobs.getNextPage();
      cohortFiles.addAll(getJsonAndFilterByTypeFromPage(allBlobs, "cohort"));
    }
    return cohortFiles;
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
