package org.pmiops.workbench.institution;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueMappingStrategy;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionEmailDomain;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(config = MapStructConfig.class, nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
public interface InstitutionEmailDomainMapper {
  default Set<DbInstitutionEmailDomain> modelToDb(
      final Institution modelInstitution, final DbInstitution dbInstitution) {
    return Optional.ofNullable(modelInstitution.getEmailDomains()).orElse(Collections.emptyList())
        .stream()
        .distinct()
        .map(
            domain ->
                new DbInstitutionEmailDomain().setEmailDomain(domain).setInstitution(dbInstitution))
        .collect(Collectors.toSet());
  }

  SortedSet<String> dbDomainsToStrings(final Set<DbInstitutionEmailDomain> dbDomains);

  default String dbDomainToString(final DbInstitutionEmailDomain dbDomain) {
    return dbDomain.getEmailDomain();
  }
}
