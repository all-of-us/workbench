package org.pmiops.workbench.mandrill;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.json.JSONObject;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.exceptions.EmailException;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.mandrill.ApiException;
import org.pmiops.workbench.mandrill.api.MandrillApi;
import org.pmiops.workbench.mandrill.model.MandrillApiKeyAndMessage;
import org.pmiops.workbench.mandrill.model.MandrillMessage;
import org.pmiops.workbench.mandrill.model.MandrillMessageStatuses;
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
  private final Provider<CloudStorageService> cloudStorageServiceProvider;

  @Autowired
  public MandrillServiceImpl(Provider<WorkbenchConfig> configProvider,
                             Provider<MandrillApi> mandrillApiProvider,
                             Provider<CloudStorageService> cloudStorageServiceProvider) {
    this.configProvider = configProvider;
    this.mandrillApiProvider = mandrillApiProvider;
    this.cloudStorageServiceProvider = cloudStorageServiceProvider;
  }

  @Override
  public MandrillMessageStatuses sendEmail(MandrillMessage email) {
    MandrillApi mandrillApi = mandrillApiProvider.get();
    String apiKey = cloudStorageServiceProvider.get().readMandrillApiKey(); 
    MandrillApiKeyAndMessage keyAndMessage = new MandrillApiKeyAndMessage();
    keyAndMessage.setKey(apiKey);
    keyAndMessage.setMessage(email);
    try {
      MandrillMessageStatuses msgStatuses = mandrillApi.send(keyAndMessage);
      log.log(Level.INFO, "Message Status: " + msgStatuses.toString());
      return msgStatuses;
    } catch (ApiException e){
      log.log(Level.WARNING, "Sending email via Mandrill Failed.");
      throw new EmailException("Sending email failed.");
    }
  }
}
