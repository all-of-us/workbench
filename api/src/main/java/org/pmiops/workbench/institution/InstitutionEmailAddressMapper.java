package org.pmiops.workbench.institution;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueMappingStrategy;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionEmailAddress;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.model.TierEmailAddresses;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(
    config = MapStructConfig.class,
    nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
public interface InstitutionEmailAddressMapper {

  default Set<DbInstitutionEmailAddress> modelToDb(
      Institution modelInstitution,
      @Context DbInstitution dbInstitution,
      @Context DbAccessTier dbAccessTier) {
    if (modelInstitution.getTierEmailAddresses() == null
        || modelInstitution.getTierEmailAddresses().isEmpty()) {
      return new HashSet<>();
    }
    return emailAddressesToDb(
        InstitutionUtils.getEmailAddressesByTierOrEmptySet(
            modelInstitution, dbAccessTier.getShortName()),
        dbInstitution,
        dbAccessTier);
  }

  Set<DbInstitutionEmailAddress> emailAddressesToDb(
      Set<String> emailAddresses,
      @Context DbInstitution dbInstitution,
      @Context DbAccessTier dbAccessTier);

  default DbInstitutionEmailAddress emailAddressToDb(
      String emailAddress,
      @Context DbInstitution dbInstitution,
      @Context DbAccessTier dbAccessTier) {
    return new DbInstitutionEmailAddress()
        .setEmailAddress(emailAddress)
        .setInstitution(dbInstitution)
        .setAccessTier(dbAccessTier);
  }

  default List<TierEmailAddresses> dbAddressesToTierEmailAddresses(
      final Set<DbInstitutionEmailAddress> dbAddresses) {
    // Iterate dbAddresss to get tier name to addresss list map. Each of map's entry is a
    // TierEmailAddresses
    Map<String, Set<String>> tierToAddressMap =
        dbAddresses.stream()
            .collect(
                Collectors.groupingBy(
                    d -> d.getAccessTier().getShortName(),
                    Collectors.mapping(
                        DbInstitutionEmailAddress::getEmailAddress,
                        Collectors.toCollection(TreeSet::new))));
    return tierToAddressMap.entrySet().stream()
        .map(
            e ->
                new TierEmailAddresses()
                    .accessTierShortName(e.getKey())
                    .emailAddresses(new ArrayList<>(e.getValue())))
        .collect(Collectors.toList());
  }
}
