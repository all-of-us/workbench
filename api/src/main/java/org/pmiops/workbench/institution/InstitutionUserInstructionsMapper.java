package org.pmiops.workbench.institution;

import com.google.common.base.Strings;
import org.mapstruct.Mapper;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionUserInstructions;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.InstitutionUserInstructions;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(config = MapStructConfig.class)
public interface InstitutionUserInstructionsMapper {
  default DbInstitutionUserInstructions modelToDb(
      InstitutionUserInstructions modelObject, DbInstitution institution) {

    // don't store empty or null instructions
    final String instructions = modelObject.getInstructions();
    if (Strings.isNullOrEmpty(instructions)) {
      throw new BadRequestException(
          "Cannot save InstitutionUserInstructions because the instructions are missing.");
    }
    return new DbInstitutionUserInstructions()
        .setInstitution(institution)
        .setUserInstructions(instructions);
  }
}
