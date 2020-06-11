package org.pmiops.workbench.institution.deprecated;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.db.model.DbInstitutionalAffiliation;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.model.InstitutionalAffiliation;
import org.pmiops.workbench.model.NonAcademicAffiliation;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(config = MapStructConfig.class)
@Deprecated // maps old-style Institutional Affiliations, to be deleted in RW-4362
public interface InstitutionalAffiliationMapper {
  @Mapping(target = "nonAcademicAffiliation", source = "nonAcademicAffiliationEnum")
  InstitutionalAffiliation dbInstitutionalAffiliationToInstitutionalAffiliation(
      DbInstitutionalAffiliation dbInstitutionalAffiliation);

  @Mapping(target = "institutionalAffiliationId", ignore = true)
  @Mapping(target = "nonAcademicAffiliationEnum", source = "nonAcademicAffiliation")
  @Mapping(
      target = "orderIndex",
      ignore = true) // set by ProfileController.updateInstitutionalAffiliations
  @Mapping(target = "user", ignore = true) // set by UserService.createUser
  DbInstitutionalAffiliation institutionalAffiliationToDbInstitutionalAffiliation(
      InstitutionalAffiliation institutionalAffiliation);

  static NonAcademicAffiliation nonAcademicAffiliationFromStorage(Short nonAcademicAffiliation) {
    return DbStorageEnums.nonAcademicAffiliationFromStorage(nonAcademicAffiliation);
  }

  static Short nonAcademicAffiliationToStorage(NonAcademicAffiliation nonAcademicAffiliation) {
    return DbStorageEnums.nonAcademicAffiliationToStorage(nonAcademicAffiliation);
  }
}
