package org.pmiops.workbench.exfiltration;

import static org.pmiops.workbench.exfiltration.ExfiltrationConstants.EGRESS_OBJECT_LENGTHS_SERVICE_QUALIFIER;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.pmiops.workbench.actionaudit.bucket.BucketAuditEntry;
import org.pmiops.workbench.actionaudit.bucket.BucketAuditQueryService;
import org.pmiops.workbench.db.dao.EgressEventDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbEgressEvent;
import org.pmiops.workbench.db.model.DbEgressEvent.DbEgressEventStatus;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.iam.IamService;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceResponse;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ObjectNameLengthServiceImpl implements ObjectNameLengthService {

  private static final Logger logger =
      Logger.getLogger(ObjectNameLengthServiceImpl.class.getName());

  private final FireCloudService fireCloudService;
  private final WorkspaceService workspaceService;
  private final BucketAuditQueryService bucketAuditQueryService;
  private final IamService iamService;
  private final UserService userService;
  private final EgressEventDao egressEventDao;
  private final EgressRemediationService egressRemediationService;

  public ObjectNameLengthServiceImpl(
      FireCloudService fireCloudService,
      WorkspaceService workspaceService,
      BucketAuditQueryService bucketAuditQueryService,
      IamService iamService,
      UserService userService,
      EgressEventDao egressEventDao,
      @Qualifier(EGRESS_OBJECT_LENGTHS_SERVICE_QUALIFIER)
          EgressRemediationService egressRemediationService) {
    this.fireCloudService = fireCloudService;
    this.workspaceService = workspaceService;
    this.bucketAuditQueryService = bucketAuditQueryService;
    this.iamService = iamService;
    this.userService = userService;
    this.egressEventDao = egressEventDao;
    this.egressRemediationService = egressRemediationService;
  }

  /**
   * Get file access info from BQ grouped by the pet account, google project and bucket. If the
   * lengths of the filenames is greater than the threshold, an alert is triggered for the user who
   * created the files.
   *
   * <p>Depending on the policy, the {@link EgressRemediationService} takes care of disabling the
   * user, emailing him about the problem, and creating the jira ticket. By disabled the user we
   * make sure that the user doesn't get multiple alerts for the same problem because this method
   * filters only the active users.
   */
  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void calculateObjectNameLength() {

    // Call BQ to get the files created by each owner in the past 24 hours. This will only return
    // the entries with file lengths > THRESHOLD
    List<BucketAuditEntry> fileAccessInfoFromBQ = getFileAccessInfoFromBQ();

    // Get the workspaces for these entries. To only find relevant ones.
    Map<String, DbWorkspace> projectIdToDbWorkspace =
        workspaceService.getWorkspacesByGoogleProject(
            fileAccessInfoFromBQ.stream()
                .map(BucketAuditEntry::getGoogleProjectId)
                .collect(Collectors.toSet()));

    for (BucketAuditEntry bucketAuditEntry : fileAccessInfoFromBQ) {
      // If the returned google project ID from BQ doesn't exist in the DB, do nothing. This may
      // happen in a shared env environment such as test. I am not sure if it would happen in other
      // envs.
      if (!projectIdToDbWorkspace.containsKey(bucketAuditEntry.getGoogleProjectId())) {
        continue;
      }

      DbWorkspace workspace = projectIdToDbWorkspace.get(bucketAuditEntry.getGoogleProjectId());
      logger.warning(
          String.format(
              "Found an audit entry that exceeds the threshold, workspace namespace: %s, google ID: %s",
              workspace.getWorkspaceNamespace(), workspace.getGoogleProject()));

      // Get the workspace from firecloud. In order to get the bucket name. To avoid processing
      // other buckets not related to the user actions.
      final RawlsWorkspaceResponse fsWorkspace =
          fireCloudService.getWorkspaceAsService(
              workspace.getWorkspaceNamespace(), workspace.getFirecloudName());

      final String bucketName = fsWorkspace.getWorkspace().getBucketName();

      // We're only concerned about the workspace bucket, any other buckets should be ignored
      // because the researcher doesn't have access on them.
      if (StringUtils.isNoneEmpty(bucketName)
          && bucketName.equals(bucketAuditEntry.getBucketName())) {

        logger.info(
            String.format("Bucket found for the offending workspace, bucket name: %s", bucketName));
        // Get the user which caused the egress alert from his pet account.
        // There is no known straightforward way to find this other than getting the
        // users of this workspace and lookup which user the pet account corresponds to.
        final List<UserRole> workspaceOwners = getWorkspaceOwnersOrWriters(workspace);
        final Set<DbUser> activeUsers =
            userService.findActiveUsersByUsernames(
                workspaceOwners.stream().map(UserRole::getEmail).collect(Collectors.toList()));

        maybeAlertUsers(bucketAuditEntry, workspace, activeUsers);
      }
    }
  }

  private void maybeAlertUsers(
      BucketAuditEntry bucketAuditEntry, DbWorkspace workspace, Set<DbUser> activeUsers) {
    for (DbUser user : activeUsers) {
      // Get user pet account
      try {
        Optional<String> petServiceAccount =
            iamService.getOrCreatePetServiceAccountUsingImpersonation(
                workspace.getGoogleProject(), user.getUsername());

        if (petServiceAccount.isPresent()
            && petServiceAccount.get().equals(bucketAuditEntry.getPetAccount())) {
          logger.info(
              String.format(
                  "Alerting user with ID: %s, workspace with long file names is: %s",
                  user.getUserId(), workspace.getWorkspaceId()));
          // Create an egress alert.
          DbEgressEvent event = this.persistEgressEvent(bucketAuditEntry, user, workspace);
          // There's no need to push this event to the executor because it's not expected that
          // it happens frequently. And if it happens, it has to be handled immediately.
          egressRemediationService.remediateEgressEvent(event.getEgressEventId());
        }

      } catch (IOException | ApiException e) {
        // Log and continue nothing we can do.
        logger.log(
            Level.WARNING,
            String.format(
                "Unable to get file info from BQ for workspace %s and user %s",
                workspace.getWorkspaceId(), user.getUsername()));
      }
    }
  }

  /**
   * Get the workspace owners from firecloud. To iterate over them and make sure noone created the
   * files with super long names. Filter only on OWNER and WRITER roles, because a reader shouldn't
   * have access to write files.
   *
   * @param workspace The workspace to get the owners
   * @return List of user roles that are either owners or writers on this workspace
   */
  @NotNull
  private List<UserRole> getWorkspaceOwnersOrWriters(DbWorkspace workspace) {
    return workspaceService
        .getFirecloudUserRoles(workspace.getWorkspaceNamespace(), workspace.getFirecloudName())
        .stream()
        .filter(
            user ->
                WorkspaceAccessLevel.OWNER.equals(user.getRole())
                    || WorkspaceAccessLevel.WRITER.equals(user.getRole()))
        .collect(Collectors.toList());
  }

  private List<BucketAuditEntry> getFileAccessInfoFromBQ() {
    return bucketAuditQueryService.queryBucketFileInformationGroupedByPetAccount();
  }

  private DbEgressEvent persistEgressEvent(
      BucketAuditEntry bucketAuditEntry, DbUser dbUser, DbWorkspace dbWorkspace) {

    return egressEventDao.save(
        new DbEgressEvent()
            .setUser(dbUser)
            .setWorkspace(dbWorkspace)
            .setSumologicEvent("{}")
            .setEgressMegabytes((float) (bucketAuditEntry.getFileLengths() / (1024.0 * 1024.0)))
            .setEgressWindowSeconds(bucketAuditEntry.getTimeWindowDurationInSeconds())
            .setStatus(DbEgressEventStatus.PENDING));
  }
}
