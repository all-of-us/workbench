package org.pmiops.workbench.opsgenie;

import com.ifountain.opsgenie.client.swagger.ApiException;
import com.ifountain.opsgenie.client.swagger.api.AlertApi;
import com.ifountain.opsgenie.client.swagger.model.CreateAlertRequest;
import com.ifountain.opsgenie.client.swagger.model.SuccessResponse;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OpsGenieServiceImpl implements OpsGenieService {
  private static final Logger logger = Logger.getLogger(OpsGenieServiceImpl.class.getName());

  private Provider<AlertApi> alertApiProvider;

  @Autowired
  public OpsGenieServiceImpl(Provider<AlertApi> alertApiProvider) {
    this.alertApiProvider = alertApiProvider;
  }

  @Override
  public SuccessResponse createAlert(CreateAlertRequest createAlertRequest) throws ApiException {
    return this.alertApiProvider.get().createAlert(createAlertRequest);
  }
}
