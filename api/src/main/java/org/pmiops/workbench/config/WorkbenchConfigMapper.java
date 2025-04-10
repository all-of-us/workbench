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
  @Mapping(
      target = "defaultFreeCreditsDollarLimit",
      source = "config.billing.defaultInitialCreditsDollarLimit")
  @Mapping(
      target = "defaultInitialCreditsDollarLimit",
      source = "config.billing.defaultInitialCreditsDollarLimit")
  @Mapping(target = "enableComplianceTraining", source = "config.access.enableComplianceTraining")
  @Mapping(target = "absorbSamlIdentityProviderId", source = "config.absorb.samlIdentityProviderId")
  @Mapping(target = "absorbSamlServiceProviderId", source = "config.absorb.samlServiceProviderId")
  @Mapping(
      target = "complianceTrainingRenewalLookback",
      source = "config.access.renewal.trainingLookbackPeriod")
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
  @Mapping(target = "initialCreditsBillingAccountId", source = "config.billing.accountId")
  @Mapping(target = "currentDuccVersions", source = "config.access.currentDuccVersions")
  @Mapping(target = "enableCaptcha", source = "config.captcha.enableCaptcha")
  @Mapping(target = "enableDataExplorer", source = "config.featureFlags.enableDataExplorer")
  @Mapping(target = "enableGKEAppPausing", source = "config.featureFlags.enableGKEAppPausing")
  @Mapping(
      target = "enableGKEAppMachineTypeChoice",
      source = "config.featureFlags.enableGKEAppMachineTypeChoice")
  @Mapping(target = "enableLoginIssueBanner", source = "config.banner.enableLoginIssueBanner")
  @Mapping(
      target = "enableInitialCreditsExpiration",
      source = "config.featureFlags.enableInitialCreditsExpiration")
  @Mapping(
      target = "initialCreditsValidityPeriodDays",
      source = "config.billing.initialCreditsValidityPeriodDays")
  @Mapping(
      target = "initialCreditsExtensionPeriodDays",
      source = "config.billing.initialCreditsExtensionPeriodDays")
  @Mapping(
      target = "initialCreditsExpirationWarningDays",
      source = "config.billing.initialCreditsExpirationWarningDays")
  @Mapping(
      target = "blockComplianceTraining",
      source = "config.featureFlags.blockComplianceTraining")
  @Mapping(target = "isDownForMaintenance", source = "config.server.isDownForMaintenance")
  @Mapping(target = "gceVmZones", source = "config.firecloud.gceVmZones")
  @Mapping(target = "defaultGceVmZone", source = "config.firecloud.defaultGceVmZone")
  @Mapping(
      target = "enableVWBWorkspaceCreation",
      source = "config.featureFlags.enableVWBWorkspaceCreation")
  ConfigResponse toModel(WorkbenchConfig config, List<DbAccessModule> accessModules);
}
