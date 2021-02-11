package org.pmiops.workbench.profile;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserTermsOfService;
import org.pmiops.workbench.model.AdminTableUser;
import org.pmiops.workbench.model.Profile;
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
  @Mapping(target = "contactEmailFailure", ignore = true) // I don't think we actually use this
  @Mapping(source = "latestTermsOfService.tosVersion", target = "latestTermsOfServiceVersion")
  @Mapping(source = "latestTermsOfService.agreementTime", target = "latestTermsOfServiceTime")
  @Mapping(source = "dbUser.userId", target = "userId")
  Profile toModel(
      DbUser dbUser,
      VerifiedInstitutionalAffiliation verifiedInstitutionalAffiliation,
      DbUserTermsOfService latestTermsOfService,
      Double freeTierUsage,
      Double freeTierDollarQuota);

  AdminTableUser adminViewToModel(UserDao.DbAdminTableUser dbUser);

  @Mapping(target = "authoritiesEnum", ignore = true) // derived property
  @Mapping(target = "billingProjectRetries", ignore = true) // I don't think we actually use this
  @Mapping(
      target = "complianceTrainingExpirationTime",
      ignore = true) // handled by UserService.syncComplianceTraining[V1|V2]
  @Mapping(target = "creationTime", ignore = true) // handled by ProfileController.createProfile
  @Mapping(target = "dataAccessLevelEnum", ignore = true) // derived property
  @Mapping(target = "degreesEnum", ignore = true) // derived property
  @Mapping(target = "emailVerificationStatusEnum", ignore = true) // derived property
  @Mapping(target = "freeTierCreditsLimitDaysOverride", ignore = true) // unused
  @Mapping(
      target = "freeTierCreditsLimitDollarsOverride",
      ignore = true) // handled by FreeTierBillingService.getUserFreeTierDollarLimit
  @Mapping(target = "idVerificationIsValid", ignore = true) // I don't think we actually use this
  @Mapping(target = "lastFreeTierCreditsTimeCheck", ignore = true) // used only by cron
  @Mapping(target = "lastModifiedTime", ignore = true) // handled by ProfileController.updateProfile
  @Mapping(
      target = "moodleId",
      ignore = true) // handled by UserService.syncComplianceTraining[V1|V2]
  @Mapping(target = "version", ignore = true)
  DbUser profileToDbUser(Profile profile);
}
