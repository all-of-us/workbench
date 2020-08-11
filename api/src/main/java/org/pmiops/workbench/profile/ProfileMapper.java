package org.pmiops.workbench.profile;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserTermsOfService;
import org.pmiops.workbench.institution.deprecated.InstitutionalAffiliationMapper;
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
      InstitutionalAffiliationMapper.class,
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
}
