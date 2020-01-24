package org.pmiops.workbench.institution;

import java.util.List;
import org.pmiops.workbench.model.Institution;

public interface InstitutionService {
  List<Institution> getInstitutions();

  Institution getInstitution(final String id);

  Institution createInstitution(final Institution institutionToCreate);

  void deleteInstitution(final String id);

  Institution updateInstitution(final String id, final Institution institutionToUpdate);
}
