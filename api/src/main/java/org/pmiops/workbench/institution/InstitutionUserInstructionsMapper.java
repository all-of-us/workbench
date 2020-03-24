package org.pmiops.workbench.institution;

import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.pmiops.workbench.db.model.DbInstitutionUserInstructions;
import org.pmiops.workbench.model.InstitutionUserInstructions;

@Mapper(componentModel = "spring")
public interface InstitutionUserInstructionsMapper {
  @Mapping(target = "institutionUserInstructionsId", ignore = true)
  @Mapping(target = "institutionId", ignore = true) // set by setDbInstitutionId()
  @Mapping(target = "userInstructions", source = "instructions")
  DbInstitutionUserInstructions modelToDb(
      InstitutionUserInstructions modelObject, @Context InstitutionService institutionService);

  @AfterMapping
  default void setDbInstitutionId(
      @MappingTarget DbInstitutionUserInstructions target,
      InstitutionUserInstructions modelObject,
      @Context InstitutionService institutionService) {

    final long institutionId =
        institutionService
            .getDbInstitutionOrThrow(modelObject.getInstitutionShortName())
            .getInstitutionId();
    final PolicyFactory removeAllTags = new HtmlPolicyBuilder().toFactory();
    final String sanitizedInstructions =
        removeAllTags.sanitize(modelObject.getInstructions()).trim();

    target.setInstitutionId(institutionId).setUserInstructions(sanitizedInstructions);
  }
}
