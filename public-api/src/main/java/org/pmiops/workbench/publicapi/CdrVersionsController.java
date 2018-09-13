package org.pmiops.workbench.publicapi;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.function.Function;
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
    // TODO: consider different default CDR versions for different access levels
    String defaultCdrVersionName = workbenchConfigProvider.get().cdr.defaultCdrVersion;
    String defaultCdrVersionId = null;
    for (CdrVersion version : cdrVersions) {
      if (defaultCdrVersionName.equals(version.getName())) {
        defaultCdrVersionId = String.valueOf(version.getCdrVersionId());
      }
    }
    if (defaultCdrVersionId == null) {
      // This shouldn't happen.
      throw new ServerErrorException("User does not have access to default CDR version");
    }
    CdrVersionListResponse response = new CdrVersionListResponse();
    response.setItems(cdrVersions.stream()
        .map(TO_CLIENT_CDR_VERSION)
        .collect(Collectors.toList()));
    response.setDefaultCdrVersionId(defaultCdrVersionId);
    return ResponseEntity.ok(response);
  }
}
