package org.pmiops.workbench.compliancetraining;

import java.sql.Timestamp;
import java.util.Map;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.absorb.AbsorbService;
import org.pmiops.workbench.absorb.ApiException;
import org.pmiops.workbench.absorb.Credentials;
import org.pmiops.workbench.access.AccessModuleService;
import org.pmiops.workbench.access.AccessSyncService;
import org.pmiops.workbench.actionaudit.Agent;
import org.pmiops.workbench.db.dao.ComplianceTrainingVerificationDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbAccessModule;
import org.pmiops.workbench.db.model.DbComplianceTrainingVerification;
import org.pmiops.workbench.db.model.DbComplianceTrainingVerification.DbComplianceTrainingVerificationSystem;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserAccessModule;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ComplianceTrainingServiceImpl implements ComplianceTrainingService {
  public static final String rtTrainingCourseId = "9ad49c70-3b72-4789-8282-5794efcd4ce1";
  public static final String ctTrainingCourseId = "3765dc64-cc64-4efa-bfc0-9a4dc2e9d09d";

  private static final Logger log = Logger.getLogger(ComplianceTrainingServiceImpl.class.getName());
  private final AccessModuleService accessModuleService;
  private final AccessSyncService accessSyncService;
  private final Provider<DbUser> userProvider;
  private final UserService userService;
  private final ComplianceTrainingVerificationDao complianceTrainingVerificationDao;
  private final AbsorbService absorbService;

  @Autowired
  public ComplianceTrainingServiceImpl(
      AccessModuleService accessModuleService,
      AccessSyncService accessSyncService,
      Provider<DbUser> userProvider,
      UserService userService,
      ComplianceTrainingVerificationDao complianceTrainingVerificationDao,
      AbsorbService absorbService) {
    this.accessModuleService = accessModuleService;
    this.accessSyncService = accessSyncService;
    this.userProvider = userProvider;
    this.userService = userService;
    this.complianceTrainingVerificationDao = complianceTrainingVerificationDao;
    this.absorbService = absorbService;
  }

  /**
   * Syncs the current user's training status from the relevant LMS (Learning Management System).
   */
  public DbUser syncComplianceTrainingStatus() throws NotFoundException, ApiException {
    DbUser user = userProvider.get();

    log.info(String.format("Syncing compliance training status for user %s", user.getUsername()));

    // Skip sync for service account user rows.
    if (userService.isServiceAccount(user)) {
      return user;
    }
    return syncComplianceTrainingStatusAbsorb(user, Agent.asUser(user));
  }

  @Transactional
  public DbUser syncComplianceTrainingStatusAbsorb(DbUser dbUser, Agent agent) throws ApiException {
    /*
    When debugging or manually testing Absorb, it can be helpful to use the Absorb Admin UI.
    You can log into https://aoudev.myabsorb.com/admin/dashboard using our Absorb API credentials.
    From there, you can view the current state of Absorb or manually bypass courses.
     */

    log.info("Using Absorb to sync compliance training status.");

    Credentials credentials = absorbService.fetchCredentials(dbUser.getUsername());

    if (!absorbService.userHasLoggedIntoAbsorb(credentials)) {
      return dbUser;
    }

    Map<String, DbAccessModule.DbAccessModuleName> courseToAccessModuleMap =
        Map.of(
            rtTrainingCourseId,
            DbAccessModule.DbAccessModuleName.RT_COMPLIANCE_TRAINING,
            ctTrainingCourseId,
            DbAccessModule.DbAccessModuleName.CT_COMPLIANCE_TRAINING);

    var enrollments = absorbService.getActiveEnrollmentsForUser(credentials);

    for (Map.Entry<String, DbAccessModule.DbAccessModuleName> entry :
        courseToAccessModuleMap.entrySet()) {
      var courseId = entry.getKey();
      var accessModuleName = entry.getValue();

      var maybeEnrollment =
          enrollments.stream().filter(e -> e.courseId.equals(courseId)).findFirst();

      maybeEnrollment.ifPresentOrElse(
          enrollment -> {
            // If the course is incomplete, do not update the user access module
            if (enrollment.completionTime != null) {
              var updatedUserAccessModule =
                  accessModuleService.updateCompletionTime(
                      dbUser, accessModuleName, Timestamp.from(enrollment.completionTime));

              addVerificationSystem(
                  updatedUserAccessModule, DbComplianceTrainingVerificationSystem.ABSORB);
            }
          },
          () -> {
            if (accessModuleName == DbAccessModule.DbAccessModuleName.RT_COMPLIANCE_TRAINING) {
              log.severe(
                  String.format(
                      "User `%s` is not enrolled in RT compliance training. "
                          + "Users are expected to be automatically enrolled in RT training upon logging into Absorb.",
                      dbUser.getUsername()));
              throw new NotFoundException(
                  String.format(
                      "User %s is not enrolled in Absorb course %s",
                      dbUser.getUsername(), courseId));
            }

            // Else: The user is not enrolled in CT training.
            // This is expected behavior. Users are not enrolled in CT training in Absorb until
            // they complete RT training.
          });
    }

    return accessSyncService.updateUserAccessTiers(dbUser, agent);
  }

  private void addVerificationSystem(
      DbUserAccessModule updatedUserAccessModule, DbComplianceTrainingVerificationSystem absorb) {
    var rtVerification = retrieveVerificationOrCreate(updatedUserAccessModule);
    rtVerification.setComplianceTrainingVerificationSystem(absorb);
    complianceTrainingVerificationDao.save(rtVerification);
  }

  private DbComplianceTrainingVerification retrieveVerificationOrCreate(
      DbUserAccessModule userAccessModule) {
    return complianceTrainingVerificationDao
        .getByUserAccessModule(userAccessModule)
        .orElse(new DbComplianceTrainingVerification().setUserAccessModule(userAccessModule));
  }
}
