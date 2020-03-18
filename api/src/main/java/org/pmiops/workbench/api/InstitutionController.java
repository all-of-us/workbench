package org.pmiops.workbench.api;

import org.pmiops.workbench.annotations.AuthorityRequired;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.CheckEmailResponse;
import org.pmiops.workbench.model.CheckInstitutionResponse;
import org.pmiops.workbench.model.GetInstitutionsResponse;
import org.pmiops.workbench.model.GetPublicInstitutionDetailsResponse;
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
    institutionService.deleteInstitution(shortName);
    return ResponseEntity.noContent().build();
  }

  @Override
  @AuthorityRequired({Authority.INSTITUTION_ADMIN})
  public ResponseEntity<Institution> getInstitution(final String shortName) {
    final Institution institution =
        institutionService
            .getInstitution(shortName)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        String.format("Could not find Institution '%s'", shortName)));

    return ResponseEntity.ok(institution);
  }

  @Override
  @AuthorityRequired({Authority.INSTITUTION_ADMIN})
  public ResponseEntity<GetInstitutionsResponse> getInstitutions() {
    final GetInstitutionsResponse response =
        new GetInstitutionsResponse().institutions(institutionService.getInstitutions());
    return ResponseEntity.ok(response);
  }

  /**
   * This API is publicly-accessible since it is called during account creation.
   * @return
   */
  @Override
  public ResponseEntity<GetPublicInstitutionDetailsResponse> getPublicInstitutionDetails() {
    final GetPublicInstitutionDetailsResponse response =
        new GetPublicInstitutionDetailsResponse()
            .institutions(institutionService.getPublicInstitutionDetails());
    return ResponseEntity.ok(response);
  }

  /**
   * This API is publicly-accessible since it is called during account creation.
   * @return
   */
  public ResponseEntity<CheckEmailResponse> checkEmail(String email, String institutionShortName) {
    final Institution institution =
        institutionService
            .getInstitution(institutionShortName)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        String.format("Could not find Institution '%s'", institutionShortName)));
    return ResponseEntity.ok(
        new CheckEmailResponse()
            .isValidMember(institutionService.validateInstitutionalEmail(institution, email)));
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
                        String.format("Could not update Institution '%s'", shortName)));

    return ResponseEntity.ok(institution);
  }
}
