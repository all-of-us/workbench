package org.pmiops.workbench.institution;

import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
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

  /**
   * After the primary @Mappings have been executed for modelToDbWithoutUser(), use the
   * InstitutionService to retrieve a DbInstitution object to attach to the
   * DbVerifiedInstitutionalAffiliation object as its Institution field. If there is no DB object
   * matching the InstitutionShortName, the target does not receive a DbInstitution association.
   *
   * @param target the DbVerifiedInstitutionalAffiliation we are mapping to
   * @param modelObject the VerifiedInstitutionalAffiliation we are mapping from
   * @param institutionService the InstitutionService we are using to enable the mapping from an
   *     institutionShortName to a DbInstitution object which is present in the DB
   */
  @AfterMapping
  default void setDbInstitution(
      @MappingTarget DbVerifiedInstitutionalAffiliation target,
      VerifiedInstitutionalAffiliation modelObject,
      @Context InstitutionService institutionService) {

    target.setInstitution(
        institutionService.getDbInstitutionOrThrow(modelObject.getInstitutionShortName()));
  }

  @Mapping(target = "institutionShortName", source = "institution", qualifiedByName = "shortName")
  @Mapping(
      target = "institutionDisplayName",
      source = "institution",
      qualifiedByName = "displayName")
  VerifiedInstitutionalAffiliation dbToModel(DbVerifiedInstitutionalAffiliation dbObject);

  @Named("shortName")
  default String toShortName(DbInstitution institution) {
    return institution.getShortName();
  }

  @Named("displayName")
  default String toDisplayName(DbInstitution institution) {
    return institution.getDisplayName();
  }
}
