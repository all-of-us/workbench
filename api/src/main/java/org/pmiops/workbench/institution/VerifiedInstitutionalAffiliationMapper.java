package org.pmiops.workbench.institution;

import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.model.VerifiedInstitutionalAffiliation;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(config = MapStructConfig.class)
public interface VerifiedInstitutionalAffiliationMapper {
  default DbInstitution toDbInstitution(
      String institutionShortName, @Context InstitutionService institutionService) {
    return institutionService.getDbInstitutionOrThrow(institutionShortName);
  }

  @Mapping(target = "verifiedInstitutionalAffiliationId", ignore = true)
  @Mapping(target = "institution", source = "modelObject.institutionShortName")
  @Mapping(target = "user", ignore = true) // set by caller
  DbVerifiedInstitutionalAffiliation modelToDbWithoutUser(
      VerifiedInstitutionalAffiliation modelObject, @Context InstitutionService institutionService);

  @Mapping(target = "institutionShortName", source = "institution.shortName")
  @Mapping(target = "institutionDisplayName", source = "institution.displayName")
  @Mapping(target = "institutionRequestAccessUrl", source = "institution.requestAccessUrl")
  VerifiedInstitutionalAffiliation dbToModel(DbVerifiedInstitutionalAffiliation dbObject);
}
