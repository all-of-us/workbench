package org.pmiops.workbench.institution;

import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(config = MapStructConfig.class)
public interface InstitutionMapper {
  @Mapping(target = "institutionId", ignore = true)
  DbInstitution modelToDb(Institution modelObject);

  // these fields will be populated by @AfterMapping populateFromAuxTables
  @Mapping(target = "tierEmailDomains", ignore = true)
  @Mapping(target = "tierEmailAddresses", ignore = true)
  @Mapping(target = "userInstructions", ignore = true)
  @Mapping(target = "tierRequirements", ignore = true)
  Institution dbToModel(DbInstitution dbObject, @Context InstitutionService institutionService);

  @AfterMapping
  default void populateFromAuxTables(
      @MappingTarget Institution target, @Context InstitutionService institutionService) {
    target.setTierEmailDomains(institutionService.getEmailDomains(target.getShortName()));
    target.setTierEmailAddresses(institutionService.getEmailAddresses(target.getShortName()));
    target.setTierRequirements(institutionService.getTierRequirements(target.getShortName()));
    institutionService
        .getInstitutionUserInstructions(target.getShortName())
        .ifPresent(target::setUserInstructions);
  }
}
