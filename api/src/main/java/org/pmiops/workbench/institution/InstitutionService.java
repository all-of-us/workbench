package org.pmiops.workbench.institution;

import java.util.List;
import java.util.Optional;
import org.pmiops.workbench.model.Institution;

public interface InstitutionService {
  List<Institution> getInstitutions();

  Optional<Institution> getInstitution(final String id);

  Institution createInstitution(final Institution institutionToCreate);

  boolean deleteInstitution(final String id);

  Optional<Institution> updateInstitution(final String id, final Institution institutionToUpdate);
}
