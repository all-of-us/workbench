package org.pmiops.workbench.api;

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
            .gsuiteDomain(config.googleDirectoryService.gSuiteDomain)
            .projectId(config.server.projectId)
            .firecloudURL(config.firecloud.baseUrl)
            .publicApiKeyForErrorReports(config.server.publicApiKeyForErrorReports)
            .shibbolethUiBaseUrl(config.firecloud.shibbolethUiBaseUrl)
            .defaultFreeCreditsDollarLimit(config.billing.defaultFreeCreditsDollarLimit)
            .enableComplianceTraining(config.access.enableComplianceTraining)
            .enableEraCommons(config.access.enableEraCommons)
            .unsafeAllowSelfBypass(config.access.unsafeAllowSelfBypass)
            .enableBillingUpgrade(config.featureFlags.enableBillingUpgrade)
            .enableEventDateModifier(config.featureFlags.enableEventDateModifier)
            .enableResearchReviewPrompt(config.featureFlags.enableResearchPurposePrompt)
            .enableRasLoginGovLinking(config.access.enableRasLoginGovLinking)
            .enableGenomicExtraction(config.featureFlags.enableGenomicExtraction)
            .enableAccessModuleRewrite(config.featureFlags.enableAccessModuleRewrite)
            .enableStandardSourceDomains(config.featureFlags.enableStandardSourceDomains)
            .rasHost(config.ras.host)
            .rasClientId(config.ras.clientId)
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
