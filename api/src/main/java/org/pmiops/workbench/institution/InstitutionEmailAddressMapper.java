package org.pmiops.workbench.institution;

import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import org.mapstruct.Context;
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
      Institution modelInstitution, @Context DbInstitution dbInstitution) {
    return emailAddressesToDb(modelInstitution.getEmailAddresses(), dbInstitution);
  }

  Set<DbInstitutionEmailAddress> emailAddressesToDb(
      List<String> emailAddresses, @Context DbInstitution dbInstitution);

  default DbInstitutionEmailAddress emailAddressToDb(
      String emailAddress, @Context DbInstitution dbInstitution) {
    return new DbInstitutionEmailAddress()
        .setEmailAddress(emailAddress)
        .setInstitution(dbInstitution);
  }

  SortedSet<String> dbAddressesToStrings(final Set<DbInstitutionEmailAddress> dbAddresses);

  default String dbAddressToString(final DbInstitutionEmailAddress dbAddress) {
    return dbAddress.getEmailAddress();
  }
}
