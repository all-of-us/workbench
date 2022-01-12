package org.pmiops.workbench.config;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.mapstruct.BeforeMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.pmiops.workbench.leonardo.model.LeonardoRuntimeConfig.CloudServiceEnum;
import org.pmiops.workbench.model.ConfigResponse;
import org.pmiops.workbench.model.RuntimeImage;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(config = MapStructConfig.class)
public interface WorkbenchConfigMapper {
  default RuntimeImage dataprocToModel(String imageName) {
    return new RuntimeImage().cloudService(CloudServiceEnum.DATAPROC.toString()).name(imageName);
  }

  default RuntimeImage gceToModel(String imageName) {
    return new RuntimeImage().cloudService(CloudServiceEnum.GCE.toString()).name(imageName);
  }

  @BeforeMapping
  default void mapRuntimeImages(WorkbenchConfig source, @MappingTarget ConfigResponse target) {
    target.runtimeImages(
        Stream.concat(
                source.firecloud.runtimeImages.dataproc.stream().map(this::dataprocToModel),
                source.firecloud.runtimeImages.gce.stream().map(this::gceToModel))
            .collect(Collectors.toList()));
  }

  // handled by mapRuntimeImages()
  @Mapping(target = "runtimeImages", ignore = true)
  @Mapping(target = "accessRenewalLookback", source = "accessRenewal.lookbackPeriod")
  @Mapping(target = "gsuiteDomain", source = "googleDirectoryService.gSuiteDomain")
  @Mapping(target = "projectId", source = "server.projectId")
  @Mapping(target = "firecloudURL", source = "firecloud.baseUrl")
  @Mapping(target = "publicApiKeyForErrorReports", source = "server.publicApiKeyForErrorReports")
  @Mapping(target = "shibbolethUiBaseUrl", source = "firecloud.shibbolethUiBaseUrl")
  @Mapping(
      target = "defaultFreeCreditsDollarLimit",
      source = "billing.defaultFreeCreditsDollarLimit")
  @Mapping(target = "enableComplianceTraining", source = "access.enableComplianceTraining")
  @Mapping(target = "complianceTrainingHost", source = "moodle.host")
  @Mapping(target = "enableEraCommons", source = "access.enableEraCommons")
  @Mapping(target = "unsafeAllowSelfBypass", source = "access.unsafeAllowSelfBypass")
  @Mapping(target = "enableEventDateModifier", source = "featureFlags.enableEventDateModifier")
  @Mapping(
      target = "enableResearchReviewPrompt",
      source = "featureFlags.enableResearchPurposePrompt")
  @Mapping(target = "enableRasLoginGovLinking", source = "access.enableRasLoginGovLinking")
  @Mapping(target = "enforceRasLoginGovLinking", source = "access.enforceRasLoginGovLinking")
  @Mapping(target = "enableGenomicExtraction", source = "featureFlags.enableGenomicExtraction")
  @Mapping(target = "enableGpu", source = "featureFlags.enableGpu")
  @Mapping(target = "enablePersistentDisk", source = "featureFlags.enablePersistentDisk")
  @Mapping(target = "rasHost", source = "ras.host")
  @Mapping(target = "rasClientId", source = "ras.clientId")
  @Mapping(target = "rasLogoutUrl", source = "ras.logoutUrl")
  @Mapping(target = "freeTierBillingAccountId", source = "billing.accountId")
  ConfigResponse toModel(WorkbenchConfig config);
}
