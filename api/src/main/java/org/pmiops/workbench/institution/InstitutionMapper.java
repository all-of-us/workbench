package org.pmiops.workbench.institution;

import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.model.InstitutionUserInstructions;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(config = MapStructConfig.class)
//    uses = {InstitutionEmailDomainMapper.class, InstitutionEmailDomainMapper.class})
public interface InstitutionMapper {
  @Mapping(target = "institutionId", ignore = true)
  DbInstitution modelToDb(Institution modelObject, @Context InstitutionService institutionService);

  // these fields will be populated by @AfterMapping populateFromOtherTables
  @Mapping(target = "emailDomains", ignore = true)
  @Mapping(target = "emailAddresses", ignore = true)
  @Mapping(target = "userInstructions", ignore = true)
  Institution dbToModel(DbInstitution dbObject, @Context InstitutionService institutionService);

  @AfterMapping
  default void populateOtherTables(
      Institution institution, @Context InstitutionService institutionService) {
    institutionService.setInstitutionEmailDomains(institution);
    institutionService.setInstitutionEmailAddresses(institution);

    institutionService.setInstitutionUserInstructions(
        new InstitutionUserInstructions()
            .institutionShortName(institution.getShortName())
            .instructions(institution.getUserInstructions()));
  }

  @AfterMapping
  default void populateModelFromOtherTables(
      @MappingTarget Institution target, @Context InstitutionService institutionService) {
    target.setEmailDomains(institutionService.getInstitutionEmailDomains(target.getShortName()));
    target.setEmailAddresses(
        institutionService.getInstitutionEmailAddresses(target.getShortName()));
    institutionService
        .getInstitutionUserInstructions(target.getShortName())
        .ifPresent(target::setUserInstructions);
  }
}
