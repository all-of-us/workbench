package org.pmiops.workbench.cdr;

import jakarta.inject.Provider;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.CdrVersion;
import org.pmiops.workbench.model.CdrVersionTier;
import org.pmiops.workbench.model.CdrVersionTiersResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CdrVersionService {
  private final AccessTierService accessTierService;
  private final CdrVersionDao cdrVersionDao;
  private final CdrVersionMapper cdrVersionMapper;
  private final FireCloudService fireCloudService;
  private final Provider<DbUser> userProvider;

  private static final Logger log = Logger.getLogger(CdrVersionService.class.getName());

  @Autowired
  public CdrVersionService(
      AccessTierService accessTierService,
      CdrVersionDao cdrVersionDao,
      CdrVersionMapper cdrVersionMapper,
      FireCloudService fireCloudService,
      Provider<DbUser> userProvider) {
    this.accessTierService = accessTierService;
    this.cdrVersionDao = cdrVersionDao;
    this.cdrVersionMapper = cdrVersionMapper;
    this.fireCloudService = fireCloudService;
    this.userProvider = userProvider;
  }

  /**
   * Sets the active CDR version, after checking to ensure that the requester is in the appropriate
   * authorization domain. If you have already retrieved a workspace for the requester (and thus
   * implicitly know they are in the authorization domain for its CDR version), you can instead just
   * call {@link CdrVersionContext#setCdrVersionNoCheckAuthDomain(DbCdrVersion)} directly.
   *
   * @param version
   */
  public void setCdrVersion(DbCdrVersion version) {
    if (!accessTierService
        .getAccessTiersForUser(userProvider.get())
        .contains(version.getAccessTier())) {
      throw new ForbiddenException(
          "Requester does not have access to tier "
              + version.getAccessTier().getShortName()
              + ", cannot access CDR");
    }

    String authorizationDomain = version.getAccessTier().getAuthDomainName();
    if (!fireCloudService.isUserMemberOfGroupWithCache(
        userProvider.get().getUsername(), authorizationDomain)) {
      throw new ForbiddenException(
          "Requester is not a member of " + authorizationDomain + ", cannot access CDR");
    }

    CdrVersionContext.setCdrVersionNoCheckAuthDomain(version);
  }

  public Optional<DbCdrVersion> findByCdrVersionId(Long cdrVersionId) {
    return cdrVersionDao.findById(cdrVersionId);
  }

  /**
   * Assemble a CdrVersionTiersResponse consisting of all the CDR Versions in each tier, along with
   * the default CDR Version's ID and creation time for each tier.
   */
  public CdrVersionTiersResponse getCdrVersionsByTier() {
    boolean hasRegisteredTierAccess =
        accessTierService.getAccessTiersForUser(userProvider.get()).stream()
            .anyMatch(
                tier -> AccessTierService.REGISTERED_TIER_SHORT_NAME.equals(tier.getShortName()));
    if (!hasRegisteredTierAccess) {
      throw new ForbiddenException("User does not have access to any CDR versions");
    }

    return new CdrVersionTiersResponse()
        .tiers(
            accessTierService.getAllTiers().stream()
                .map(this::getVersionsForTier)
                .collect(Collectors.toList()));
  }

  private CdrVersionTier getVersionsForTier(DbAccessTier accessTier) {
    List<DbCdrVersion> cdrVersions =
        cdrVersionDao.findByAccessTierOrderByCreationTimeDesc(accessTier);
    if (cdrVersions.isEmpty()) {
      throw new ForbiddenException(
          String.format(
              "User does not have access to any CDR versions in access tier '%s'",
              accessTier.getShortName()));
    }

    CdrVersion defaultVersion =
        cdrVersionMapper.dbModelToClient(getDefaultVersionForTier(accessTier, cdrVersions));

    return new CdrVersionTier()
        .versions(
            cdrVersions.stream()
                .map(cdrVersionMapper::dbModelToClient)
                .collect(Collectors.toList()))
        .defaultCdrVersionId(defaultVersion.getCdrVersionId())
        .defaultCdrVersionCreationTime(defaultVersion.getCreationTime())
        .accessTierShortName(accessTier.getShortName())
        .accessTierDisplayName(accessTier.getDisplayName());
  }

  private DbCdrVersion getDefaultVersionForTier(
      DbAccessTier accessTier, List<DbCdrVersion> cdrVersions) {
    List<DbCdrVersion> defaultVersions =
        cdrVersions.stream().filter(DbCdrVersion::getIsDefaultNotNull).collect(Collectors.toList());
    if (defaultVersions.isEmpty()) {
      throw new ForbiddenException(
          String.format(
              "User does not have access to a default CDR version in access tier '%s'",
              accessTier.getShortName()));
    }
    if (defaultVersions.size() > 1) {
      log.severe(
          String.format(
              "Found multiple (%d) default CDR versions in access tier '%s', picking one",
              defaultVersions.size(), accessTier.getShortName()));
    }
    return defaultVersions.get(0);
  }
}
