package org.pmiops.workbench.api;

import org.pmiops.workbench.annotations.AuthorityRequired;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.institution.InstitutionService.DeletionResult;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.GetInstitutionsResponse;
import org.pmiops.workbench.model.Institution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InstitutionController implements InstitutionApiDelegate {

  private final InstitutionService institutionService;

  @Autowired
  InstitutionController(InstitutionService institutionService) {
    this.institutionService = institutionService;
  }

  @Override
  @AuthorityRequired({Authority.INSTITUTION_ADMIN})
  public ResponseEntity<Institution> createInstitution(final Institution institution) {
    return ResponseEntity.ok(institutionService.createInstitution(institution));
  }

  @Override
  @AuthorityRequired({Authority.INSTITUTION_ADMIN})
  public ResponseEntity<Void> deleteInstitution(final String shortName) {
    final DeletionResult result = institutionService.deleteInstitution(shortName);

    // I wanted to use a switch here but Java complained about lacking a return value
    if (result == DeletionResult.HAS_VERIFIED_AFFILIATIONS) {
      // TODO: 405 or 409?
      // https://stackoverflow.com/questions/25122472/rest-http-status-code-if-delete-impossible
      // https://stackoverflow.com/questions/45899743/proper-http-error-for-deleting-non-empty-resource
      throw new ConflictException(
          String.format(
              "Could not delete Institution '%s' because it has verified user affiliations",
              shortName));
    } else if (result == DeletionResult.NOT_FOUND) {
      throw new NotFoundException(
          String.format("Could not delete Institution '%s' because it was not found", shortName));
    }

    // result == SUCCESS
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Institution> getInstitution(final String shortName) {
    final Institution institution =
        institutionService
            .getInstitution(shortName)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        String.format("Could not find Institution '%s", shortName)));

    return ResponseEntity.ok(institution);
  }

  @Override
  public ResponseEntity<GetInstitutionsResponse> getInstitutions() {
    final GetInstitutionsResponse response =
        new GetInstitutionsResponse().institutions(institutionService.getInstitutions());
    return ResponseEntity.ok(response);
  }

  @Override
  @AuthorityRequired({Authority.INSTITUTION_ADMIN})
  public ResponseEntity<Institution> updateInstitution(
      final String shortName, final Institution institutionToUpdate) {
    final Institution institution =
        institutionService
            .updateInstitution(shortName, institutionToUpdate)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        String.format("Could not update Institution '%s", shortName)));

    return ResponseEntity.ok(institution);
  }
}
