package org.pmiops.workbench.institution;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.Nullable;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionEmailAddress;
import org.pmiops.workbench.db.model.DbInstitutionEmailDomain;
import org.pmiops.workbench.db.model.DbInstitutionUserInstructions;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(config = MapStructConfig.class)
public interface InstitutionMapper {
  @Mapping(target = "institutionId", ignore = true)
  @Mapping(target = "userInstructions", ignore = true)
  DbInstitution modelToDb(Institution modelObject, @Context InstitutionService institutionService);

  // userInstructions will be populated by setUserInstruction afterMapping
  @Mapping(target = "userInstructions", source = "userInstructions.userInstructions")
  Institution dbToModel(DbInstitution dbObject);

  default List<String> toModelDomains(Set<DbInstitutionEmailDomain> dbDomains) {
    return Optional.ofNullable(dbDomains).orElse(Collections.emptySet()).stream()
        .map(DbInstitutionEmailDomain::getEmailDomain)
        .sorted()
        .collect(Collectors.toList());
  }

  default List<String> toModelAddresses(Set<DbInstitutionEmailAddress> dbAddresses) {
    return Optional.ofNullable(dbAddresses).orElse(Collections.emptySet()).stream()
        .map(DbInstitutionEmailAddress::getEmailAddress)
        .sorted()
        .collect(Collectors.toList());
  }

  // Swagger-generated Lists are null by default, so we should handle that
  default Set<DbInstitutionEmailDomain> toDbDomainsWithoutInstitution(
      @Nullable Collection<String> modelDomains) {
    return Optional.ofNullable(modelDomains).orElse(Collections.emptySet()).stream()
        .map(domain -> new DbInstitutionEmailDomain().setEmailDomain(domain))
        .collect(Collectors.toSet());
  }

  default Set<DbInstitutionEmailAddress> toDbAddressesWithoutInstitution(
      @Nullable Collection<String> modelAddresses) {
    return Optional.ofNullable(modelAddresses).orElse(Collections.emptySet()).stream()
        .map(address -> new DbInstitutionEmailAddress().setEmailAddress(address))
        .collect(Collectors.toSet());
  }

  @AfterMapping
  default void setFields(
      @MappingTarget DbInstitution target,
      Institution modelObject,
      @Context InstitutionService institutionService) {

    final PolicyFactory removeAllTags = new HtmlPolicyBuilder().toFactory();
    final String sanitizedInstructions =
        removeAllTags.sanitize(modelObject.getUserInstructions()).trim();

    DbInstitutionUserInstructions dbInstitutionUserInstructions =
        institutionService
            .getDbInstitutionUserInstructions(modelObject.getShortName())
            .orElse(new DbInstitutionUserInstructions())
            .setUserInstructions(sanitizedInstructions);
    target.setUserInstructions(dbInstitutionUserInstructions);
  }
}
