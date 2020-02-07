package org.pmiops.workbench.institution;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionEmailAddress;
import org.pmiops.workbench.db.model.DbInstitutionEmailDomain;
import org.pmiops.workbench.model.Institution;

@Mapper(componentModel = "spring")
public interface InstitutionMapper {
  @Mapping(target = "institutionId", ignore = true)
  DbInstitution modelToDb(Institution modelObject);

  Institution dbToModel(DbInstitution dbObject);

  default List<String> toModelDomains(@NotNull Set<DbInstitutionEmailDomain> dbDomains) {
    return dbDomains.stream()
        .map(DbInstitutionEmailDomain::getEmailDomain)
        .distinct()
        .collect(Collectors.toList());
  }

  default List<String> toModelAddresses(@NotNull Set<DbInstitutionEmailAddress> dbAddresses) {
    return dbAddresses.stream()
        .map(DbInstitutionEmailAddress::getEmailAddress)
        .distinct()
        .collect(Collectors.toList());
  }

  // Swagger-generated Lists are null by default, so we should handle that
  default Set<DbInstitutionEmailDomain> toDbDomains(@Nullable List<String> modelDomains) {
    return Optional.ofNullable(modelDomains)
        .map(List::stream)
        .orElse(Stream.empty())
        .map(domain -> new DbInstitutionEmailDomain().setEmailDomain(domain))
        .collect(Collectors.toSet());
  }

  default Set<DbInstitutionEmailAddress> toDbAddresses(@Nullable List<String> modelAddresses) {
    return Optional.ofNullable(modelAddresses)
        .map(List::stream)
        .orElse(Stream.empty())
        .map(address -> new DbInstitutionEmailAddress().setEmailAddress(address))
        .collect(Collectors.toSet());
  }
}
