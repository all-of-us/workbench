package org.pmiops.workbench.institution;

import static org.pmiops.workbench.institution.InstitutionUtils.getEmailAddressesByTierOrThrow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueMappingStrategy;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionEmailAddress;
import org.pmiops.workbench.db.model.DbInstitutionEmailDomain;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.model.TierEmailAddresses;
import org.pmiops.workbench.model.TierEmailDomains;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(
    config = MapStructConfig.class,
    nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
public interface InstitutionEmailAddressMapper {

  default Set<DbInstitutionEmailAddress> modelToDb(
      Institution modelInstitution, @Context DbInstitution dbInstitution, @Context DbAccessTier dbAccessTier) {
    return emailAddressesToDb(getEmailAddressesByTierOrThrow(modelInstitution, dbAccessTier.getShortName()), dbInstitution, dbAccessTier);
  }

  Set<DbInstitutionEmailAddress> emailAddressesToDb(
      Set<String> emailAddresses, @Context DbInstitution dbInstitution, @Context DbAccessTier dbAccessTier);

  default DbInstitutionEmailAddress emailAddressToDb(
      String emailAddress, @Context DbInstitution dbInstitution, @Context DbAccessTier dbAccessTier) {
    return new DbInstitutionEmailAddress()
        .setEmailAddress(emailAddress)
        .setInstitution(dbInstitution).setAccessTier(dbAccessTier);
  }

  default List<TierEmailAddresses> dbAddressesToTierEmailAddresses(final Set<DbInstitutionEmailAddress> dbAddresses) {
    // Iterate dbDomains to get tier name to domains list map. Each of map's entry is a
    // TierEmailAddresses
    Map<String, Set<String>> tierToDomainMap = dbAddresses.stream()
        .collect(Collectors.groupingBy(d -> d.getAccessTier().getShortName(),
            Collectors.mapping(DbInstitutionEmailAddress::getEmailAddress, Collectors.toSet())));
    List<TierEmailAddresses> result = new ArrayList<>();
    tierToDomainMap.forEach((key, value) -> result
        .add(new TierEmailAddresses().accessTierShortName(key).emailAddresses(new ArrayList<>(value))));
    return result;
  }
}
