package org.pmiops.workbench.institution;

import java.util.List;
import java.util.Optional;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.model.Institution;

public interface InstitutionService {
  List<Institution> getInstitutions();

  Optional<Institution> getInstitution(final String shortName);

  // TODO find a better way - DB exposure does not belong in a service
  Optional<DbInstitution> getDbInstitution(final String shortName);

  Institution createInstitution(final Institution institutionToCreate);

  enum DeletionResult {
    SUCCESS,
    NOT_FOUND,
    HAS_VERIFIED_AFFILIATIONS
  }

  DeletionResult deleteInstitution(final String shortName);

  Optional<Institution> updateInstitution(
      final String shortName, final Institution institutionToUpdate);
}
