package org.pmiops.workbench.institution;

import org.mapstruct.Mapper;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.model.PublicInstitutionDetails;

@Mapper(componentModel = "spring")
public interface PublicInstitutionDetailsMapper {
  PublicInstitutionDetails dbToModel(DbInstitution dbObject);
}
