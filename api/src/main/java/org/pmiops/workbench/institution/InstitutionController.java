package org.pmiops.workbench.institution;

import org.pmiops.workbench.api.InstitutionApiDelegate;
import org.pmiops.workbench.exceptions.NotFoundException;
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
  public ResponseEntity<Institution> createInstitution(Institution institution) {
    return ResponseEntity.ok(institutionService.createInstitution(institution));
  }

  @Override
  public ResponseEntity<Void> deleteInstitution(final String id) {
    if (institutionService.deleteInstitution(id)) {
      return ResponseEntity.noContent().build();
    } else {
      throw new NotFoundException(String.format("Could not delete Institution with ID %s", id));
    }
  }

  @Override
  public ResponseEntity<Institution> getInstitution(final String id) {
    final Institution institution =
        institutionService
            .getInstitution(id)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        String.format("Could not find Institution with ID %s", id)));

    return ResponseEntity.ok(institution);
  }

  @Override
  public ResponseEntity<GetInstitutionsResponse> getInstitutions() {
    final GetInstitutionsResponse response =
        new GetInstitutionsResponse().institutions(institutionService.getInstitutions());
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<Institution> updateInstitution(
      final String id, final Institution institutionToUpdate) {
    final Institution institution =
        institutionService
            .updateInstitution(id, institutionToUpdate)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        String.format("Could not update Institution with ID %s", id)));

    return ResponseEntity.ok(institution);
  }
}
