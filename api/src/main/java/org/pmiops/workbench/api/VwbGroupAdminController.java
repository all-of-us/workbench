package org.pmiops.workbench.api;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.pmiops.workbench.annotations.AuthorityRequired;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.VwbAddGroupMemberRequest;
import org.pmiops.workbench.model.VwbGroupDescription;
import org.pmiops.workbench.model.VwbGroupListResponse;
import org.pmiops.workbench.model.VwbGroupMember;
import org.pmiops.workbench.model.VwbGroupMemberListResponse;
import org.pmiops.workbench.model.VwbRemoveGroupMemberRequest;
import org.pmiops.workbench.vwb.user.model.GroupRole;
import org.pmiops.workbench.vwb.usermanager.VwbUserManagerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VwbGroupAdminController implements VwbGroupAdminApiDelegate {

  private static final Logger logger = LoggerFactory.getLogger(VwbGroupAdminController.class);

  private final VwbUserManagerClient vwbUserManagerClient;
  private final AccessTierDao accessTierDao;

  @Autowired
  public VwbGroupAdminController(
      VwbUserManagerClient vwbUserManagerClient, AccessTierDao accessTierDao) {
    this.vwbUserManagerClient = vwbUserManagerClient;
    this.accessTierDao = accessTierDao;
  }

  @Override
  @AuthorityRequired({Authority.RESEARCHER_DATA_VIEW})
  public ResponseEntity<VwbGroupListResponse> listVwbGroups() {
    Set<String> managedGroupNames = getManagedGroupNames();

    var groupDescriptionList = vwbUserManagerClient.listOrganizationGroups();
    List<VwbGroupDescription> groups =
        groupDescriptionList.getGroups().stream()
            .map(
                g ->
                    new VwbGroupDescription()
                        .groupName(g.getGroupName())
                        .managed(managedGroupNames.contains(g.getGroupName()))
                        .createdBy(g.getCreatedBy())
                        .createdDate(
                            g.getCreatedDate() != null ? g.getCreatedDate().toString() : null))
            .collect(Collectors.toList());

    return ResponseEntity.ok(new VwbGroupListResponse().groups(groups));
  }

  @Override
  @AuthorityRequired({Authority.RESEARCHER_DATA_VIEW})
  public ResponseEntity<VwbGroupMemberListResponse> listVwbGroupMembers(String groupName) {
    var members = vwbUserManagerClient.listGroupMembers(groupName);
    List<VwbGroupMember> memberList =
        members.stream()
            .map(
                m -> {
                  String email = null;
                  if (m.getPrincipal() != null && m.getPrincipal().getUserPrincipal() != null) {
                    email = m.getPrincipal().getUserPrincipal().getEmail();
                  } else if (m.getPrincipal() != null
                      && m.getPrincipal().getGroupPrincipal() != null) {
                    email = m.getPrincipal().getGroupPrincipal().getGroupName();
                  }
                  List<String> roles =
                      m.getRoles() != null
                          ? m.getRoles().stream().map(Enum::name).collect(Collectors.toList())
                          : List.of();
                  return new VwbGroupMember().email(email).roles(roles);
                })
            .collect(Collectors.toList());

    return ResponseEntity.ok(new VwbGroupMemberListResponse().members(memberList));
  }

  @Override
  @AuthorityRequired({Authority.ACCESS_CONTROL_ADMIN})
  public ResponseEntity<Void> addVwbGroupMember(
      String groupName, VwbAddGroupMemberRequest request) {
    logger.info(
        "Adding member {} to group {} with role {}. Reason: {}",
        request.getEmail(),
        groupName,
        request.getRole(),
        request.getReason());
    GroupRole role = GroupRole.valueOf(request.getRole().name());
    vwbUserManagerClient.addUserToGroup(groupName, request.getEmail(), role);
    return ResponseEntity.noContent().build();
  }

  @Override
  @AuthorityRequired({Authority.ACCESS_CONTROL_ADMIN})
  public ResponseEntity<Void> removeVwbGroupMember(
      String groupName, VwbRemoveGroupMemberRequest request) {
    logger.info("Removing member {} from group {}", request.getEmail(), groupName);
    vwbUserManagerClient.removeUserFromGroup(groupName, request.getEmail());
    return ResponseEntity.noContent().build();
  }

  private Set<String> getManagedGroupNames() {
    return StreamSupport.stream(accessTierDao.findAll().spliterator(), false)
        .map(DbAccessTier::getVwbTierGroupName)
        .filter(name -> name != null && !name.isEmpty())
        .collect(Collectors.toSet());
  }
}
