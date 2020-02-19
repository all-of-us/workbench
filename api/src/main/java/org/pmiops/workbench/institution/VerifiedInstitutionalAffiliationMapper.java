package org.pmiops.workbench.institution;

import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.model.VerifiedInstitutionalAffiliation;

@Mapper(componentModel = "spring")
public interface VerifiedInstitutionalAffiliationMapper {
  @Mapping(target = "verifiedInstitutionalAffiliationId", ignore = true)
  @Mapping(target = "institution", ignore = true) // set by setDbInstitution()
  @Mapping(target = "user", ignore = true) // set by caller
  DbVerifiedInstitutionalAffiliation modelToDbWithoutUser(
      VerifiedInstitutionalAffiliation modelObject, @Context InstitutionService institutionService);

  @AfterMapping
  default void setDbInstitution(
      @MappingTarget DbVerifiedInstitutionalAffiliation target,
      VerifiedInstitutionalAffiliation modelObject,
      @Context InstitutionService institutionService) {

    institutionService
        .getDbInstitution(modelObject.getInstitutionShortName())
        .ifPresent(target::setInstitution);
  }

  @Mapping(target = "institutionShortName", source = "institution")
  VerifiedInstitutionalAffiliation dbToModel(DbVerifiedInstitutionalAffiliation dbObject);

  default String toShortName(DbInstitution institution) {
    return institution.getShortName();
  }
}
