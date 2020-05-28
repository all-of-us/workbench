package org.pmiops.workbench.institution;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionEmailAddress;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(config = MapStructConfig.class)
public interface InstitutionEmailAddressMapper {
  default Set<DbInstitutionEmailAddress> modelToDb(
      final Institution modelInstitution, final DbInstitution dbInstitution) {
    return Optional.ofNullable(modelInstitution.getEmailAddresses()).orElse(Collections.emptyList())
        .stream()
        .distinct()
        .map(
            address ->
                new DbInstitutionEmailAddress()
                    .setEmailAddress(address)
                    .setInstitution(dbInstitution))
        .collect(Collectors.toSet());
  }

  default List<String> dbToModel(Set<DbInstitutionEmailAddress> dbAddresses) {
    return Optional.ofNullable(dbAddresses).orElse(Collections.emptySet()).stream()
        .map(DbInstitutionEmailAddress::getEmailAddress)
        .sorted()
        .collect(Collectors.toList());
  }
}
