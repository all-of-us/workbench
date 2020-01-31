package org.pmiops.workbench.api;

import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.model.ConfigResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConfigController implements ConfigApiDelegate {

  private final Provider<WorkbenchConfig> configProvider;

  @Autowired
  ConfigController(Provider<WorkbenchConfig> configProvider) {
    this.configProvider = configProvider;
  }

  @Override
  public ResponseEntity<ConfigResponse> getConfig() {
    WorkbenchConfig config = configProvider.get();
    return ResponseEntity.ok(
        new ConfigResponse()
            .gsuiteDomain(config.googleDirectoryService.gSuiteDomain)
            .projectId(config.server.projectId)
            .publicApiKeyForErrorReports(config.server.publicApiKeyForErrorReports)
            .enableComplianceTraining(config.access.enableComplianceTraining)
            .enableDataUseAgreement(config.access.enableDataUseAgreement)
            .enableEraCommons(config.access.enableEraCommons)
            .defaultFreeCreditsDollarLimit(config.billing.defaultFreeCreditsDollarLimit)
            .enableBillingLockout(config.featureFlags.enableBillingLockout)
            .firecloudURL(config.firecloud.baseUrl)
            .unsafeAllowSelfBypass(config.access.unsafeAllowSelfBypass)
            .enableNewAccountCreation(config.featureFlags.enableNewAccountCreation));
  }
}
