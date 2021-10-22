package org.pmiops.workbench.institution;

import java.util.List;
import java.util.Set;
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
  // InstitutionTierConfig to DbInstitutionTierRequirement
  List<DbInstitutionTierRequirement> tierConfigsToDbTierRequirements(
      List<InstitutionTierConfig> tierConfigs,
      @Context DbInstitution dbInstitution,
      @Context List<DbAccessTier> dbAccessTiers);

  @Mapping(target = "institution", ignore = true)
  @Mapping(target = "accessTier", ignore = true)
  @Mapping(target = "institutionTierRequirementId", ignore = true)
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
        .setAccessTier(
            AccessUtils.getAccessTierByShortNameOrThrow(
                dbAccessTiers, source.getAccessTierShortName()));
  }

  // InstitutionTierConfig to DbInstitutionEmailDomain
  Set<DbInstitutionEmailDomain> emailDomainsToDb(
      Set<String> emailDomains,
      @Context DbInstitution dbInstitution,
      @Context DbAccessTier dbAccessTier);

  default DbInstitutionEmailDomain emailDomainToDb(
      String emailDomain,
      @Context DbInstitution dbInstitution,
      @Context DbAccessTier dbAccessTier) {
    return new DbInstitutionEmailDomain()
        .setEmailDomain(emailDomain)
        .setInstitution(dbInstitution)
        .setAccessTier(dbAccessTier);
  }

  // InstitutionTierConfig to DbInstitutionEmailAddress
  Set<DbInstitutionEmailAddress> emailAddressesToDb(
      Set<String> emailAddresses,
      @Context DbInstitution dbInstitution,
      @Context DbAccessTier dbAccessTier);

  default DbInstitutionEmailAddress emailAddressToDb(
      String emailAddress,
      @Context DbInstitution dbInstitution,
      @Context DbAccessTier dbAccessTier) {
    return new DbInstitutionEmailAddress()
        .setEmailAddress(emailAddress)
        .setInstitution(dbInstitution)
        .setAccessTier(dbAccessTier);
  }

  // Combine DbInstitutionTierRequirement, emailAddresses, and emailDomains into
  // InstitutionTierConfig.
  @Mapping(target = "accessTierShortName", source = "source.accessTier.shortName")
  InstitutionTierConfig dbToTierConfigModel(
      DbInstitutionTierRequirement source, Set<String> emailAddresses, Set<String> emailDomains);
}
