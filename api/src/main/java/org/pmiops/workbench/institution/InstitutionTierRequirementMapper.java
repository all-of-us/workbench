package org.pmiops.workbench.institution;

import java.util.List;
import java.util.Set;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValueMappingStrategy;
import org.pmiops.workbench.access.AccessTierServiceImpl;
import org.pmiops.workbench.access.AccessUtils;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionTierRequirement;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.model.InstitutionTierRequirement;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(
    config = MapStructConfig.class,
    nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
public interface InstitutionTierRequirementMapper {

  default List<DbInstitutionTierRequirement> modelToDb(
      Institution modelInstitution, @Context DbInstitution dbInstitution, @Context List<DbAccessTier> dbAccessTiers) {
    return tierRequirementsToDb(modelInstitution.getTierRequirements(), dbInstitution, dbAccessTiers);
  }

  List<DbInstitutionTierRequirement> tierRequirementsToDb(
      List<InstitutionTierRequirement> tierRequirements, @Context DbInstitution dbInstitution, @Context List<DbAccessTier> dbAccessTiers);

  @Mapping(target = "institution", ignore = true)
  @Mapping(target = "accessTier", ignore = true)
  @Mapping(target = "institutionTierRequirementId", ignore = true)
  DbInstitutionTierRequirement tierRequirementToDb(
      InstitutionTierRequirement source, @Context DbInstitution dbInstitution, @Context List<DbAccessTier> dbAccessTiers);

  @AfterMapping
  default void afterMappingModelToDb(
      InstitutionTierRequirement source,
      @MappingTarget DbInstitutionTierRequirement dbInstitutionTierRequirement,
      @Context DbInstitution dbInstitution,
      @Context List<DbAccessTier> dbAccessTiers) {
    dbInstitutionTierRequirement.
        setInstitution(dbInstitution).
        setAccessTier(AccessUtils.getAccessTierByShortName(dbAccessTiers, source.getAccessTierShortName()).orElseThrow(
            () -> new NotFoundException("Access tier " + source.getAccessTierShortName() + "not found")
        ));
  }

  default List<InstitutionTierRequirement> dbToModel(List<DbInstitutionTierRequirement> dbInstitutionTierRequirements) {
    return tierRequirementsToModel(dbInstitutionTierRequirements);
  }

  List<InstitutionTierRequirement> tierRequirementsToModel(
      List<DbInstitutionTierRequirement> dbInstitutionTierRequirements);

  @Mapping(target = "accessTierShortName", source = "accessTier.shortName")
  @Mapping(target = "eraRequired", ignore = true)
  InstitutionTierRequirement tierRequirementToModel(DbInstitutionTierRequirement source);
}
