package org.pmiops.workbench.cdr;

import com.google.common.annotations.VisibleForTesting;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * Maintains state of what CDR version is being used in the context of the current request.
 */
@Service
public class CdrVersionContext {

  private static ThreadLocal<CdrVersion> cdrVersion = new ThreadLocal<>();

  private Provider<WorkbenchConfig> configProvider;
  private FireCloudService fireCloudService;

  @Autowired
  public CdrVersionContext(Provider<WorkbenchConfig> workbenchConfigProvider,
      FireCloudService fireCloudService) {
    this.configProvider = configProvider;
    this.fireCloudService = fireCloudService;
  }

  public void setCdrVersion(CdrVersion version) {
    if (configProvider.get().firecloud.enforceRegistered) {
      // TODO: map data access level to authorization domain here (RW-943)
      String authorizationDomain = configProvider.get().firecloud.registeredDomainName;
      if (!fireCloudService.isUserMemberOfGroup(authorizationDomain)) {
        throw new ForbiddenException("Requester is not a member of " + authorizationDomain +
            ", cannot access CDR");
      }
    }
    setCdrVersionNoCheckAuthDomain(version);
  }

  /**
   * Call this method from source only if you've already fetched the workspace for the user from
   * Firecloud (and have thus already checked that they are still members of the appropriate
   * authorization domain.) Call it from tests in order to set up the CdrVersion used subsequently
   * when reading CDR metadata or BigQuery.
   */
  public static void setCdrVersionNoCheckAuthDomain(CdrVersion version) {
    cdrVersion.set(version);
  }

  public static void clearCdrVersion() {
    cdrVersion.remove();
  }

  public static CdrVersion getCdrVersion() {
    return cdrVersion.get();
  }
}
