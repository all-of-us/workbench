package org.pmiops.workbench.google;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import java.util.Map;
import java.util.Properties;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.model.FileDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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
  public List<FileDetail> getBucketFileList(String bucketName) {
    List<FileDetail> fileList = new ArrayList<FileDetail>();
    Storage storage = StorageOptions.getDefaultInstance().getService();
    Iterable<Blob> blobList =storage.get(bucketName).list().getValues();
    blobList.forEach(blobItem->{
      FileDetail fileDetail = new FileDetail();
      fileDetail.setName(blobItem.getName());
      fileDetail.setPath("gs://"+bucketName+"/"+blobItem.getName());
      fileList.add(fileDetail);
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
