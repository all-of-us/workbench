package org.pmiops.workbench.institution;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueMappingStrategy;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionEmailAddress;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(
    config = MapStructConfig.class,
    nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
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

  SortedSet<String> dbAddressesToStrings(final Set<DbInstitutionEmailAddress> dbAddresses);

  default String dbAddressToString(final DbInstitutionEmailAddress dbAddress) {
    return dbAddress.getEmailAddress();
  }
}
