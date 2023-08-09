package org.pmiops.workbench.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OfflineWorkspaceController implements OfflineWorkspaceApiDelegate {
  @Deprecated(forRemoval = true) // we are removing the research purpose review feature
  @Override
  public ResponseEntity<Void> updateResearchPurposeReviewPrompt() {
    return ResponseEntity.noContent().build();
  }
}
