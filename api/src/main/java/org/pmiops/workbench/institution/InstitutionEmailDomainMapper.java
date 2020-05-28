package org.pmiops.workbench.institution;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionEmailDomain;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(config = MapStructConfig.class)
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

  default List<String> dbToModel(final Set<DbInstitutionEmailDomain> dbDomains) {
    return Optional.ofNullable(dbDomains).orElse(Collections.emptySet()).stream()
        .map(DbInstitutionEmailDomain::getEmailDomain)
        .sorted()
        .collect(Collectors.toList());
  }
}
