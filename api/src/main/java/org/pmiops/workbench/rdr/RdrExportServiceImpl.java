package org.pmiops.workbench.rdr;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import main.java.org.pmiops.workbench.db.model.RdrEntityEnums;
import org.pmiops.workbench.db.dao.RdrExportDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbRdrExport;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceACL;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceAccessEntry;
import org.pmiops.workbench.model.RdrEntity;
import org.pmiops.workbench.rdr.api.RdrApi;
import org.pmiops.workbench.rdr.model.RdrResearcher;
import org.pmiops.workbench.rdr.model.RdrWorkspace;
import org.pmiops.workbench.rdr.model.RdrWorkspaceUser;
import org.pmiops.workbench.rdr.model.ResearcherAffiliation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * The purpose of this service is to export user/workspace data from workbench to Research Directory
 *
 * @author nsaxena
 */
@Service
public class RdrExportServiceImpl implements RdrExportService {

  ZoneOffset offset = OffsetDateTime.now().getOffset();

  private Clock clock;

  private Provider<RdrApi> RdrApiProvider;

  private RdrExportDao rdrExportDao;
  private WorkspaceDao workspaceDao;
  private FireCloudService fireCloudService;
  private UserDao userDao;
  private static final Logger log = Logger.getLogger(RdrExportService.class.getName());

  @Autowired
  public RdrExportServiceImpl(
      Clock clock,
      FireCloudService fireCloudService,
      Provider<RdrApi> RdrApiProvider,
      RdrExportDao rdrExportDao,
      WorkspaceDao workspaceDao,
      UserDao userDao) {
    this.clock = clock;
    this.fireCloudService = fireCloudService;
    this.rdrExportDao = rdrExportDao;
    this.RdrApiProvider = RdrApiProvider;
    this.workspaceDao = workspaceDao;
    this.userDao = userDao;
  }

  /**
   * Retrieve the list of all users ids that are either a) not in rdr_Export table or b) have
   * last_modified_time (user table) > export_time (rdr_export table)
   *
   * @return list of User Ids
   */
  @Override
  public List<Long> findAllUserIdsToExport() {
    List<Long> userIdList = new ArrayList<Long>();
    try {
      userIdList =
          rdrExportDao.findDbUserIdsToExport().stream()
              .map(user -> user.longValue())
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
    List<Long> workspaceListToExport = new ArrayList<Long>();
    try {
      workspaceListToExport =
          rdrExportDao.findDbWorkspaceIdsToExport().stream()
              .map(workspaceId -> workspaceId.longValue())
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
    List<RdrResearcher> RdrResearchersList;
    try {
      RdrResearchersList =
          userIds.stream()
              .map(userId -> toRdrResearcher(userDao.findUserByUserId(userId)))
              .collect(Collectors.toList());
      // TODO: REMOVE DEBUGGER
      RdrApiProvider.get().getApiClient().setDebugging(true);
      RdrApiProvider.get().exportResearcheres(RdrResearchersList);

      updateRDRExport(RdrEntity.USER, userIds);
    } catch (ApiException ex) {
      log.severe("Error while sending researcher data to RDR");
    }
  }

  /**
   * Call the Rdr API to send researcher data and if successful store all the ids in rdr_export
   * table with current date as the lastExport date
   *
   * @param workspaceIds
   */
  @Override
  public void exportWorkspaces(List<Long> workspaceIds) {
    List<RdrWorkspace> RdrWorkspacesList;
    try {
      RdrWorkspacesList =
          workspaceIds.stream()
              .map(
                  workspaceId ->
                      toRdrWorkspace(workspaceDao.findDbWorkspaceByWorkspaceId(workspaceId)))
              .collect(Collectors.toList());
      RdrApiProvider.get().getApiClient().setDebugging(true);
      RdrApiProvider.get().exportWorkspaces(RdrWorkspacesList);
      updateRDRExport(RdrEntity.WORKSPACE, workspaceIds);
    } catch (ApiException ex) {
      log.severe("Error while sending workspace data to RDR");
    }
  }

  // Convert workbench DBUser to RDR Model
  private RdrResearcher toRdrResearcher(DbUser workbenchUser) {
    RdrResearcher researcher = new RdrResearcher();
    researcher.setUserId((int) workbenchUser.getUserId());
    researcher.setCreationTime(workbenchUser.getCreationTime().toLocalDateTime().atOffset(offset));
    if (workbenchUser.getLastModifiedTime() != null)
      researcher.setModifiedTime(
          workbenchUser.getLastModifiedTime().toLocalDateTime().atOffset(offset));
    researcher.setGivenName(workbenchUser.getGivenName());
    researcher.setFamilyName(workbenchUser.getFamilyName());
    if (workbenchUser.getAddress() != null) {
      researcher.setStreetAddress1(workbenchUser.getAddress().getStreetAddress1());
      researcher.setStreetAddress2(workbenchUser.getAddress().getStreetAddress2());
      researcher.setCity(workbenchUser.getAddress().getCity());
      researcher.setState(workbenchUser.getAddress().getState());
      researcher.setCountry(workbenchUser.getAddress().getCountry());
      researcher.setZipCode(workbenchUser.getAddress().getZipCode());
    }
    // TODO Gender and Race will change in RDR API from string to array

    // researcher.setGender(workbenchUser.getDemographicSurvey().getGenderEnum());
    // researcher.setRace(workbenchUser.getDemographicSurvey().getRace());
    researcher.setAffiliations(
        workbenchUser.getInstitutionalAffiliations().stream()
            .map(
                inst -> {
                  return new ResearcherAffiliation()
                      .institution(inst.getInstitution())
                      .role(inst.getRole());
                })
            .collect(Collectors.toList()));
    return researcher;
  }

  private RdrWorkspace toRdrWorkspace(DbWorkspace workbenchWorkspace) {
    RdrWorkspace RdrWorkspace = new RdrWorkspace();
    RdrWorkspace.setWorkspaceId((int) workbenchWorkspace.getWorkspaceId());
    RdrWorkspace.setName(workbenchWorkspace.getName());

    RdrWorkspace.setCreationTime(
        workbenchWorkspace.getCreationTime().toLocalDateTime().atOffset(offset));
    RdrWorkspace.setModifiedTime(
        workbenchWorkspace.getLastModifiedTime().toLocalDateTime().atOffset(offset));
    RdrWorkspace.setStatus(
        org.pmiops.workbench.rdr.model.RdrWorkspace.StatusEnum.fromValue(
            workbenchWorkspace.getWorkspaceActiveStatusEnum().toString()));
    RdrWorkspace.setExcludeFromPublicDirectory(false);
    RdrWorkspace.setDiseaseFocusedResearch(workbenchWorkspace.getDiseaseFocusedResearch());
    RdrWorkspace.setDiseaseFocusedResearchName(workbenchWorkspace.getDiseaseOfFocus());
    RdrWorkspace.setOtherPurpose(workbenchWorkspace.getOtherPurpose());
    RdrWorkspace.setOtherPurposeDetails(workbenchWorkspace.getOtherPurposeDetails());
    RdrWorkspace.setMethodsDevelopment(workbenchWorkspace.getMethodsDevelopment());
    RdrWorkspace.setControlSet(workbenchWorkspace.getControlSet());
    RdrWorkspace.setAncestry(workbenchWorkspace.getAncestry());
    RdrWorkspace.setSocialBehavioral(workbenchWorkspace.getSocialBehavioral());
    RdrWorkspace.setPopulationHealth(workbenchWorkspace.getPopulationHealth());
    RdrWorkspace.setDrugDevelopment(workbenchWorkspace.getDrugDevelopment());
    RdrWorkspace.setCommercialPurpose(workbenchWorkspace.getCommercialPurpose());
    RdrWorkspace.setEducational(workbenchWorkspace.getEducational());

    // Call Firecloud to get a list of Collaborators
    FirecloudWorkspaceACL firecloudResponse =
        fireCloudService.getWorkspaceAcl(
            workbenchWorkspace.getWorkspaceNamespace(), workbenchWorkspace.getFirecloudName());
    Map<String, FirecloudWorkspaceAccessEntry> aclMap = firecloudResponse.getAcl();
    aclMap.forEach(
        (email, access) -> {
          RdrWorkspaceUser workspaceUderMap = new RdrWorkspaceUser();
          workspaceUderMap.setUserId((int) userDao.findUserByUsername(email).getUserId());
          workspaceUderMap.setRole(RdrWorkspaceUser.RoleEnum.fromValue(access.getAccessLevel()));
          RdrWorkspace.addWorkspaceUsersItem(workspaceUderMap);
        });

    return RdrWorkspace;
  }

  /**
   * For Each entityType and entity id update lastExportDate to current date time if it exist in
   * rdr_export table else add a new entry
   *
   * @param entity
   * @param idList
   */
  private void updateRDRExport(RdrEntity entity, List<Long> idList) {
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
    rdrExportDao.save(exportList);
  }
}
