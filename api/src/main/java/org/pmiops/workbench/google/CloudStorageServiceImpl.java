package org.pmiops.workbench.google;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import javax.inject.Provider;
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
    String bucketName = configProvider.get().googleCloudStorageService.credentialsBucketName;
    return readToString(bucketName, "invitation-key.txt").trim();
  }

  String readToString(String bucketName, String objectPath) {
    Storage storage = StorageOptions.getDefaultInstance().getService();
    return new String(storage.get(bucketName, objectPath).getContent());
  }
}
