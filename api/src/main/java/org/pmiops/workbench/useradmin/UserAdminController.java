package org.pmiops.workbench.useradmin;

import org.pmiops.workbench.annotations.AuthorityRequired;
import org.pmiops.workbench.api.UserAdminApiDelegate;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.AccessBypassRequest;
import org.pmiops.workbench.model.Authority;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.Profile;
import org.pmiops.workbench.model.UserAuditLogQueryResponse;
import org.pmiops.workbench.model.UserListResponse;
import org.pmiops.workbench.profile.ProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
public class UserAdminController implements UserAdminApiDelegate {

  private final UserService userService;
  private final UserAdminService userAdminService;
  private final ProfileService profileService;

  @Autowired
  public UserAdminController(
      UserService userService, UserAdminService userAdminService, ProfileService profileService) {
    this.userService = userService;
    this.userAdminService = userAdminService;
    this.profileService = profileService;
  }

  @Override
  @AuthorityRequired({Authority.ACCESS_CONTROL_ADMIN})
  public ResponseEntity<EmptyResponse> bypassAccessRequirement(
      Long userId, AccessBypassRequest bypassed) {
    userService.updateBypassTime(userId, bypassed);
    return ResponseEntity.noContent().build();
  }

  @Override
  @AuthorityRequired({Authority.ACCESS_CONTROL_ADMIN})
  public ResponseEntity<UserListResponse> getAllUsers() {
    return ResponseEntity.ok(new UserListResponse().profileList(profileService.listAllProfiles()));
  }

  @Override
  @AuthorityRequired({Authority.ACCESS_CONTROL_ADMIN})
  public ResponseEntity<UserAuditLogQueryResponse> getAuditLogEntries(
      String usernameWithoutGsuiteDomain, Integer limit, Long after, Long before) {
    return ResponseEntity.ok(
        userAdminService.queryEventsForUser(usernameWithoutGsuiteDomain, limit, after, before));
  }

  @Override
  @AuthorityRequired({Authority.ACCESS_CONTROL_ADMIN})
  public ResponseEntity<Profile> getUser(Long userDatabaseId) {
    return userService
        .getByDatabaseId(userDatabaseId)
        .map(profileService::getProfile)
        .map(ResponseEntity::ok)
        .orElseThrow(
            () ->
                new NotFoundException(
                    String.format("User with database ID %d not found", userDatabaseId)));
  }

  @Override
  @AuthorityRequired({Authority.ACCESS_CONTROL_ADMIN})
  public ResponseEntity<Profile> getUserByUsername(String username) {
    return userService
        .getByUsername(username)
        .map(profileService::getProfile)
        .map(ResponseEntity::ok)
        .orElseThrow(
            () ->
                new NotFoundException(String.format("User with username %s not found", username)));
  }
}
