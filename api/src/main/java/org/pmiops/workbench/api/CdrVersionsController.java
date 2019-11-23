package org.pmiops.workbench.api;

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.cdr.CdrVersionService;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.model.CdrVersion;
import org.pmiops.workbench.model.CdrVersionListResponse;
import org.pmiops.workbench.model.DataAccessLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CdrVersionsController implements CdrVersionsApiDelegate {
  private static final Logger log = Logger.getLogger(CdrVersionsController.class.getName());

  @VisibleForTesting
  static final Function<DbCdrVersion, CdrVersion> TO_CLIENT_CDR_VERSION =
      (DbCdrVersion cdrVersion) ->
          new CdrVersion()
              .cdrVersionId(String.valueOf(cdrVersion.getCdrVersionId()))
              .creationTime(cdrVersion.getCreationTime().getTime())
              .dataAccessLevel(cdrVersion.getDataAccessLevelEnum())
              .archivalStatus(cdrVersion.getArchivalStatusEnum())
              .name(cdrVersion.getName());

  private final CdrVersionService cdrVersionService;
  private Provider<DbUser> userProvider;

  @Autowired
  CdrVersionsController(CdrVersionService cdrVersionService, Provider<DbUser> userProvider) {
    this.cdrVersionService = cdrVersionService;
    this.userProvider = userProvider;
  }

  @Override
  public ResponseEntity<CdrVersionListResponse> getCdrVersions() {
    // TODO: Consider filtering this based on what is currently instantiated as a data source. Newly
    // added CDR versions will not function until a server restart.
    DataAccessLevel accessLevel = userProvider.get().getDataAccessLevelEnum();
    List<DbCdrVersion> cdrVersions = cdrVersionService.findAuthorizedCdrVersions(accessLevel);
    if (cdrVersions.isEmpty()) {
      throw new ForbiddenException("User does not have access to any CDR versions");
    }
    List<Long> defaultVersions =
        cdrVersions.stream()
            .filter(v -> v.getIsDefault())
            .map(DbCdrVersion::getCdrVersionId)
            .collect(Collectors.toList());
    if (defaultVersions.isEmpty()) {
      throw new ForbiddenException("User does not have access to a default CDR version");
    }
    if (defaultVersions.size() > 1) {
      log.severe(
          String.format(
              "Found multiple (%d) default CDR versions, picking one", defaultVersions.size()));
    }
    // TODO: consider different default CDR versions for different access levels
    return ResponseEntity.ok(
        new CdrVersionListResponse()
            .items(cdrVersions.stream().map(TO_CLIENT_CDR_VERSION).collect(Collectors.toList()))
            .defaultCdrVersionId(Long.toString(defaultVersions.get(0))));
  }
}
