package org.pmiops.workbench.institution;

import java.util.List;
import java.util.Optional;
import org.pmiops.workbench.model.Institution;

public interface InstitutionService {
  List<Institution> getInstitutions();

  Optional<Institution> getInstitution(final String shortName);

  Institution createInstitution(final Institution institutionToCreate);

  boolean deleteInstitution(final String shortName);

  Optional<Institution> updateInstitution(
      final String shortName, final Institution institutionToUpdate);
}
