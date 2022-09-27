package org.pmiops.workbench.exfiltration;

import static org.pmiops.workbench.exfiltration.ObjectNameSizeConstants.THRESHOLD;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
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
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.iam.IamService;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ObjectNameLengthServiceImpl implements ObjectNameLengthService {

  private static final Logger LOGGER =
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
      @Qualifier("internal-remediation-service")
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
   * For the given workspace, get its file access info from BQ grouped by the pet account. If the
   * lengths of the filenames is greater than the threshold, an alert is triggered for the user who
   * created the files.
   *
   * <p>Depending on the policy, the {@link EgressRemediationService} takes care of disabling the
   * user, emailing him about the problem, and creating the jira ticket. By disabled the user we
   * make sure that the user doesn't get multiple alerts for the same problem because this method
   * filters only the active users.
   *
   * @param workspace The workspace to check for the filtration attempt.
   */
  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void calculateObjectNameLength(DbWorkspace workspace) {

    // Get the workspace from firecloud. In order to get the bucket name. The bucket name is needed
    // to filter entries from BQ
    final FirecloudWorkspaceResponse fsWorkspace =
        fireCloudService.getWorkspaceAsService(
            workspace.getWorkspaceNamespace(), workspace.getFirecloudName());
    final String bucketName = fsWorkspace.getWorkspace().getBucketName();

    // Call BQ to get the files created by each owner in the past 24 hours
    List<BucketAuditEntry> fileAccessInfoFromBQ =
        getFileAccessInfoFromBQ(bucketName, workspace.getGoogleProject());

    for (BucketAuditEntry bucketAuditEntry : fileAccessInfoFromBQ) {
      if (bucketAuditEntry.getFileLengths() > THRESHOLD) {
        // Get the user which caused the egress alert from his pet account.
        // There is no known straightforward way to find this other than getting the
        // users of this workspace and lookup which user does the pet account corresponds to.
        final List<UserRole> workspaceOwners = getWorkspaceOwners(workspace);
        final Set<DbUser> activeUsers =
            userService.findActiveUsersByUsernames(
                workspaceOwners.stream().map(UserRole::getEmail).collect(Collectors.toList()));

        for (DbUser user : activeUsers) {
          // Get user pet account
          try {
            Optional<String> petServiceAccount =
                iamService.getOrCreatePetServiceAccountUsingImpersonation(
                    workspace.getGoogleProject(), user.getUsername());

            if (petServiceAccount.isPresent()
                && petServiceAccount.get().equals(bucketAuditEntry.getPetAccount())) {
              // Create an egress alert.
              Optional<DbEgressEvent> maybeEvent =
                  this.maybePersistEgressEvent(bucketAuditEntry.getFileLengths(), user, workspace);
              // There's no need to push this event to the executor because it's not expected that
              // it happens frequently. And if it happens, it has to be handled immediately.
              egressRemediationService.remediateEgressEvent(maybeEvent.get().getEgressEventId());
            }

          } catch (IOException | ApiException e) {
            // Log and continue nothing we can do.
            LOGGER.log(
                Level.WARNING,
                String.format(
                    "Unable to get file info from BQ for workspace %s and user %s",
                    workspace.getWorkspaceId(), user.getUsername()));
          }
        }
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
  private List<UserRole> getWorkspaceOwners(DbWorkspace workspace) {
    return workspaceService
        .getFirecloudUserRoles(workspace.getWorkspaceNamespace(), workspace.getFirecloudName())
        .stream()
        .filter(
            user ->
                WorkspaceAccessLevel.OWNER.equals(user.getRole())
                    || WorkspaceAccessLevel.WRITER.equals(user.getRole()))
        .collect(Collectors.toList());
  }

  private List<BucketAuditEntry> getFileAccessInfoFromBQ(
      String bucketName, String googleProjectId) {
    return bucketAuditQueryService.queryBucketFileInformationGroupedByPetAccount(
        bucketName, googleProjectId);
  }

  private Optional<DbEgressEvent> maybePersistEgressEvent(
      Long objectNameLengths, DbUser dbUser, DbWorkspace dbWorkspace) {

    return Optional.of(
        egressEventDao.save(
            new DbEgressEvent()
                .setUser(dbUser)
                .setWorkspace(dbWorkspace)
                .setSumologicEvent("{}")
                .setEgressMegabytes(
                    Optional.ofNullable(objectNameLengths)
                        // bytes -> Megabytes (10^6 bytes)
                        .map(bytes -> (float) (bytes / (1024 * 1024)))
                        .orElse(null))
                .setStatus(DbEgressEventStatus.PENDING)));
  }
}
