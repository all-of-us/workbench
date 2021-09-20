package org.pmiops.workbench.rdr;

import com.google.common.annotations.VisibleForTesting;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.db.dao.RdrExportDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.VerifiedInstitutionalAffiliationDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbDemographicSurvey;
import org.pmiops.workbench.db.model.DbRdrExport;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.RdrEntityEnums;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.model.InstitutionalRole;
import org.pmiops.workbench.model.RdrEntity;
import org.pmiops.workbench.model.SpecificPopulationEnum;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.rdr.api.RdrApi;
import org.pmiops.workbench.rdr.model.RdrResearcher;
import org.pmiops.workbench.rdr.model.RdrWorkspace;
import org.pmiops.workbench.rdr.model.RdrWorkspaceUser;
import org.pmiops.workbench.rdr.model.ResearcherVerifiedInstitutionalAffiliation;
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

  private Clock clock;
  private Provider<RdrApi> rdrApiProvider;
  private RdrExportDao rdrExportDao;
  private WorkspaceDao workspaceDao;
  private UserDao userDao;

  private InstitutionService institutionService;
  private WorkspaceService workspaceService;
  private final VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao;
  private final RdrMapper rdrMapper;
  private static final Logger log = Logger.getLogger(RdrExportService.class.getName());
  ZoneOffset offset = OffsetDateTime.now().getOffset();

  @Autowired
  public RdrExportServiceImpl(
      Clock clock,
      Provider<RdrApi> rdrApiProvider,
      RdrExportDao rdrExportDao,
      RdrMapper rdrMapper,
      WorkspaceDao workspaceDao,
      InstitutionService institutionService,
      WorkspaceService workspaceService,
      UserDao userDao,
      VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao) {
    this.clock = clock;
    this.rdrExportDao = rdrExportDao;
    this.rdrApiProvider = rdrApiProvider;
    this.rdrMapper = rdrMapper;
    this.workspaceDao = workspaceDao;
    this.institutionService = institutionService;
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
    } catch (ApiException ex) {
      log.severe(
          String.format(
              "Error while sending workspace data to RDR for workspace IDs: %s", workspaceIds));
    }
    log.info(
        String.format("successfully exported workspace data for workspace IDs: %s", workspaceIds));
  }

  // Convert workbench DBUser to RDR Model
  private RdrResearcher toRdrResearcher(DbUser dbUser) {
    RdrResearcher researcher = new RdrResearcher();
    researcher.setUserId((int) dbUser.getUserId());
    // RDR will start accepting null creation Time and later once workbench works on story
    // https://precisionmedicineinitiative.atlassian.net/browse/RW-3741 RDR will create API to
    // backfill data
    if (null != researcher.getCreationTime()) {
      researcher.setCreationTime(dbUser.getCreationTime().toLocalDateTime().atOffset(offset));
    }
    researcher.setModifiedTime(dbUser.getLastModifiedTime().toLocalDateTime().atOffset(offset));

    researcher.setGivenName(dbUser.getGivenName());
    researcher.setFamilyName(dbUser.getFamilyName());
    researcher.setDegrees(
        dbUser.getDegreesEnum().stream()
            .map(RdrExportEnums::degreeToRdrDegree)
            .collect(Collectors.toList()));

    if (dbUser.getContactEmail() != null) {
      researcher.setEmail(dbUser.getContactEmail());
    }

    if (dbUser.getAddress() != null) {
      researcher.setStreetAddress1(dbUser.getAddress().getStreetAddress1());
      researcher.setStreetAddress2(dbUser.getAddress().getStreetAddress2());
      researcher.setCity(dbUser.getAddress().getCity());
      researcher.setState(dbUser.getAddress().getState());
      researcher.setCountry(dbUser.getAddress().getCountry());
      researcher.setZipCode(dbUser.getAddress().getZipCode());
    }
    DbDemographicSurvey dbDemographicSurvey = dbUser.getDemographicSurvey();
    if (null != dbDemographicSurvey) {
      researcher.setDisability(
          RdrExportEnums.disabilityToRdrDisability(dbDemographicSurvey.getDisabilityEnum()));
      researcher.setEducation(
          RdrExportEnums.educationToRdrEducation(dbDemographicSurvey.getEducationEnum()));
      researcher.setEthnicity(
          Optional.ofNullable(dbDemographicSurvey.getEthnicityEnum())
              .map(RdrExportEnums::ethnicityToRdrEthnicity)
              .orElse(null));

      researcher.setSexAtBirth(
          Optional.ofNullable(
                  dbDemographicSurvey.getSexAtBirthEnum().stream()
                      .map(RdrExportEnums::sexAtBirthToRdrSexAtBirth)
                      .collect(Collectors.toList()))
              .orElse(new ArrayList<>()));
      researcher.setGender(
          Optional.ofNullable(
                  dbDemographicSurvey.getGenderIdentityEnumList().stream()
                      .map(RdrExportEnums::genderToRdrGender)
                      .collect(Collectors.toList()))
              .orElse(new ArrayList<>()));

      researcher.setDisability(
          RdrExportEnums.disabilityToRdrDisability(dbDemographicSurvey.getDisabilityEnum()));

      researcher.setRace(
          Optional.ofNullable(
                  dbDemographicSurvey.getRaceEnum().stream()
                      .map(RdrExportEnums::raceToRdrRace)
                      .collect(Collectors.toList()))
              .orElse(new ArrayList<>()));

      researcher.setLgbtqIdentity(dbDemographicSurvey.getLgbtqIdentity());
      researcher.setIdentifiesAsLgbtq(dbDemographicSurvey.getIdentifiesAsLgbtq());
    }

    // Deprecated old-style institutional affiliations
    // To be removed in RW-4362
    researcher.setAffiliations(Collections.emptyList());

    verifiedInstitutionalAffiliationDao
        .findFirstByUser(dbUser)
        .ifPresent(
            verifiedInstitutionalAffiliation -> {
              final InstitutionalRole roleEnum =
                  verifiedInstitutionalAffiliation.getInstitutionalRoleEnum();
              final String role =
                  (roleEnum == InstitutionalRole.OTHER)
                      ? verifiedInstitutionalAffiliation.getInstitutionalRoleOtherText()
                      : roleEnum.toString();

              researcher.setVerifiedInstitutionalAffiliation(
                  new ResearcherVerifiedInstitutionalAffiliation()
                      .institutionShortName(
                          verifiedInstitutionalAffiliation.getInstitution().getShortName())
                      .institutionDisplayName(
                          verifiedInstitutionalAffiliation.getInstitution().getDisplayName())
                      .institutionalRole(role));
            });
    return researcher;
  }

  private RdrWorkspace toRdrWorkspace(DbWorkspace dbWorkspace) {
    RdrWorkspace rdrWorkspace = rdrMapper.toRdrModel(dbWorkspace);
    setExcludeFromPublicDirectory(dbWorkspace.getCreator(), rdrWorkspace);

    if (dbWorkspace.getSpecificPopulationsEnum().contains(SpecificPopulationEnum.OTHER)) {
      rdrWorkspace.getWorkspaceDemographic().setOthers(dbWorkspace.getOtherPopulationDetails());
    }
    rdrWorkspace.setWorkspaceUsers(new ArrayList<>());
    if (dbWorkspace.getWorkspaceActiveStatusEnum().equals(WorkspaceActiveStatus.ACTIVE)) {
      try {
        // Call Firecloud to get a list of Collaborators
        List<UserRole> collaboratorsMap =
            workspaceService.getFirecloudUserRoles(
                dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName());
        // Since the USERS cannot be deleted from workbench yet, hence sending the the status of
        // COLLABORATOR as ACTIVE
        collaboratorsMap.forEach(
            (userRole) -> {
              RdrWorkspaceUser workspaceUserMap = new RdrWorkspaceUser();
              workspaceUserMap.setUserId(
                  (int) userDao.findUserByUsername(userRole.getEmail()).getUserId());
              workspaceUserMap.setRole(
                  RdrWorkspaceUser.RoleEnum.fromValue(userRole.getRole().toString()));
              workspaceUserMap.setStatus(RdrWorkspaceUser.StatusEnum.ACTIVE);
              rdrWorkspace.addWorkspaceUsersItem(workspaceUserMap);
            });
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
            verifiedInstitutionalAffiliation -> {
              rdrWorkspace.setExcludeFromPublicDirectory(
                  institutionService.validateOperationalUser(
                      verifiedInstitutionalAffiliation.getInstitution()));
            });
  }
}
