package org.pmiops.workbench.institution;

import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueMappingStrategy;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionTierRequirement;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.model.InstitutionTierRequirement;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(
    config = MapStructConfig.class,
    nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
public interface InstitutionTierRequirementMapper {

  default Set<DbInstitutionTierRequirement> modelToDb(
      Institution modelInstitution, @Context DbInstitution dbInstitution, @Context WorkbenchConfig workbenchConfig) {
    workbenchConfig.cdr.
    return tierRequirementesToDb(modelInstitution.getTierRequirements(), dbInstitution, workbenchConfig);
  }

  Set<DbInstitutionTierRequirement> tierRequirementesToDb(
      List<InstitutionTierRequirement> tierRequirementes, @Context DbInstitution dbInstitution);

  default DbInstitutionTierRequirement tierRequirementToDb(
      InstitutionTierRequirement tierRequirement, @Context DbInstitution dbInstitution) {
    return new DbInstitutionTierRequirement()
        .setAccessTier(tierRequirement.accessTierShortName())
        .setInstitution(dbInstitution);
  }
}
