package org.pmiops.workbench.exfiltration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.pmiops.workbench.exfiltration.ExfiltrationConstants.THRESHOLD_MB;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.javers.common.collections.Maps;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pmiops.workbench.actionaudit.bucket.BucketAuditEntry;
import org.pmiops.workbench.actionaudit.bucket.BucketAuditQueryService;
import org.pmiops.workbench.db.dao.EgressEventDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbEgressEvent;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exfiltration.impl.EgressObjectLengthsRemediationService;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.iam.IamService;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceResponse;
import org.pmiops.workbench.workspaces.WorkspaceService;

@ExtendWith(MockitoExtension.class)
public class ObjectNameLengthServiceTest {

  public static final String GOOGLE_PROJECT = "google-project";
  public static final String FIRECLOUD_NAME = "firecloud-name";
  public static final String USER_EMAIL = "test@aou.com";
  public static final String NAMESPACE = "namespace";
  public static final String PET_ACCOUNT = "pet@service.com";

  @InjectMocks private ObjectNameLengthServiceImpl objectNameLengthService;

  @Mock private FireCloudService fireCloudService;
  @Mock private WorkspaceService workspaceService;
  @Mock private BucketAuditQueryService bucketAuditQueryService;
  @Mock private IamService iamService;
  @Mock private UserService userService;
  @Mock private EgressEventDao egressEventDao;
  @Mock private EgressObjectLengthsRemediationService egressRemediationService;

  @Test
  void calculateObjectNameLength_noAlertsFired_WhenNoFileInfoReturned() {
    doReturn(Collections.emptyList())
        .when(bucketAuditQueryService)
        .queryBucketFileInformationGroupedByPetAccount();

    objectNameLengthService.calculateObjectNameLength();

    verifyNoInteractions(iamService);
    verifyNoInteractions(egressEventDao);
    verifyNoInteractions(egressRemediationService);
  }

  @Test
  void calculateObjectNameLength_firesAlert_WhenFileLengthIsGreaterThanThreshold()
      throws IOException, ApiException {

    List<BucketAuditEntry> entries =
        Collections.singletonList(getBucketAuditEntry(PET_ACCOUNT, THRESHOLD_MB + 1));
    doReturn(entries).when(bucketAuditQueryService).queryBucketFileInformationGroupedByPetAccount();

    DbWorkspace dbWorkspace =
        new DbWorkspace()
            .setGoogleProject(GOOGLE_PROJECT)
            .setWorkspaceNamespace(NAMESPACE)
            .setFirecloudName(FIRECLOUD_NAME);
    Map<String, DbWorkspace> workspaces = Maps.of(GOOGLE_PROJECT, dbWorkspace);
    doReturn(workspaces)
        .when(workspaceService)
        .getWorkspacesByGoogleProject(Sets.newHashSet(GOOGLE_PROJECT));

    RawlsWorkspaceResponse response =
        new RawlsWorkspaceResponse()
            .workspace(new RawlsWorkspaceDetails().bucketName("some-bucket"));
    doReturn(response).when(fireCloudService).getWorkspaceAsService(NAMESPACE, FIRECLOUD_NAME);

    List<UserRole> userRoles =
        Collections.singletonList(
            new UserRole().role(WorkspaceAccessLevel.OWNER).email(USER_EMAIL));
    doReturn(userRoles).when(workspaceService).getFirecloudUserRoles(NAMESPACE, FIRECLOUD_NAME);

    DbUser aUser = new DbUser().setUsername(USER_EMAIL);
    Set<DbUser> dbUsers = Sets.newHashSet(aUser);

    doReturn(dbUsers)
        .when(userService)
        .findActiveUsersByUsernames(Collections.singletonList(USER_EMAIL));

    doReturn(Optional.of(PET_ACCOUNT))
        .when(iamService)
        .getOrCreatePetServiceAccountUsingImpersonation(GOOGLE_PROJECT, aUser.getUsername());

    DbEgressEvent egressEvent = new DbEgressEvent();
    doReturn(egressEvent).when(egressEventDao).save(any(DbEgressEvent.class));

    objectNameLengthService.calculateObjectNameLength();

    verify(iamService, times(1))
        .getOrCreatePetServiceAccountUsingImpersonation(GOOGLE_PROJECT, aUser.getUsername());

    verify(egressEventDao, times(1)).save(any(DbEgressEvent.class));
  }

  @Test
  void calculateObjectNameLength_firesAlertsForMultipleUser_WhenFileLengthIsGreaterThanThreshold()
      throws IOException, ApiException {

    List<BucketAuditEntry> entries = new ArrayList<>();
    BucketAuditEntry entry = getBucketAuditEntry(PET_ACCOUNT, THRESHOLD_MB + 1);
    entries.add(entry);
    entry = getBucketAuditEntry("pet2@service.com", THRESHOLD_MB + 1);
    entries.add(entry);
    doReturn(entries).when(bucketAuditQueryService).queryBucketFileInformationGroupedByPetAccount();

    DbWorkspace dbWorkspace =
        new DbWorkspace()
            .setGoogleProject(GOOGLE_PROJECT)
            .setWorkspaceNamespace(NAMESPACE)
            .setFirecloudName(FIRECLOUD_NAME);
    Map<String, DbWorkspace> workspaces = Maps.of(GOOGLE_PROJECT, dbWorkspace);
    doReturn(workspaces)
        .when(workspaceService)
        .getWorkspacesByGoogleProject(Sets.newHashSet(GOOGLE_PROJECT));

    RawlsWorkspaceResponse response =
        new RawlsWorkspaceResponse()
            .workspace(new RawlsWorkspaceDetails().bucketName("some-bucket"));

    doReturn(response).when(fireCloudService).getWorkspaceAsService(NAMESPACE, FIRECLOUD_NAME);

    List<UserRole> userRoles =
        Lists.newArrayList(
            new UserRole().role(WorkspaceAccessLevel.OWNER).email(USER_EMAIL),
            new UserRole().role(WorkspaceAccessLevel.WRITER).email("1" + USER_EMAIL));
    doReturn(userRoles).when(workspaceService).getFirecloudUserRoles(NAMESPACE, FIRECLOUD_NAME);

    DbUser aUser = new DbUser().setUsername(USER_EMAIL);
    DbUser anotherUser = new DbUser().setUsername("1" + USER_EMAIL);

    Set<DbUser> dbUsers = Sets.newHashSet(aUser, anotherUser);

    doReturn(dbUsers)
        .when(userService)
        .findActiveUsersByUsernames(Lists.newArrayList(USER_EMAIL, "1" + USER_EMAIL));

    doReturn(Optional.of(PET_ACCOUNT))
        .when(iamService)
        .getOrCreatePetServiceAccountUsingImpersonation(GOOGLE_PROJECT, aUser.getUsername());

    doReturn(Optional.of("pet2@service.com"))
        .when(iamService)
        .getOrCreatePetServiceAccountUsingImpersonation(GOOGLE_PROJECT, anotherUser.getUsername());

    DbEgressEvent egressEvent = new DbEgressEvent();
    doReturn(egressEvent).when(egressEventDao).save(any(DbEgressEvent.class));

    objectNameLengthService.calculateObjectNameLength();

    verify(iamService, times(2))
        .getOrCreatePetServiceAccountUsingImpersonation(GOOGLE_PROJECT, aUser.getUsername());

    verify(egressEventDao, times(2)).save(any(DbEgressEvent.class));
    verify(egressRemediationService, times(2)).remediateEgressEvent(any(Long.class));
  }

  @NotNull
  private BucketAuditEntry getBucketAuditEntry(String petAccount, long fileLengths) {
    BucketAuditEntry entry = new BucketAuditEntry();
    entry.setPetAccount(petAccount);
    entry.setFileLengths(fileLengths);
    entry.setBucketName("some-bucket");
    entry.setGoogleProjectId(GOOGLE_PROJECT);
    return entry;
  }
}
