package org.pmiops.workbench.institution;

import static org.pmiops.workbench.institution.InstitutionUtils.getEmailDomainsByTierOrEmptySet;

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
import org.pmiops.workbench.db.model.DbInstitutionEmailDomain;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.model.TierEmailDomains;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(
    config = MapStructConfig.class,
    nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
public interface InstitutionEmailDomainMapper {

  default Set<DbInstitutionEmailDomain> modelToDb(
      Institution modelInstitution,
      @Context DbInstitution dbInstitution,
      @Context DbAccessTier dbAccessTier) {
    if (modelInstitution.getTierEmailDomains() == null
        || modelInstitution.getTierEmailDomains().isEmpty()) {
      return new HashSet<>();
    }
    return emailDomainsToDb(
        getEmailDomainsByTierOrEmptySet(modelInstitution, dbAccessTier.getShortName()),
        dbInstitution,
        dbAccessTier);
  }

  Set<DbInstitutionEmailDomain> emailDomainsToDb(
      Set<String> emailDomains,
      @Context DbInstitution dbInstitution,
      @Context DbAccessTier dbAccessTier);

  default DbInstitutionEmailDomain emailDomainToDb(
      String emailDomain,
      @Context DbInstitution dbInstitution,
      @Context DbAccessTier dbAccessTier) {
    return new DbInstitutionEmailDomain()
        .setEmailDomain(emailDomain)
        .setInstitution(dbInstitution)
        .setAccessTier(dbAccessTier);
  }

  default List<TierEmailDomains> dbDomainsToTierEmailDomains(
      final Set<DbInstitutionEmailDomain> dbDomains) {
    // Iterate dbDomains to get tier name to domains list map. Each of map's entry is a
    // TierEmailDomains
    Map<String, Set<String>> tierToDomainMap =
        dbDomains.stream()
            .collect(
                Collectors.groupingBy(
                    d -> d.getAccessTier().getShortName(),
                    Collectors.mapping(
                        DbInstitutionEmailDomain::getEmailDomain,
                        Collectors.toCollection(TreeSet::new))));
    return tierToDomainMap.entrySet().stream()
        .map(
            e ->
                new TierEmailDomains()
                    .accessTierShortName(e.getKey())
                    .emailDomains(new ArrayList<>(e.getValue())))
        .collect(Collectors.toList());
  }
}
