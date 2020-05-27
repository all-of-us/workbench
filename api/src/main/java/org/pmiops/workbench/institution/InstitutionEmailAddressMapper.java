package org.pmiops.workbench.institution;

import org.mapstruct.Mapper;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionEmailAddress;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(config = MapStructConfig.class)
public interface InstitutionEmailAddressMapper {
  default DbInstitutionEmailAddress modelToDb(String address, DbInstitution dbInstitution) {
    return new DbInstitutionEmailAddress().setEmailAddress(address).setInstitution(dbInstitution);
  }

  default String dbToModel(DbInstitutionEmailAddress dbObject) {
    return dbObject.getEmailAddress();
  }
}
