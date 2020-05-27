package org.pmiops.workbench.institution;

import org.mapstruct.Mapper;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionEmailDomain;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(config = MapStructConfig.class)
public interface InstitutionEmailDomainMapper {
  default DbInstitutionEmailDomain modelToDb(String domain, DbInstitution dbInstitution) {
    return new DbInstitutionEmailDomain().setEmailDomain(domain).setInstitution(dbInstitution);
  }

  default String dbToModel(DbInstitutionEmailDomain dbObject) {
    return dbObject.getEmailDomain();
  }
}
