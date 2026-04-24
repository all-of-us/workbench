package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.model.VwbAddGroupMemberRequest;
import org.pmiops.workbench.model.VwbGroupDescription;
import org.pmiops.workbench.model.VwbGroupListResponse;
import org.pmiops.workbench.model.VwbGroupMember;
import org.pmiops.workbench.model.VwbGroupMemberListResponse;
import org.pmiops.workbench.model.VwbRemoveGroupMemberRequest;
import org.pmiops.workbench.vwb.user.model.GroupDescription;
import org.pmiops.workbench.vwb.user.model.GroupDescriptionList;
import org.pmiops.workbench.vwb.user.model.GroupMember;
import org.pmiops.workbench.vwb.user.model.GroupRole;
import org.pmiops.workbench.vwb.user.model.Principal;
import org.pmiops.workbench.vwb.user.model.PrincipalUser;
import org.pmiops.workbench.vwb.user.model.PrincipalWorkbenchGroup;
import org.pmiops.workbench.vwb.usermanager.VwbUserManagerClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
public class VwbGroupAdminControllerTest {

  @Mock private VwbUserManagerClient mockVwbUserManagerClient;
  @Mock private AccessTierDao mockAccessTierDao;

  private VwbGroupAdminController controller;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    controller = new VwbGroupAdminController(mockVwbUserManagerClient, mockAccessTierDao);
  }

  @Test
  public void testListVwbGroups() {
    GroupDescription group1 =
        new GroupDescription()
            .groupName("aou-prod-rt")
            .createdBy("admin@verily.com")
            .createdDate(OffsetDateTime.parse("2025-01-01T00:00:00Z"));
    GroupDescription group2 =
        new GroupDescription()
            .groupName("custom-group")
            .createdBy("user@verily.com")
            .createdDate(OffsetDateTime.parse("2025-06-01T00:00:00Z"));
    GroupDescriptionList groupList = new GroupDescriptionList().groups(List.of(group1, group2));

    when(mockVwbUserManagerClient.listOrganizationGroups()).thenReturn(groupList);

    DbAccessTier rtTier = new DbAccessTier().setVwbTierGroupName("aou-prod-rt");
    DbAccessTier ctTier = new DbAccessTier().setVwbTierGroupName("aou-prod-ct");
    when(mockAccessTierDao.findAll()).thenReturn(List.of(rtTier, ctTier));

    ResponseEntity<VwbGroupListResponse> response = controller.listVwbGroups();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<VwbGroupDescription> groups = response.getBody().getGroups();
    assertThat(groups).hasSize(2);

    assertThat(groups.get(0).getGroupName()).isEqualTo("aou-prod-rt");
    assertThat(groups.get(0).isManaged()).isTrue();
    assertThat(groups.get(0).getCreatedBy()).isEqualTo("admin@verily.com");

    assertThat(groups.get(1).getGroupName()).isEqualTo("custom-group");
    assertThat(groups.get(1).isManaged()).isFalse();
  }

  @Test
  public void testListVwbGroups_noManagedGroups() {
    GroupDescription group =
        new GroupDescription().groupName("custom-group").createdBy("user@verily.com");
    GroupDescriptionList groupList = new GroupDescriptionList().groups(List.of(group));

    when(mockVwbUserManagerClient.listOrganizationGroups()).thenReturn(groupList);
    when(mockAccessTierDao.findAll()).thenReturn(Collections.emptyList());

    ResponseEntity<VwbGroupListResponse> response = controller.listVwbGroups();

    assertThat(response.getBody().getGroups()).hasSize(1);
    assertThat(response.getBody().getGroups().get(0).isManaged()).isFalse();
  }

  @Test
  public void testListVwbGroups_nullVwbTierGroupName() {
    GroupDescription group =
        new GroupDescription().groupName("some-group").createdBy("user@verily.com");
    GroupDescriptionList groupList = new GroupDescriptionList().groups(List.of(group));

    when(mockVwbUserManagerClient.listOrganizationGroups()).thenReturn(groupList);

    DbAccessTier tierWithNull = new DbAccessTier().setVwbTierGroupName(null);
    DbAccessTier tierWithEmpty = new DbAccessTier().setVwbTierGroupName("");
    when(mockAccessTierDao.findAll()).thenReturn(List.of(tierWithNull, tierWithEmpty));

    ResponseEntity<VwbGroupListResponse> response = controller.listVwbGroups();

    assertThat(response.getBody().getGroups().get(0).isManaged()).isFalse();
  }

  @Test
  public void testListVwbGroupMembers_userPrincipal() {
    PrincipalUser principalUser = new PrincipalUser().email("user@verily.com");
    Principal principal = new Principal().userPrincipal(principalUser);
    GroupMember member =
        new GroupMember().principal(principal).roles(List.of(GroupRole.MEMBER, GroupRole.ADMIN));

    when(mockVwbUserManagerClient.listGroupMembers("test-group")).thenReturn(List.of(member));

    ResponseEntity<VwbGroupMemberListResponse> response =
        controller.listVwbGroupMembers("test-group");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    List<VwbGroupMember> members = response.getBody().getMembers();
    assertThat(members).hasSize(1);
    assertThat(members.get(0).getEmail()).isEqualTo("user@verily.com");
    assertThat(members.get(0).getRoles()).containsExactly("MEMBER", "ADMIN");
  }

  @Test
  public void testListVwbGroupMembers_groupPrincipal() {
    PrincipalWorkbenchGroup groupPrincipal =
        new PrincipalWorkbenchGroup().groupName("nested-group");
    Principal principal = new Principal().groupPrincipal(groupPrincipal);
    GroupMember member = new GroupMember().principal(principal).roles(List.of(GroupRole.MEMBER));

    when(mockVwbUserManagerClient.listGroupMembers("test-group")).thenReturn(List.of(member));

    ResponseEntity<VwbGroupMemberListResponse> response =
        controller.listVwbGroupMembers("test-group");

    List<VwbGroupMember> members = response.getBody().getMembers();
    assertThat(members).hasSize(1);
    assertThat(members.get(0).getEmail()).isEqualTo("nested-group");
    assertThat(members.get(0).getRoles()).containsExactly("MEMBER");
  }

  @Test
  public void testListVwbGroupMembers_nullPrincipal() {
    GroupMember member = new GroupMember().principal(null).roles(List.of(GroupRole.READER));

    when(mockVwbUserManagerClient.listGroupMembers("test-group")).thenReturn(List.of(member));

    ResponseEntity<VwbGroupMemberListResponse> response =
        controller.listVwbGroupMembers("test-group");

    List<VwbGroupMember> members = response.getBody().getMembers();
    assertThat(members).hasSize(1);
    assertThat(members.get(0).getEmail()).isNull();
    assertThat(members.get(0).getRoles()).containsExactly("READER");
  }

  @Test
  public void testListVwbGroupMembers_nullRoles() {
    PrincipalUser principalUser = new PrincipalUser().email("user@verily.com");
    Principal principal = new Principal().userPrincipal(principalUser);
    GroupMember member = new GroupMember().principal(principal);
    member.setRoles(null);

    when(mockVwbUserManagerClient.listGroupMembers("test-group")).thenReturn(List.of(member));

    ResponseEntity<VwbGroupMemberListResponse> response =
        controller.listVwbGroupMembers("test-group");

    assertThat(response.getBody().getMembers().get(0).getRoles()).isEmpty();
  }

  @Test
  public void testListVwbGroupMembers_emptyList() {
    when(mockVwbUserManagerClient.listGroupMembers("empty-group"))
        .thenReturn(Collections.emptyList());

    ResponseEntity<VwbGroupMemberListResponse> response =
        controller.listVwbGroupMembers("empty-group");

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody().getMembers()).isEmpty();
  }

  @Test
  public void testAddVwbGroupMember_memberRole() {
    VwbAddGroupMemberRequest request =
        new VwbAddGroupMemberRequest()
            .email("newuser@verily.com")
            .role(VwbAddGroupMemberRequest.RoleEnum.MEMBER)
            .reason("Needs access for testing");

    ResponseEntity<Void> response = controller.addVwbGroupMember("test-group", request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    verify(mockVwbUserManagerClient)
        .addUserToGroup(
            "test-group", "newuser@verily.com", GroupRole.MEMBER, "Needs access for testing");
  }

  @Test
  public void testAddVwbGroupMember_adminRole() {
    VwbAddGroupMemberRequest request =
        new VwbAddGroupMemberRequest()
            .email("admin@verily.com")
            .role(VwbAddGroupMemberRequest.RoleEnum.ADMIN)
            .reason("Admin access required");

    ResponseEntity<Void> response = controller.addVwbGroupMember("test-group", request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    verify(mockVwbUserManagerClient)
        .addUserToGroup(
            "test-group", "admin@verily.com", GroupRole.ADMIN, "Admin access required");
  }

  @Test
  public void testRemoveVwbGroupMember() {
    VwbRemoveGroupMemberRequest request =
        new VwbRemoveGroupMemberRequest().email("user@verily.com");

    ResponseEntity<Void> response = controller.removeVwbGroupMember("test-group", request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    verify(mockVwbUserManagerClient).removeUserFromGroup("test-group", "user@verily.com");
  }
}
