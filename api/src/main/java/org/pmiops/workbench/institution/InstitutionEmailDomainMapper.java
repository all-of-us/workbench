package org.pmiops.workbench.institution;

import org.mapstruct.Mapper;
import org.pmiops.workbench.db.model.DbInstitutionEmailDomain;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(config = MapStructConfig.class)
public interface InstitutionEmailDomainMapper {
  default DbInstitutionEmailDomain modelToDb(String domain, long dbInstitutionId) {
    return new DbInstitutionEmailDomain().setEmailDomain(domain).setInstitutionId(dbInstitutionId);
  }

  default String dbToModel(DbInstitutionEmailDomain dbObject) {
    return dbObject.getEmailDomain();
  }
}
