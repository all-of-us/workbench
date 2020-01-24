package org.pmiops.workbench.institution;

import javax.inject.Provider;
import org.pmiops.workbench.api.InstitutionApiDelegate;
import org.pmiops.workbench.model.GetInstitutionsResponse;
import org.pmiops.workbench.model.Institution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InstitutionController implements InstitutionApiDelegate {

  private final Provider<InstitutionService> institutionServiceProvider;

  @Autowired
  InstitutionController(Provider<InstitutionService> institutionServiceProvider) {
    this.institutionServiceProvider = institutionServiceProvider;
  }

  @Override
  public ResponseEntity<Institution> createInstitution(Institution institution) {
    return ResponseEntity.ok(institutionServiceProvider.get().createInstitution(institution));
  }

  @Override
  public ResponseEntity<Void> deleteInstitution(final String id) {
    institutionServiceProvider.get().deleteInstitution(id);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Institution> getInstitution(final String id) {
    return ResponseEntity.ok(institutionServiceProvider.get().getInstitution(id));
  }

  @Override
  public ResponseEntity<GetInstitutionsResponse> getInstitutions() {
    final GetInstitutionsResponse response =
        new GetInstitutionsResponse()
            .institutions(institutionServiceProvider.get().getInstitutions());
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<Institution> updateInstitution(
      final String id, final Institution institutionToUpdate) {
    return ResponseEntity.ok(
        institutionServiceProvider.get().updateInstitution(id, institutionToUpdate));
  }
}
