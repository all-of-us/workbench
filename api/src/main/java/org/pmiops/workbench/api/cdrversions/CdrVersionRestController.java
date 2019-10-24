package org.pmiops.workbench.api.cdrversions;

import com.google.common.annotations.VisibleForTesting;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.api.CdrVersionsApiDelegate;
import org.pmiops.workbench.cdr.ImmutableCdrVersion;
import org.pmiops.workbench.cdr.CdrVersionService;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.model.CdrVersionListResponse;
import org.pmiops.workbench.model.DataAccessLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CdrVersionRestController implements CdrVersionsApiDelegate {
  private static final Logger log = Logger.getLogger(CdrVersionRestController.class.getName());

//  @VisibleForTesting
//  public static final Function<CdrVersionEntity, org.pmiops.workbench.model.CdrVersion> TO_CLIENT_CDR_VERSION =
//      (CdrVersionEntity cdrVersionEntity) ->
//          new org.pmiops.workbench.model.CdrVersion()
//              .cdrVersionId(String.valueOf(cdrVersionEntity.getCdrVersionId()))
//              .creationTime(cdrVersionEntity.getCreationTime().getTime())
//              .dataAccessLevel(cdrVersionEntity.getDataAccessLevelEnum())
//              .archivalStatus(cdrVersionEntity.getArchivalStatusEnum())
//              .name(cdrVersionEntity.getName());

  private final CdrVersionService cdrVersionService;
  private Provider<User> userProvider;

  @Autowired
  CdrVersionRestController(CdrVersionService cdrVersionService, Provider<User> userProvider) {
    this.cdrVersionService = cdrVersionService;
    this.userProvider = userProvider;
  }
  // $REFACTOR$ create a service and delegate to these methods

  // $REFACTOR$ this method shouldn't be necessary. Set the test up to use a mock or fake
  // directly provided
  @VisibleForTesting
  public void setUserProvider(Provider<User> userProvider) {
    this.userProvider = userProvider;
  }

  @Override
  public ResponseEntity<CdrVersionListResponse> getCdrVersions() {
    // TODO: Consider filtering this based on what is currently instantiated as a data source. Newly
    // added CDR versions will not function until a server restart.
    DataAccessLevel accessLevel = userProvider.get().getDataAccessLevelEnum();
    List<ImmutableCdrVersion> cdrVersionEntities = cdrVersionService.findAuthorizedCdrVersions(accessLevel);
    if (cdrVersionEntities.isEmpty()) {
      throw new ForbiddenException("User does not have access to any CDR versions");
    }
    List<Long> defaultVersions =
        cdrVersionEntities.stream()
            .filter(ImmutableCdrVersion::isDefault)
            .map(ImmutableCdrVersion::getCdrVersionId)
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
            .items(cdrVersionEntities.stream()
                .map(ImmutableCdrVersion::toClientCdrVerrsion)
                .collect(Collectors.toList()))
            .defaultCdrVersionId(Long.toString(defaultVersions.get(0))));
  }
}
