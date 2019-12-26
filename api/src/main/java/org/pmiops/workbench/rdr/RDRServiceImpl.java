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
import main.java.org.pmiops.workbench.db.model.RDREntityEnums;
import org.pmiops.workbench.db.dao.RDRExportDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbRDRExport;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceACL;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceAccessEntry;
import org.pmiops.workbench.model.RDREntity;
import org.pmiops.workbench.rdr.api.RDRApi;
import org.pmiops.workbench.rdr.model.RDRResearcher;
import org.pmiops.workbench.rdr.model.RDRWorkspace;
import org.pmiops.workbench.rdr.model.RDRWorkspaceUser;
import org.pmiops.workbench.rdr.model.ResearcherAffiliation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RDRServiceImpl implements RDRService {

  ZoneOffset offset = OffsetDateTime.now().getOffset();
  private Clock clock;

  private Provider<RDRApi> rdrApiProvider;

  private RDRExportDao rdrExportDao;
  private WorkspaceDao workspaceDao;
  private final FireCloudService fireCloudService;
  private UserDao userDao;
  private static final Logger log = Logger.getLogger(RDRService.class.getName());

  @Autowired
  public RDRServiceImpl(
      Clock clock,
      FireCloudService fireCloudService,
      Provider<RDRApi> rdrApiProvider,
      RDRExportDao rdrExportDao,
      WorkspaceDao workspaceDao,
      UserDao userDao) {
    this.clock = clock;
    this.fireCloudService = fireCloudService;
    this.rdrExportDao = rdrExportDao;
    this.rdrApiProvider = rdrApiProvider;
    this.workspaceDao = workspaceDao;
    this.userDao = userDao;
  }

  /*
  Return the list of all users ids that are either
   a) not in rdr_Export table or
   b) have last_modified_time (user table) > export_time (rdr_export table)
   */

  @Override
  public List<Long> findAllUserIdsToExport() {
    List<Long> userIdList = new ArrayList<Long>();
    try {
      userIdList =
          rdrExportDao.findDbUserToExport().stream()
              .map(user -> user.longValue())
              .collect(Collectors.toList());
    } catch (Exception ex) {
      log.severe(
          String.format(
              "Error while trying to fetch modified/created user list: %s", ex.getMessage()));
    }
    return userIdList;
  }

  /*
  Return the list of all workspaces that are either
    a) not in rdr_Export table OR
    b) last_modified_time (workspace) > export_time (rdr_export table)
   */
  @Override
  public List<Long> findAllWorkspacesIdsToExport() {
    List<Long> workspaceListToExport = new ArrayList<Long>();
    try {
      workspaceListToExport =
          rdrExportDao.findDbWorkspaceToExport().stream()
              .map(workspaceId -> workspaceId.longValue())
              .collect(Collectors.toList());
    } catch (Exception ex) {
      log.severe(
          String.format(
              "Error while trying to fetch modified/created workspace list: %s", ex.getMessage()));
    }
    return workspaceListToExport;
  }

  /* Call the RDR API to send researcher data and if successful store all the ids in
  rdr_export table with current date as the lastExport date*/
  @Override
  public void sendUser(List<Long> usersToExport) {
    List<RDRResearcher> rdrResearchersList;
    try {
      rdrResearchersList =
          usersToExport.stream()
              .map(user -> toRDRResearcher(userDao.findUserByUserId(user)))
              .collect(Collectors.toList());
      // TODO: REMOVE DEBUGGER
      rdrApiProvider.get().getApiClient().setDebugging(true);
      rdrApiProvider.get().sendResearcher(rdrResearchersList);

      List<Integer> successfulExportedIds =
          rdrResearchersList.stream()
              .map(researcher -> researcher.getUserId())
              .collect(Collectors.toList());
      updateRDRExport(RDREntity.USER, successfulExportedIds);
    } catch (ApiException ex) {
      log.severe("Error while sending researcher data to RDR");
    }
  }

  /*
   * Call the RDR API to send workspace data and if successful store all the ids in rdr_export table
   * with current date as the lastExport date
   */
  @Override
  public void sendWorkspace(List<Long> workspacesToExport) {
    List<RDRWorkspace> rdrWorkspacesList;
    try {
      rdrWorkspacesList =
          workspacesToExport.stream()
              .map(
                  workspace -> toRDRWorkspace(workspaceDao.findDbWorkspaceByWorkspaceId(workspace)))
              .collect(Collectors.toList());
      rdrApiProvider.get().getApiClient().setDebugging(true);
      rdrApiProvider.get().sendWorkspace(rdrWorkspacesList);
      List<Integer> successfulExportedIds =
          rdrWorkspacesList.stream()
              .map(researcher -> researcher.getWorkspaceId())
              .collect(Collectors.toList());
      updateRDRExport(RDREntity.WORKSPACE, successfulExportedIds);
    } catch (ApiException ex) {
      log.severe("Error while sending workspace data to RDR");
    }
  }

  // Convert workbench DBUser to RDR Model
  private RDRResearcher toRDRResearcher(DbUser workbenchUser) {
    RDRResearcher researcher = new RDRResearcher();
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

  private RDRWorkspace toRDRWorkspace(DbWorkspace workbenchWorkspace) {
    RDRWorkspace rdrWorkspace = new RDRWorkspace();
    rdrWorkspace.setWorkspaceId((int) workbenchWorkspace.getWorkspaceId());
    rdrWorkspace.setName(workbenchWorkspace.getName());

    rdrWorkspace.setCreationTime(
        workbenchWorkspace.getCreationTime().toLocalDateTime().atOffset(offset));
    rdrWorkspace.setModifiedTime(
        workbenchWorkspace.getLastModifiedTime().toLocalDateTime().atOffset(offset));
    rdrWorkspace.setStatus(
        RDRWorkspace.StatusEnum.fromValue(
            workbenchWorkspace.getWorkspaceActiveStatusEnum().toString()));
    rdrWorkspace.setExcludeFromPublicDirectory(false);
    rdrWorkspace.setDiseaseFocusedResearch(workbenchWorkspace.getDiseaseFocusedResearch());
    rdrWorkspace.setDiseaseFocusedResearchName(workbenchWorkspace.getDiseaseOfFocus());
    rdrWorkspace.setOtherPurpose(workbenchWorkspace.getOtherPurpose());
    rdrWorkspace.setOtherPurposeDetails(workbenchWorkspace.getOtherPurposeDetails());
    rdrWorkspace.setMethodsDevelopment(workbenchWorkspace.getMethodsDevelopment());
    rdrWorkspace.setControlSet(workbenchWorkspace.getControlSet());
    rdrWorkspace.setAncestry(workbenchWorkspace.getAncestry());
    rdrWorkspace.setSocialBehavioral(workbenchWorkspace.getSocialBehavioral());
    rdrWorkspace.setPopulationHealth(workbenchWorkspace.getPopulationHealth());
    rdrWorkspace.setDrugDevelopment(workbenchWorkspace.getDrugDevelopment());
    rdrWorkspace.setCommercialPurpose(workbenchWorkspace.getCommercialPurpose());
    rdrWorkspace.setEducational(workbenchWorkspace.getEducational());

    // Call Firecloud to get a list of Collaborators
    FirecloudWorkspaceACL firecloudResponse =
        fireCloudService.getWorkspaceAcl(
            workbenchWorkspace.getWorkspaceNamespace(), workbenchWorkspace.getFirecloudName());
    Map<String, FirecloudWorkspaceAccessEntry> aclMap = firecloudResponse.getAcl();
    aclMap.forEach(
        (email, access) -> {
          RDRWorkspaceUser workspaceUderMap = new RDRWorkspaceUser();
          workspaceUderMap.setUserId((int) userDao.findUserByEmail(email).getUserId());
          workspaceUderMap.setRole(RDRWorkspaceUser.RoleEnum.fromValue(access.getAccessLevel()));
          rdrWorkspace.addWorkspaceUsersItem(workspaceUderMap);
        });

    return rdrWorkspace;
  }

  // For Each id passed check if entity and id is already in rdr_export if yes update the
  // lastExportDate otherwise add a new entry to rdr_export table
  private void updateRDRExport(RDREntity entity, List<Integer> idList) {
    Timestamp now = new Timestamp(clock.instant().toEpochMilli());

    List<DbRDRExport> exportList =
        idList.stream()
            .map(
                id -> {
                  DbRDRExport rd =
                      rdrExportDao.findByEntityAndId(RDREntityEnums.entityToStorage(entity), id);
                  // If Entry doesn't exist in rdr_export create an object else just update the
                  // export Date
                  // to right now
                  if (rd == null) {
                    rd = new DbRDRExport();
                    rd.setEntityEnum(entity);
                    rd.setId(id);
                  }
                  rd.setExportDate(now);
                  return rd;
                })
            .collect(Collectors.toList());
    rdrExportDao.save(exportList);
  }
}
