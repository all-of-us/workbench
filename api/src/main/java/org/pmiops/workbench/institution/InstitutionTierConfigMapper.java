package org.pmiops.workbench.institution;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValueMappingStrategy;
import org.pmiops.workbench.access.AccessUtils;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionEmailAddress;
import org.pmiops.workbench.db.model.DbInstitutionEmailDomain;
import org.pmiops.workbench.db.model.DbInstitutionTierRequirement;
import org.pmiops.workbench.model.InstitutionTierConfig;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(
    config = MapStructConfig.class,
    nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
public interface InstitutionTierConfigMapper {
  List<DbInstitutionTierRequirement> tierConfigsToDbTierRequirements(
      List<InstitutionTierConfig> tierConfigs,
      @Context DbInstitution dbInstitution,
      @Context List<DbAccessTier> dbAccessTiers);

  @Mapping(target = "institution", ignore = true)
  @Mapping(target = "accessTier", ignore = true)
  @Mapping(target = "institutionTierRequirementId", ignore = true)
  @Mapping(target = "eraRequired", ignore = true)
  DbInstitutionTierRequirement tierConfigToDbTierRequirement(
      InstitutionTierConfig source,
      @Context DbInstitution dbInstitution,
      @Context List<DbAccessTier> dbAccessTiers);

  @AfterMapping
  default void afterMappingConfigModelToRequirementDb(
      InstitutionTierConfig source,
      @MappingTarget DbInstitutionTierRequirement dbInstitutionTierRequirement,
      @Context DbInstitution dbInstitution,
      @Context List<DbAccessTier> dbAccessTiers) {
    dbInstitutionTierRequirement
        .setInstitution(dbInstitution)
        .setEraRequired(false)
        .setAccessTier(
            AccessUtils.getAccessTierByShortNameOrThrow(
                dbAccessTiers, source.getAccessTierShortName()));
  }

  default Set<DbInstitutionEmailDomain> emailDomainsToDb(
      Set<String> emailDomains, DbInstitution institution, DbAccessTier accessTier) {
    return emailDomains.stream()
        .map(emailDomain -> emailDomainToDb(emailDomain, institution, accessTier))
        .collect(Collectors.toSet());
  }

  @Mapping(target = "institutionEmailDomainId", ignore = true)
  DbInstitutionEmailDomain emailDomainToDb(
      String emailDomain, DbInstitution institution, DbAccessTier accessTier);

  default Set<DbInstitutionEmailAddress> emailAddressesToDb(
      Set<String> emailAddresses, DbInstitution institution, DbAccessTier accessTier) {
    return emailAddresses.stream()
        .map(emailAddress -> emailAddressToDb(emailAddress, institution, accessTier))
        .collect(Collectors.toSet());
  }

  @Mapping(target = "institutionEmailAddressId", ignore = true)
  DbInstitutionEmailAddress emailAddressToDb(
      String emailAddress, DbInstitution institution, DbAccessTier accessTier);

  // Combine DbInstitutionTierRequirement, emailAddresses, and emailDomains into
  // InstitutionTierConfig.
  @Mapping(target = "accessTierShortName", source = "source.accessTier.shortName")
  InstitutionTierConfig dbToTierConfigModel(
      DbInstitutionTierRequirement source, Set<String> emailAddresses, Set<String> emailDomains);
}
