package org.pmiops.workbench.api;

import org.pmiops.workbench.annotations.AuthorityRequired;
import org.pmiops.workbench.exceptions.NotImplementedException;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.ListEgressEventsRequest;
import org.pmiops.workbench.model.ListEgressEventsResponse;
import org.springframework.http.ResponseEntity;

public class EgressEventsAdminController implements EgressEventsAdminApiDelegate {

  @AuthorityRequired(Authority.SECURITY_ADMIN)
  @Override
  public ResponseEntity<ListEgressEventsResponse> listEgressEvents(
      ListEgressEventsRequest request) {
    throw new NotImplementedException();
  }
}
