package org.pmiops.workbench.api;

import com.google.api.services.cloudresourcemanager.model.ResourceId;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.AuditProjectAccessRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CloudTaskUserController implements CloudTaskUserApiDelegate {

  @Override
  public ResponseEntity<Void> auditProjectAccess(AuditProjectAccessRequest request) {

    int errorCount = 0;
    // For now, continue checking both enabled and disabled users. If needed for performance, this
    // could be scoped down to just enabled users. However, access to other GCP resources could also
    // indicate general Google account abuse, which may be a concern regardless of whether or not
    // the user has been disabled in the Workbench.

    for (DbUser user : users) {
      // TODO(RW-2062): Move to using the gcloud api for list all resources when it is available.
      try {
        List<String> unauthorizedLogs =
            cloudResourceManagerService.getAllProjectsForUser(user).stream()
                .filter(
                    project ->
                        project.getParent() == null
                            || !(WHITELISTED_ORG_IDS.contains(project.getParent().getId())))
                .map(
                    project ->
                        project.getName()
                            + " in organization "
                            + Optional.ofNullable(project.getParent())
                                .map(ResourceId::getId)
                                .orElse("[none]"))
                .collect(Collectors.toList());
        if (unauthorizedLogs.size() > 0) {
          log.warning(
              "User "
                  + user.getUsername()
                  + " has access to projects: "
                  + String.join(", ", unauthorizedLogs));
        }
      } catch (IOException e) {
        log.log(Level.SEVERE, "failed to audit project access for user " + user.getUsername(), e);
        errorCount++;
      }
    }
    if (errorCount > 0) {
      log.severe(String.format("encountered errors on %d/%d users", errorCount, users.size()));
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
    log.info(String.format("successfully audited %d users", users.size()));
    return ResponseEntity.noContent().build();
  }
}
