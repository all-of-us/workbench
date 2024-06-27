package org.pmiops.workbench.config;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.access.AccessModuleNameMapper;
import org.pmiops.workbench.db.model.DbAccessModule;
import org.pmiops.workbench.model.AccessModuleConfig;
import org.pmiops.workbench.model.ConfigResponse;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(
    config = MapStructConfig.class,
    uses = {AccessModuleNameMapper.class})
public interface WorkbenchConfigMapper {
  AccessModuleConfig mapAccessModule(DbAccessModule accessModule);

  @Mapping(target = "accessRenewalLookback", source = "config.access.renewal.lookbackPeriod")
  @Mapping(target = "gsuiteDomain", source = "config.googleDirectoryService.gSuiteDomain")
  @Mapping(target = "projectId", source = "config.server.projectId")
  @Mapping(target = "firecloudURL", source = "config.firecloud.baseUrl")
  @Mapping(
      target = "publicApiKeyForErrorReports",
      source = "config.server.publicApiKeyForErrorReports")
  @Mapping(target = "shibbolethUiBaseUrl", source = "config.firecloud.shibbolethUiBaseUrl")
  @Mapping(
      target = "defaultFreeCreditsDollarLimit",
      source = "config.billing.defaultFreeCreditsDollarLimit")
  @Mapping(target = "enableComplianceTraining", source = "config.access.enableComplianceTraining")
  @Mapping(target = "absorbSamlIdentityProviderId", source = "config.absorb.samlIdentityProviderId")
  @Mapping(target = "absorbSamlServiceProviderId", source = "config.absorb.samlServiceProviderId")
  @Mapping(
      target = "complianceTrainingRenewalLookback",
      source = "config.access.renewal.trainingLookbackPeriod")
  @Mapping(target = "enableEraCommons", source = "config.access.enableEraCommons")
  @Mapping(target = "unsafeAllowSelfBypass", source = "config.access.unsafeAllowSelfBypass")
  @Mapping(
      target = "enableEventDateModifier",
      source = "config.featureFlags.enableEventDateModifier")
  @Mapping(target = "enableRasIdMeLinking", source = "config.access.enableRasIdMeLinking")
  @Mapping(target = "enableRasLoginGovLinking", source = "config.access.enableRasLoginGovLinking")
  @Mapping(target = "rasHost", source = "config.ras.host")
  @Mapping(target = "rasClientId", source = "config.ras.clientId")
  @Mapping(target = "rasLogoutUrl", source = "config.ras.logoutUrl")
  @Mapping(target = "tanagraBaseUrl", source = "config.tanagra.baseUrl")
  @Mapping(target = "freeTierBillingAccountId", source = "config.billing.accountId")
  @Mapping(target = "currentDuccVersions", source = "config.access.currentDuccVersions")
  @Mapping(target = "enableCaptcha", source = "config.captcha.enableCaptcha")
  @Mapping(target = "enableDataExplorer", source = "config.featureFlags.enableDataExplorer")
  @Mapping(target = "enableGKEAppPausing", source = "config.featureFlags.enableGKEAppPausing")
  @Mapping(
      target = "enableGKEAppMachineTypeChoice",
      source = "config.featureFlags.enableGKEAppMachineTypeChoice")
  ConfigResponse toModel(WorkbenchConfig config, List<DbAccessModule> accessModules);
}
