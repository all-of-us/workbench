package org.pmiops.workbench.google;

import com.google.cloud.storage.*;

import java.util.ArrayList;
import java.util.List;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.model.FileDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.pmiops.workbench.model.BlobDetail;

@Service
public class CloudStorageServiceImpl implements CloudStorageService {

  final Provider<WorkbenchConfig> configProvider;

  @Autowired
  public CloudStorageServiceImpl(Provider<WorkbenchConfig> configProvider) {
    this.configProvider = configProvider;
  }

  public String readInvitationKey() {
    return readToString(getCredentialsBucketName(), "invitation-key.txt").trim();
  }

  public String readBlockscoreApiKey() {
    return readToString(getCredentialsBucketName(), "blockscore-api-key.txt").trim();
  }

  @Override
  public List<BlobDetail> getBucketFileList(String bucketName, String directory) {
    List<BlobDetail> fileList = new ArrayList<BlobDetail>();
    Storage storage = StorageOptions.getDefaultInstance().getService();
    Iterable<Blob> blobList = storage.get(bucketName)
        .list(Storage.BlobListOption.prefix(directory)).getValues();
    blobList.forEach(blobItem -> {
      BlobDetail detail = new BlobDetail(blobItem.getName(), blobItem.getName());
      fileList.add(detail);
    });
    return fileList;
  }

  String getCredentialsBucketName() {
    return configProvider.get().googleCloudStorageService.credentialsBucketName;
  }

  String readToString(String bucketName, String objectPath) {
    Storage storage = StorageOptions.getDefaultInstance().getService();
    return new String(storage.get(bucketName, objectPath).getContent());
  }

  @Override
  public void writeFile(String bucketName, String fileName, byte[] bytes) {
    Storage storage = StorageOptions.getDefaultInstance().getService();
    BlobId blobId = BlobId.of(bucketName, fileName);
    BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("text/plain").build();
    storage.create(blobInfo, bytes);
  }
}
