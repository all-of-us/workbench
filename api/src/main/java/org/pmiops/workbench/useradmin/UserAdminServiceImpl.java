package org.pmiops.workbench.useradmin;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.joda.time.DateTime;
import org.pmiops.workbench.actionaudit.ActionAuditQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.model.Profile;
import org.pmiops.workbench.model.UserAuditLogQueryResponse;
import org.pmiops.workbench.profile.ProfileService;
import org.springframework.stereotype.Service;

@Service
public class UserAdminServiceImpl implements UserAdminService {

  private ActionAuditQueryService actionAuditQueryService;
  private Clock clock;
  private InstitutionService institutionService;
  private Provider<WorkbenchConfig> workbenchConfigProvider;
  private ProfileService profileService;
  private UserService userService;

  public UserAdminServiceImpl(
      ActionAuditQueryService actionAuditQueryService,
      Clock clock,
      InstitutionService institutionService,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      ProfileService profileService,
      UserService userService) {
    this.actionAuditQueryService = actionAuditQueryService;
    this.clock = clock;
    this.institutionService = institutionService;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.profileService = profileService;
    this.userService = userService;
  }

  @Override
  public UserAuditLogQueryResponse queryEventsForUser(
      String usernameWithoutGsuiteDomain,
      Integer limit,
      Long afterMillis,
      Long beforeMillisNullable) {
    final String username =
        String.format(
            "%s@%s",
            usernameWithoutGsuiteDomain,
            workbenchConfigProvider.get().googleDirectoryService.gSuiteDomain);
    final long userDatabaseId =
        userService
            .getByUsername(username)
            .map(DbUser::getUserId)
            .orElseThrow(() -> new NotFoundException(String.format("User %s not found", username)));
    final DateTime after = new DateTime(afterMillis);
    final DateTime before =
        Optional.ofNullable(beforeMillisNullable).map(DateTime::new).orElse(DateTime.now());
    return actionAuditQueryService.queryEventsForUser(userDatabaseId, limit, after, before);
  }

  @Override
  public List<Profile> listAllProfiles() {
    return userService.getAllUsers().stream()
        .map(profileService::getProfile)
        .collect(Collectors.toList());
  }
}
