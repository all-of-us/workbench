package org.pmiops.workbench.institution;

import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbVerifiedUserInstitution;
import org.pmiops.workbench.model.VerifiedInstitutionalAffiliation;

@Mapper(componentModel = "spring")
public interface VerifiedUserInstitutionMapper {
  @Mapping(target = "verifiedUserInstitutionId", ignore = true)
  @Mapping(target = "institution", ignore = true) // set by setDbInstitution()
  @Mapping(target = "user", ignore = true) // set by caller
  DbVerifiedUserInstitution modelToDbWithoutUser(
      VerifiedInstitutionalAffiliation modelObject, @Context InstitutionService institutionService);

  @AfterMapping
  default void setDbInstitution(
      @MappingTarget DbVerifiedUserInstitution target,
      VerifiedInstitutionalAffiliation modelObject,
      @Context InstitutionService institutionService) {

    institutionService
        .getDbInstitution(modelObject.getInstitutionShortName())
        .ifPresent(target::setInstitution);
  }

  @Mapping(target = "institutionShortName", source = "institution")
  VerifiedInstitutionalAffiliation dbToModel(DbVerifiedUserInstitution dbObject);

  default String toShortName(DbInstitution institution) {
    return institution.getShortName();
  }
}
