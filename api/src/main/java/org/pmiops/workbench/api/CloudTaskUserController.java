package org.pmiops.workbench.api;

import com.google.api.services.cloudresourcemanager.model.ResourceId;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.google.CloudResourceManagerService;
import org.pmiops.workbench.model.AuditProjectAccessRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CloudTaskUserController implements CloudTaskUserApiDelegate {
  private static final Set<String> ALLOWED_PARENT_IDS =
      ImmutableSet.of(
          "400176686919", // test.firecloud.org
          "386193000800", // firecloud.org
          "394551486437", // pmi-ops.org
          // TODO(calbach): Consider moving this into cdr_config_*.json
          "737976594150", // terra_dev_aou_test
          "272722258246", // terra_dev_aou_test_2
          "791559643292", // terra_perf_aou_perf
          "851853921816", // terra_perf_aou_perf_2
          "727685043294", // terra_prod_aou_staging
          "730698401092", // terra_prod_aou_staging_2
          "96867205717", // terra_prod_aou_stable
          "973382847971", // terra_prod_aou_stable_2
          "1030649683602", // terra_prod_aou_preprod
          "307813759063", // terra_prod_aou_preprod_2
          "300547813290", // terra_prod_aou_prod
          "762320479256"); // terra_prod_aou_prod_2
  private static final Logger log = Logger.getLogger(CloudTaskUserController.class.getName());

  private final UserDao userDao;
  private final CloudResourceManagerService cloudResourceManagerService;

  CloudTaskUserController(
      UserDao userDao, CloudResourceManagerService cloudResourceManagerService) {
    this.userDao = userDao;
    this.cloudResourceManagerService = cloudResourceManagerService;
  }

  @Override
  public ResponseEntity<Void> auditProjectAccess(AuditProjectAccessRequest request) {
    int errorCount = 0;
    for (long userId : request.getUserIds()) {
      DbUser user = userDao.findUserByUserId(userId);

      // TODO(RW-2062): Move to using the gcloud api for list all resources when it is available.
      try {
        List<String> unauthorizedLogs =
            cloudResourceManagerService.getAllProjectsForUser(user).stream()
                .filter(
                    project ->
                        project.getParent() == null
                            || !(ALLOWED_PARENT_IDS.contains(project.getParent().getId())))
                .map(
                    project ->
                        String.format(
                            "%s in %s %s",
                            project.getName(),
                            Optional.ofNullable(project.getParent())
                                .map(ResourceId::getType)
                                .orElse("[type unknown]"),
                            Optional.ofNullable(project.getParent())
                                .map(ResourceId::getId)
                                .orElse("[id unknown]")))
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
      log.severe(
          String.format(
              "encountered errors on %d/%d users", errorCount, request.getUserIds().size()));
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
    log.info(String.format("successfully audited %d users", request.getUserIds().size()));
    return ResponseEntity.noContent().build();
  }
}
