package org.pmiops.workbench.institution;

import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueMappingStrategy;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionEmailDomain;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(
    config = MapStructConfig.class,
    nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
public interface InstitutionEmailDomainMapper {

  default Set<DbInstitutionEmailDomain> modelToDb(
      Institution modelInstitution, @Context DbInstitution dbInstitution, @Context Acc) {
    return emailDomainsToDb(modelInstitution.getTierEmailDomains(), dbInstitution);
  }

  Set<DbInstitutionEmailDomain> emailDomainsToDb(
      List<String> emailDomains, @Context DbInstitution dbInstitution);

  default DbInstitutionEmailDomain emailDomainToDb(
      String emailDomain, @Context DbInstitution dbInstitution) {
    return new DbInstitutionEmailDomain().setEmailDomain(emailDomain).setInstitution(dbInstitution);
  }

  SortedSet<String> dbDomainsToStrings(final Set<DbInstitutionEmailDomain> dbDomains);

  default String dbDomainToString(final DbInstitutionEmailDomain dbDomain) {
    return dbDomain.getEmailDomain();
  }
}
