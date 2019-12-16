package org.pmiops.workbench.cdr;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.model.CommonStorageEnums;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.DataAccessLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CdrVersionService {

  private static final ImmutableSet<Short> REGISTERED_ONLY =
      ImmutableSet.of(CommonStorageEnums.dataAccessLevelToStorage(DataAccessLevel.REGISTERED));

  private static final ImmutableSet<Short> REGISTERED_AND_PROTECTED =
      ImmutableSet.of(
          CommonStorageEnums.dataAccessLevelToStorage(DataAccessLevel.REGISTERED),
          CommonStorageEnums.dataAccessLevelToStorage(DataAccessLevel.PROTECTED));

  private static final ImmutableMap<DataAccessLevel, ImmutableSet<Short>>
      DATA_ACCESS_LEVEL_TO_VISIBLE_VALUES =
          ImmutableMap.<DataAccessLevel, ImmutableSet<Short>>builder()
              .put(DataAccessLevel.REGISTERED, REGISTERED_ONLY)
              .put(DataAccessLevel.PROTECTED, REGISTERED_AND_PROTECTED)
              .build();

  private Provider<DbUser> userProvider;
  private Provider<WorkbenchConfig> configProvider;
  private FireCloudService fireCloudService;
  private CdrVersionDao cdrVersionDao;

  @Autowired
  public CdrVersionService(
      Provider<DbUser> userProvider,
      Provider<WorkbenchConfig> configProvider,
      FireCloudService fireCloudService,
      CdrVersionDao cdrVersionDao) {
    this.userProvider = userProvider;
    this.configProvider = configProvider;
    this.fireCloudService = fireCloudService;
    this.cdrVersionDao = cdrVersionDao;
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
    // TODO: map data access level to authorization domain here (RW-943)
    String authorizationDomain = configProvider.get().firecloud.registeredDomainName;
    if (!fireCloudService.isUserMemberOfGroup(userProvider.get().getUsername(), authorizationDomain)) {
      throw new ForbiddenException(
          "Requester is not a member of " + authorizationDomain + ", cannot access CDR");
    }
    CdrVersionContext.setCdrVersionNoCheckAuthDomain(version);
  }

  /**
   * Sets the active CDR version, after checking to ensure that the requester is in the appropriate
   * authorization domain. If you have already retrieved a workspace for the requester (and thus
   * implicitly know they are in the authorization domain for its CDR version), you can instead just
   * call {@link CdrVersionContext#setCdrVersionNoCheckAuthDomain(DbCdrVersion)} directly.
   *
   * @param cdrVersionId
   */
  public void setCdrVersion(Long cdrVersionId) {
    this.setCdrVersion(cdrVersionDao.findOne(cdrVersionId));
  }

  /**
   * Sets the active CDR version, after checking to ensure that the requester is in the appropriate
   * authorization domain. If you have already retrieved a workspace for the requester (and thus
   * implicitly know they are in the authorization domain for its CDR version), you can instead just
   * call {@link CdrVersionContext#setCdrVersionNoCheckAuthDomain(DbCdrVersion)} directly.
   *
   * @param cdrVersionId
   */
  public DbCdrVersion findAndSetCdrVersion(Long cdrVersionId) {
    DbCdrVersion dbCdrVersion = cdrVersionDao.findOne(cdrVersionId);
    this.setCdrVersion(dbCdrVersion);
    return dbCdrVersion;
  }

  /**
   * Retrieve all the CDR versions visible to users with the specified data access level. When
   * {@link DataAccessLevel#PROTECTED} is provided, CDR versions for both {@link
   * DataAccessLevel#REGISTERED} and {@link DataAccessLevel#PROTECTED} are returned. Note: this
   * relies on {@link DbUser#dataAccessLevel} accurately reflecting that the user is in the
   * authorization domain that has access to the CDR version BigQuery data sets with the matching
   * {@link DataAccessLevel} values.
   *
   * @param dataAccessLevel the data access level of the user
   * @return a list of {@link DbCdrVersion} in descending timestamp, data access level order.
   */
  public List<DbCdrVersion> findAuthorizedCdrVersions(DataAccessLevel dataAccessLevel) {
    ImmutableSet<Short> visibleValues = DATA_ACCESS_LEVEL_TO_VISIBLE_VALUES.get(dataAccessLevel);
    if (visibleValues == null) {
      return ImmutableList.of();
    }
    return cdrVersionDao.findByDataAccessLevelInOrderByCreationTimeDescDataAccessLevelDesc(
        visibleValues);
  }
}
