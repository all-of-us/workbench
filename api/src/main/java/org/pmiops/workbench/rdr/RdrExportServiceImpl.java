package org.pmiops.workbench.rdr;

import com.google.common.annotations.VisibleForTesting;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Provider;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.RdrExportDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.VerifiedInstitutionalAffiliationDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbRdrExport;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.RdrEntityEnums;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.model.RdrEntity;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.rdr.api.RdrApi;
import org.pmiops.workbench.rdr.model.RdrResearcher;
import org.pmiops.workbench.rdr.model.RdrWorkspace;
import org.pmiops.workbench.rdr.model.RdrWorkspaceUser;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * The purpose of this service is to export user/workspace data from workbench to Research Directory
 *
 * @author nsaxena
 */
@Service
public class RdrExportServiceImpl implements RdrExportService {

  private final Clock clock;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final Provider<RdrApi> rdrApiProvider;
  private final RdrExportDao rdrExportDao;
  private final WorkspaceDao workspaceDao;
  private final UserDao userDao;

  private final InstitutionService institutionService;
  private final AccessTierService accessTierService;
  private final WorkspaceService workspaceService;
  private final VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao;
  private final RdrMapper rdrMapper;
  private static final Logger log = Logger.getLogger(RdrExportService.class.getName());

  @Autowired
  public RdrExportServiceImpl(
      Clock clock,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      Provider<RdrApi> rdrApiProvider,
      RdrExportDao rdrExportDao,
      RdrMapper rdrMapper,
      WorkspaceDao workspaceDao,
      InstitutionService institutionService,
      AccessTierService accessTierService,
      WorkspaceService workspaceService,
      UserDao userDao,
      VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao) {
    this.clock = clock;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.rdrExportDao = rdrExportDao;
    this.rdrApiProvider = rdrApiProvider;
    this.rdrMapper = rdrMapper;
    this.workspaceDao = workspaceDao;
    this.institutionService = institutionService;
    this.accessTierService = accessTierService;
    this.workspaceService = workspaceService;
    this.userDao = userDao;
    this.verifiedInstitutionalAffiliationDao = verifiedInstitutionalAffiliationDao;
  }

  private List<String> excludedExportUserEmails() {
    return workbenchConfigProvider.get().auth.serviceAccountApiUsers;
  }

  @Override
  public List<Long> findUnchangedEntitiesForBackfill(RdrEntity entityType) {
    List<BigInteger> ids;
    switch (entityType) {
      case USER:
        ids = rdrExportDao.findAllUnchangedDbUserIds(excludedExportUserEmails());
        break;
      case WORKSPACE:
        ids = rdrExportDao.findAllUnchangedDbWorkspaceIds();
        break;
      default:
        throw new IllegalArgumentException("invalid entityType: " + entityType);
    }
    return ids.stream().map(BigInteger::longValue).collect(Collectors.toList());
  }

  /**
   * Retrieve the list of all users ids that are either a) not in rdr_Export table or b) have
   * last_modified_time (user table) > export_time (rdr_export table)
   *
   * @return list of User Ids
   */
  @Override
  public List<Long> findAllUserIdsToExport() {
    // Don't export service account users; they are lacking basic metadata.
    return rdrExportDao.findDbUserIdsToExport(excludedExportUserEmails()).stream()
        .map(BigInteger::longValue)
        .collect(Collectors.toList());
  }

  /**
   * Retrieve the list of all workspace ids that are either a) not in rdr_Export table or b) have
   * last_modified_time (workspace table) > export_time (rdr_export table)
   *
   * @return list of Workspace Ids
   */
  @Override
  public List<Long> findAllWorkspacesIdsToExport() {
    return rdrExportDao.findDbWorkspaceIdsToExport().stream()
        .map(BigInteger::longValue)
        .collect(Collectors.toList());
  }

  /**
   * Call the Rdr API to send researcher data and if successful store all the ids in rdr_export
   * table with current date as the lastExport date
   *
   * @param userIds
   * @param backfill
   */
  @Override
  public void exportUsers(List<Long> userIds, boolean backfill) {
    List<RdrResearcher> rdrResearchersList =
        userIds.stream().map(this::toRdrResearcher).collect(Collectors.toList());

    try {
      rdrApiProvider.get().exportResearchers(rdrResearchersList, backfill);
    } catch (ApiException ex) {
      log.severe(
          String.format(
              "Error while sending researcher data to RDR for user IDs [%s]: %s",
              userIds, ex.getResponseBody()));
      throw new ServerErrorException(ex);
    }

    if (!backfill) {
      updateDbRdrExport(RdrEntity.USER, userIds);
    }
    log.info(String.format("successfully exported researcher data for user IDs: %s", userIds));
  }

  /**
   * Call the Rdr API to send researcher data and if successful store all the ids in rdr_export
   * table with current date as the lastExport date
   *
   * @param workspaceIds
   * @param backfill
   */
  @Override
  public void exportWorkspaces(List<Long> workspaceIds, boolean backfill) {
    List<RdrWorkspace> rdrWorkspacesList;
    try {
      // toRdrWorkspace may fail and will return null, skip failures and continue.
      rdrWorkspacesList =
          workspaceIds.stream()
              .map(
                  workspaceId ->
                      toRdrWorkspace(workspaceDao.findDbWorkspaceByWorkspaceId(workspaceId)))
              .filter(Objects::nonNull)
              .collect(Collectors.toList());
      if (!rdrWorkspacesList.isEmpty()) {
        rdrApiProvider.get().exportWorkspaces(rdrWorkspacesList, backfill);

        List<Long> workspaceIdsToUpload =
            rdrWorkspacesList.stream()
                .map(r -> Long.valueOf(r.getWorkspaceId()))
                .collect(Collectors.toList());

        // Skip the RDR export table updates on backfills. A normal export may trigger manual review
        // from the RDR, where-as a backfill does not. Therefore, even if the RDR has the latest
        // data already from a backfill, we'd still want to resend any normal modifications, if any,
        // in order to trigger this review process.
        if (!backfill) {
          updateDbRdrExport(RdrEntity.WORKSPACE, workspaceIdsToUpload);
        }
        log.info(
            String.format(
                "Successfully exported workspace count: %d, total count %d",
                workspaceIdsToUpload.size(), workspaceIds.size()));
        log.info(
            String.format(
                "successfully exported workspace data for workspace IDs: %s",
                workspaceIdsToUpload));
      }
    } catch (ApiException ex) {
      log.severe(
          String.format(
              "Error while sending workspace data to RDR for workspace IDs: %s", workspaceIds));
      throw new ServerErrorException(ex);
    }
  }

  private RdrResearcher toRdrResearcher(long userId) {
    DbUser dbUser = userDao.findUserByUserId(userId);
    return rdrMapper.toRdrResearcher(
        dbUser,
        accessTierService.getAccessTiersForUser(dbUser),
        verifiedInstitutionalAffiliationDao.findFirstByUser(dbUser).orElse(null));
  }

  @Nullable
  private RdrWorkspace toRdrWorkspace(DbWorkspace dbWorkspace) {
    RdrWorkspace rdrWorkspace = rdrMapper.toRdrWorkspace(dbWorkspace);
    setExcludeFromPublicDirectory(dbWorkspace.getCreator(), rdrWorkspace);

    rdrWorkspace.setWorkspaceUsers(new ArrayList<>());
    if (WorkspaceActiveStatus.ACTIVE.equals(dbWorkspace.getWorkspaceActiveStatusEnum())) {
      try {
        // Call Firecloud to get a list of Collaborators
        List<UserRole> collaborators =
            workspaceService.getFirecloudUserRoles(
                dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName());
        rdrWorkspace.setWorkspaceUsers(
            collaborators.stream()
                .map(
                    (userRole) ->
                        new RdrWorkspaceUser()
                            .userId(
                                (int) userDao.findUserByUsername(userRole.getEmail()).getUserId())
                            .role(
                                RdrWorkspaceUser.RoleEnum.fromValue(userRole.getRole().toString()))
                            .status(RdrWorkspaceUser.StatusEnum.ACTIVE))
                .collect(Collectors.toList()));
      } catch (Exception ex) {
        log.warning(
            String.format(
                "Exception while retrieving workspace collaborators for workspace id %s, skipping"
                    + " this workspace for RDR Export",
                rdrWorkspace.getWorkspaceId()));
        return null;
      }
    }
    return rdrWorkspace;
  }

  /**
   * For Each entityType and entity id update lastExportDate to current date time if it exist in
   * rdr_export table else add a new entry
   *
   * @param entity
   * @param idList
   */
  @Override
  @VisibleForTesting
  public void updateDbRdrExport(RdrEntity entity, List<Long> idList) {
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());

    List<DbRdrExport> exportList =
        idList.stream()
            .map(
                id -> {
                  DbRdrExport rd =
                      rdrExportDao.findByEntityTypeAndEntityId(
                          RdrEntityEnums.entityToStorage(entity), id);
                  // If Entry doesn't exist in rdr_export create an object else just update the
                  // export Date
                  // to right now
                  if (rd == null) {
                    rd = new DbRdrExport();
                    rd.setEntityTypeEnum(entity);
                    rd.setEntityId(id);
                  }
                  rd.setLastExportDate(now);
                  return rd;
                })
            .collect(Collectors.toList());
    rdrExportDao.saveAll(exportList);
  }

  /**
   * Delete the workspace entries from rdr_export Table to make them eligible for next export cron
   * job
   */
  @Override
  public void deleteRdrExportEntries(RdrEntity entityType, List<Long> ids) {
    ids.forEach(
        id ->
            rdrExportDao.deleteDbRdrExportsByEntityTypeAndEntityId(
                RdrEntityEnums.entityToStorage(entityType), id));
  }

  /**
   * Set excludeFromPublicDirectory to true if the workspace creator is an operational user i.e has
   * Institution as All of Us Program operational Use
   *
   * @param creatorUser
   * @param rdrWorkspace
   */
  void setExcludeFromPublicDirectory(DbUser creatorUser, RdrWorkspace rdrWorkspace) {
    rdrWorkspace.setExcludeFromPublicDirectory(false);
    verifiedInstitutionalAffiliationDao
        .findFirstByUser(creatorUser)
        .ifPresent(
            verifiedInstitutionalAffiliation ->
                rdrWorkspace.setExcludeFromPublicDirectory(
                    institutionService.validateOperationalUser(
                        verifiedInstitutionalAffiliation.getInstitution())));
  }
}
