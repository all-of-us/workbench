package org.pmiops.workbench.useradmin;

import java.util.Optional;
import javax.inject.Provider;
import org.joda.time.DateTime;
import org.pmiops.workbench.actionaudit.ActionAuditQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.model.UserAuditLogQueryResponse;
import org.springframework.stereotype.Service;

@Service
public class UserAdminServiceImpl implements UserAdminService {

  private final ActionAuditQueryService actionAuditQueryService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final UserService userService;

  public UserAdminServiceImpl(
      ActionAuditQueryService actionAuditQueryService,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      UserService userService) {
    this.actionAuditQueryService = actionAuditQueryService;
    this.workbenchConfigProvider = workbenchConfigProvider;
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
}
