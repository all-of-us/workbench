package org.pmiops.workbench.api;

import java.math.BigDecimal;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.leonardo.model.LeonardoRuntimeConfig.CloudServiceEnum;
import org.pmiops.workbench.model.ConfigResponse;
import org.pmiops.workbench.model.RuntimeImage;
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
            .accessRenewalLookback(BigDecimal.valueOf(config.accessRenewal.lookbackPeriod))
            .gsuiteDomain(config.googleDirectoryService.gSuiteDomain)
            .projectId(config.server.projectId)
            .firecloudURL(config.firecloud.baseUrl)
            .publicApiKeyForErrorReports(config.server.publicApiKeyForErrorReports)
            .shibbolethUiBaseUrl(config.firecloud.shibbolethUiBaseUrl)
            .defaultFreeCreditsDollarLimit(config.billing.defaultFreeCreditsDollarLimit)
            .enableComplianceTraining(config.access.enableComplianceTraining)
            .complianceTrainingHost(config.moodle.host)
            .enableEraCommons(config.access.enableEraCommons)
            .unsafeAllowSelfBypass(config.access.unsafeAllowSelfBypass)
            .enableBillingUpgrade(config.featureFlags.enableBillingUpgrade)
            .enableEventDateModifier(config.featureFlags.enableEventDateModifier)
            .enableResearchReviewPrompt(config.featureFlags.enableResearchPurposePrompt)
            .enableRasLoginGovLinking(config.access.enableRasLoginGovLinking)
            .enforceRasLoginGovLinking(config.access.enforceRasLoginGovLinking)
            .enableGenomicExtraction(config.featureFlags.enableGenomicExtraction)
            .enableEgressAlertingV2(config.featureFlags.enableEgressAlertingV2)
            .enableStandardSourceDomains(config.featureFlags.enableStandardSourceDomains)
            .enableGpu(config.featureFlags.enableGpu)
            .enablePersistentDisk(config.featureFlags.enablePersistentDisk)
            .rasHost(config.ras.host)
            .rasClientId(config.ras.clientId)
            .rasLogoutUrl(config.ras.logoutUrl)
            .freeTierBillingAccountId(config.billing.accountId)
            .runtimeImages(
                Stream.concat(
                        config.firecloud.runtimeImages.dataproc.stream()
                            .map(
                                imageName ->
                                    new RuntimeImage()
                                        .cloudService(CloudServiceEnum.DATAPROC.toString())
                                        .name(imageName)),
                        config.firecloud.runtimeImages.gce.stream()
                            .map(
                                imageName ->
                                    new RuntimeImage()
                                        .cloudService(CloudServiceEnum.GCE.toString())
                                        .name(imageName)))
                    .collect(Collectors.toList())));
  }
}
