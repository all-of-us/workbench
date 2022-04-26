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
import javax.inject.Provider;
import org.pmiops.workbench.access.AccessTierService;
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

  /**
   * Retrieve the list of all users ids that are either a) not in rdr_Export table or b) have
   * last_modified_time (user table) > export_time (rdr_export table)
   *
   * @return list of User Ids
   */
  @Override
  public List<Long> findAllUserIdsToExport() {
    List<Long> userIdList = new ArrayList<>();
    try {
      userIdList =
          rdrExportDao.findDbUserIdsToExport().stream()
              .map(BigInteger::longValue)
              .collect(Collectors.toList());
    } catch (Exception ex) {
      log.severe(
          String.format(
              "Error while trying to fetch modified/created user list: %s", ex.getMessage()));
    }
    return userIdList;
  }

  /**
   * Retrieve the list of all workspace ids that are either a) not in rdr_Export table or b) have
   * last_modified_time (workspace table) > export_time (rdr_export table)
   *
   * @return list of Workspace Ids
   */
  @Override
  public List<Long> findAllWorkspacesIdsToExport() {
    List<Long> workspaceListToExport = new ArrayList<>();
    try {
      workspaceListToExport =
          rdrExportDao.findDbWorkspaceIdsToExport().stream()
              .map(BigInteger::longValue)
              .collect(Collectors.toList());
    } catch (Exception ex) {
      log.severe(
          String.format(
              "Error while trying to fetch modified/created workspace list: %s", ex.getMessage()));
    }
    return workspaceListToExport;
  }

  /**
   * Call the Rdr API to send researcher data and if successful store all the ids in rdr_export
   * table with current date as the lastExport date
   *
   * @param userIds
   */
  @Override
  public void exportUsers(List<Long> userIds) {
    List<RdrResearcher> rdrResearchersList;
    try {
      rdrResearchersList =
          userIds.stream()
              .map(userId -> toRdrResearcher(userDao.findUserByUserId(userId)))
              .collect(Collectors.toList());
      rdrApiProvider.get().exportResearchers(rdrResearchersList);

      updateDbRdrExport(RdrEntity.USER, userIds);
      log.info(String.format("successfully exported researcher data for user IDs: %s", userIds));
    } catch (ApiException ex) {
      log.severe(
          String.format(
              "Error while sending researcher data to RDR for user IDs [%s]: %s",
              userIds, ex.getResponseBody()));
      throw new ServerErrorException(ex);
    }
  }

  /**
   * Call the Rdr API to send researcher data and if successful store all the ids in rdr_export
   * table with current date as the lastExport date
   *
   * @param workspaceIds
   */
  @Override
  public void exportWorkspaces(List<Long> workspaceIds, boolean backfill) {
    List<RdrWorkspace> rdrWorkspacesList;
    try {
      rdrWorkspacesList =
          workspaceIds.stream()
              .map(
                  workspaceId ->
                      toRdrWorkspace(workspaceDao.findDbWorkspaceByWorkspaceId(workspaceId)))
              .filter(Objects::nonNull)
              .collect(Collectors.toList());
      if (!rdrWorkspacesList.isEmpty()) {
        rdrApiProvider.get().exportWorkspaces(rdrWorkspacesList, backfill);

        // Skip the RDR export table updates on backfills. A normal export may trigger manual review
        // from the RDR, where-as a backfill does not. Therefore, even if the RDR has the latest
        // data already from a backfill, we'd still want to resend any normal modifications, if any,
        // in order to trigger this review process.
        if (!backfill) {
          updateDbRdrExport(RdrEntity.WORKSPACE, workspaceIds);
        }
      }
      log.info(
          String.format(
              "successfully exported workspace data for workspace IDs: %s", workspaceIds));
    } catch (ApiException ex) {
      log.severe(
          String.format(
              "Error while sending workspace data to RDR for workspace IDs: %s", workspaceIds));
      throw new ServerErrorException(ex);
    }
  }

  private RdrResearcher toRdrResearcher(DbUser dbUser) {
    return rdrMapper.toRdrResearcher(
        dbUser,
        accessTierService.getAccessTiersForUser(dbUser),
        verifiedInstitutionalAffiliationDao.findFirstByUser(dbUser).orElse(null));
  }

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
   *
   * @param workspaceIds
   */
  @Override
  public void deleteWorkspaceExportEntries(List<Long> workspaceIds) {
    workspaceIds.forEach(
        workspaceId -> {
          try {
            rdrExportDao.deleteDbRdrExportsByEntityTypeAndEntityId(
                RdrEntityEnums.entityToStorage(RdrEntity.WORKSPACE), workspaceId);
          } catch (Exception ex) {
            log.severe(
                String.format(
                    "Error while trying to delete workspace entry from rdr_export %s",
                    workspaceId));
          }
        });
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
