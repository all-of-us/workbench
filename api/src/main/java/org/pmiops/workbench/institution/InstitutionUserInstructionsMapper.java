package org.pmiops.workbench.institution;

import com.google.common.base.Strings;
import org.mapstruct.Mapper;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionUserInstructions;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.InstitutionUserInstructions;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(config = MapStructConfig.class)
public interface InstitutionUserInstructionsMapper {
  default DbInstitutionUserInstructions modelToDb(
      InstitutionUserInstructions modelObject, InstitutionService institutionService) {

    // don't store empty or null instructions
    // sanitize() converts null to empty string, so we can't rely on it for this check

    final String instructions = modelObject.getInstructions();
    if (Strings.isNullOrEmpty(instructions)) {
      throw new BadRequestException(
          "Cannot save InstitutionUserInstructions because the instructions payload is empty.");
    }

    final DbInstitution institution =
        institutionService.getDbInstitutionOrThrow(modelObject.getInstitutionShortName());
    final PolicyFactory removeAllTags = new HtmlPolicyBuilder().toFactory();
    final String sanitizedInstructions = removeAllTags.sanitize(instructions).trim();

    return new DbInstitutionUserInstructions()
        .setInstitution(institution)
        .setUserInstructions(sanitizedInstructions);
  }
}
