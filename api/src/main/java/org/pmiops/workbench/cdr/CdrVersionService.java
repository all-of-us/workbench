package org.pmiops.workbench.cdr;

import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CdrVersionService {

  private Provider<WorkbenchConfig> configProvider;
  private FireCloudService fireCloudService;

  @Autowired
  public CdrVersionService(Provider<WorkbenchConfig> configProvider,
      FireCloudService fireCloudService) {
    this.configProvider = configProvider;
    this.fireCloudService = fireCloudService;
  }

  /**
   * Sets the active CDR version, after checking to ensure that the requester is in the appropriate
   * authorization domain. If you have already retrieved a workspace for the requester (and thus
   * implicitly know they are in the authorization domain for its CDR version), you can instead
   * just call {@link CdrVersionContext#setCdrVersionNoCheckAuthDomain(CdrVersion)} directly.
   * @param version
   */
  public void setCdrVersion(CdrVersion version) {
    if (configProvider.get().firecloud.enforceRegistered) {
      // TODO: map data access level to authorization domain here (RW-943)
      String authorizationDomain = configProvider.get().firecloud.registeredDomainName;
      if (!fireCloudService.isUserMemberOfGroup(authorizationDomain)) {
        throw new ForbiddenException("Requester is not a member of " + authorizationDomain +
            ", cannot access CDR");
      }
    }
    CdrVersionContext.setCdrVersionNoCheckAuthDomain(version);
  }
}
