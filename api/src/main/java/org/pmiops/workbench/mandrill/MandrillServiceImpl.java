package org.pmiops.workbench.mandrill;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.json.JSONObject;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.mandrill.ApiException;
import org.pmiops.workbench.mandrill.api.MandrillApi;
import org.pmiops.workbench.mandrill.model.MandrillMessage;
import org.pmiops.workbench.mandrill.model.MandrillMessageStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import javax.inject.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class MandrillServiceImpl implements MandrillService {

  private static final Logger log = Logger.getLogger(MandrillServiceImpl.class.getName());
  private final Provider<WorkbenchConfig> configProvider;
  private final Provider<MandrillApi> mandrillApiProvider;

  @Autowired
  public MandrillServiceImpl(Provider<WorkbenchConfig> configProvider,
                             Provider<MandrillApi> mandrillApiProvider) {
    this.configProvider = configProvider;
    this.mandrillApiProvider = mandrillApiProvider;
  }

  @Override
  public MandrillMessageStatus sendEmail(MandrillMessage email) {
    MandrillApi mandrillApi = mandrillApiProvider.get();
    String apiKey = readMandrillApiKey();
    try {
      return mandrillApi.send(apiKey, email);
    } catch (ApiException e){
      log.log(Level.WARNING, "Sending email via Mandrill Failed.");
      return null;
    }
  }

  private String readMandrillApiKey() {
    String bucketName = configProvider.get().googleCloudStorageService.credentialsBucketName;
    Storage storage = StorageOptions.getDefaultInstance().getService();
    String content = new String(storage.get(bucketName, "mandrill-keys.json").getContent());
    JSONObject mandrillKeys = new JSONObject(content);
    return mandrillKeys.getString("api-key");
  }

}
