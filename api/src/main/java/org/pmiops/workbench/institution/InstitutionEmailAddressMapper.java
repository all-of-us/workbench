package org.pmiops.workbench.institution;

import org.mapstruct.Mapper;
import org.pmiops.workbench.db.model.DbInstitutionEmailAddress;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(config = MapStructConfig.class)
public interface InstitutionEmailAddressMapper {
  default DbInstitutionEmailAddress modelToDb(String address, long dbInstitutionId) {
    return new DbInstitutionEmailAddress()
        .setEmailAddress(address)
        .setInstitutionId(dbInstitutionId);
  }

  default String dbToModel(DbInstitutionEmailAddress dbObject) {
    return dbObject.getEmailAddress();
  }
}
