package org.pmiops.workbench.profile;

import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.billing.InitialCreditsService;
import org.pmiops.workbench.db.dao.UserDao.DbAdminTableUser;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserTermsOfService;
import org.pmiops.workbench.model.AdminTableUser;
import org.pmiops.workbench.model.Profile;
import org.pmiops.workbench.model.ProfileAccessModules;
import org.pmiops.workbench.model.UserTierEligibility;
import org.pmiops.workbench.model.VerifiedInstitutionalAffiliation;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.MapStructConfig;
import org.pmiops.workbench.utils.mappers.UserMapper;

@Mapper(
    config = MapStructConfig.class,
    uses = {
      AddressMapper.class,
      CommonMappers.class,
      DbStorageEnums.class,
      DemographicSurveyMapper.class,
      PageVisitMapper.class,
      UserMapper.class
    })
public interface ProfileMapper {
  @Mapping(source = "latestTermsOfService.tosVersion", target = "latestTermsOfServiceVersion")
  @Mapping(source = "latestTermsOfService.aouAgreementTime", target = "latestTermsOfServiceTime")
  @Mapping(source = "dbUser.userId", target = "userId")
  @Mapping(source = "dbUser.duccAgreement.signedVersion", target = "duccSignedVersion")
  @Mapping(source = "dbUser.duccAgreement.userInitials", target = "duccSignedInitials")
  @Mapping(source = "dbUser.duccAgreement.completionTime", target = "duccCompletionTimeEpochMillis")
  @Mapping(source = "dbUser.demographicSurveyV2", target = "demographicSurveyV2")
  @Mapping(
      target = "initialCreditsExpirationEpochMillis",
      source = "dbUser",
      qualifiedByName = "getInitialCreditsExpiration")
  @Mapping(
      source = "dbUser.userInitialCreditsExpiration.bypassed",
      target = "initialCreditsExpirationBypassed",
      defaultValue = "false")
  Profile toModel(
      DbUser dbUser,
      @Context InitialCreditsService initialCreditsService,
      VerifiedInstitutionalAffiliation verifiedInstitutionalAffiliation,
      DbUserTermsOfService latestTermsOfService,
      Double freeTierUsage,
      Double freeTierDollarQuota,
      List<String> accessTierShortNames,
      List<UserTierEligibility> tierEligibilities,
      ProfileAccessModules accessModules,
      boolean newUserSatisfactionSurveyEligibility,
      Instant newUserSatisfactionSurveyEligibilityEndTime);

  List<AdminTableUser> adminViewToModel(List<DbAdminTableUser> adminTableUsers);

  // used by the generated impl of adminViewToModel()
  default List<String> splitAccessTierShortNames(
      @Nullable String commaSeparatedAccessTierShortNames) {
    if (StringUtils.isEmpty(commaSeparatedAccessTierShortNames)) {
      return Collections.emptyList();
    } else {
      return Arrays.stream(commaSeparatedAccessTierShortNames.split(","))
          .collect(Collectors.toList());
    }
  }
}
