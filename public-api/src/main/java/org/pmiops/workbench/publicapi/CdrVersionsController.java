package org.pmiops.workbench.publicapi;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.db.model.CommonStorageEnums;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.model.CdrVersionListResponse;
import org.pmiops.workbench.model.DataAccessLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CdrVersionsController implements CdrVersionsApiDelegate {
  private static final Logger log = Logger.getLogger(CdrVersionsController.class.getName());

  @VisibleForTesting
  static final Function<CdrVersion, org.pmiops.workbench.model.CdrVersion> TO_CLIENT_CDR_VERSION =
      new Function<CdrVersion, org.pmiops.workbench.model.CdrVersion>() {
        @Override
        public org.pmiops.workbench.model.CdrVersion apply(CdrVersion cdrVersion) {
          return new org.pmiops.workbench.model.CdrVersion()
              .cdrVersionId(String.valueOf(cdrVersion.getCdrVersionId()))
              .creationTime(cdrVersion.getCreationTime().getTime())
              .dataAccessLevel(cdrVersion.getDataAccessLevelEnum())
              .name(cdrVersion.getName());
        }
      };

  private final CdrVersionDao cdrVersionDao;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  @Autowired
  CdrVersionsController(CdrVersionDao cdrVersionDao, Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.cdrVersionDao = cdrVersionDao;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }


  @Override
  public ResponseEntity<CdrVersionListResponse> getCdrVersions() {
    // We return CDR versions for just registered CDR versions; controlled CDR data is currently
    // out of scope for the data browser.
    List<CdrVersion> cdrVersions = cdrVersionDao
        .findByDataAccessLevelInOrderByCreationTimeDescDataAccessLevelDesc(
            ImmutableSet.of(CommonStorageEnums.dataAccessLevelToStorage(DataAccessLevel.REGISTERED)));
    List<Long> defaultVersions = cdrVersions.stream()
      .filter(v -> v.getIsDefault())
      .map(CdrVersion::getCdrVersionId)
      .collect(Collectors.toList());
    if (defaultVersions.isEmpty()) {
      throw new ServerErrorException("Did not find a default CDR version");
    }
    if (defaultVersions.size() > 1) {
      log.severe(String.format(
          "Found multiple (%d) default CDR versions, picking one", defaultVersions.size()));
    }
    // TODO: consider different default CDR versions for different access levels
    return ResponseEntity.ok(new CdrVersionListResponse()
      .items(cdrVersions.stream()
        .map(TO_CLIENT_CDR_VERSION)
        .collect(Collectors.toList()))
      .defaultCdrVersionId(Long.toString(defaultVersions.get(0))));
  }
}
