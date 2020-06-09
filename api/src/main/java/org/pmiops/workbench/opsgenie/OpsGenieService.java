package org.pmiops.workbench.opsgenie;

import com.ifountain.opsgenie.client.swagger.ApiException;
import com.ifountain.opsgenie.client.swagger.model.CreateAlertRequest;
import com.ifountain.opsgenie.client.swagger.model.SuccessResponse;

public interface OpsGenieService {

  // Create (or potentially update) an OpsGenie alert
  SuccessResponse createAlert(CreateAlertRequest createAlertRequest) throws ApiException;
}
