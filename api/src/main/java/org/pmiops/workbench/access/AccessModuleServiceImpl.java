package org.pmiops.workbench.access;

import static org.pmiops.workbench.access.AccessUtils.auditAccessModuleFromStorage;
import static org.pmiops.workbench.access.AccessUtils.clientAccessModuleToStorage;

import com.google.common.base.Preconditions;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditor;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserAccessModuleDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbAccessModule;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserAccessModule;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.model.AccessModule;
import org.pmiops.workbench.model.AccessModuleStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccessModuleServiceImpl implements AccessModuleService {
  private static final Logger logger = Logger.getLogger(AccessModuleServiceImpl.class.getName());

  private final Provider<List<DbAccessModule>> dbAccessModulesProvider;
  private final Clock clock;

  private final UserAccessModuleDao userAccessModuleDao;
  private final UserDao userDao;
  private final UserServiceAuditor userServiceAuditor;
  private final Provider<WorkbenchConfig> configProvider;
  private final UserAccessModuleMapper userAccessModuleMapper;

  @Autowired
  public AccessModuleServiceImpl(
      Provider<List<DbAccessModule>> dbAccessModulesProvider,
      Clock clock,
      UserAccessModuleDao userAccessModuleDao,
      UserDao userDao,
      UserServiceAuditor userServiceAuditor,
      Provider<WorkbenchConfig> configProvider,
      UserAccessModuleMapper userAccessModuleMapper) {
    this.dbAccessModulesProvider = dbAccessModulesProvider;
    this.clock = clock;
    this.userAccessModuleDao = userAccessModuleDao;
    this.userDao = userDao;
    this.userServiceAuditor = userServiceAuditor;
    this.configProvider = configProvider;
    this.userAccessModuleMapper = userAccessModuleMapper;
  }

  @Override
  public void updateBypassTime(long userId, AccessModule accessModuleName, boolean isBypassed) {
    DbAccessModule accessModule =
        getDbAccessModuleFromApi(dbAccessModulesProvider.get(), accessModuleName);
    if (!accessModule.getBypassable()) {
      throw new ForbiddenException("Bypass: " + accessModuleName.toString() + " is not allowed.");
    }
    final DbUser user = userDao.findUserByUserId(userId);
    final List<DbUserAccessModule> dbUserAccessModules = userAccessModuleDao.getAllByUser(user);
    Optional<DbUserAccessModule> retrievedAccessModule =
        dbUserAccessModules.stream()
            .filter(m -> m.getAccessModule().getName().equals(accessModule.getName()))
            .findFirst();
    final Timestamp newBypassTime =
        isBypassed ? new Timestamp(clock.instant().toEpochMilli()) : null;
    final Timestamp previousBypassTime =
        retrievedAccessModule.map(DbUserAccessModule::getBypassTime).orElse(null);
    final DbUserAccessModule userAccessModuleToUpdate;

    logger.info(
        String.format(
            "Setting %s(uid: %d) for module %s bypass status to %s",
            user.getUsername(), userId, accessModule.getName(), isBypassed));
    userAccessModuleToUpdate =
        retrievedAccessModule
            .orElseGet(() -> new DbUserAccessModule().setUser(user).setAccessModule(accessModule))
            .setBypassTime(newBypassTime);

    userAccessModuleDao.save(userAccessModuleToUpdate);
    if (configProvider.get().featureFlags.enableAccessModuleRewrite) {
      // If enabled, fire audit event from here instead of from UserService.
      userServiceAuditor.fireAdministrativeBypassTime(
          user.getUserId(),
          auditAccessModuleFromStorage(accessModule.getName()),
          Optional.ofNullable(previousBypassTime).map(Timestamp::toInstant),
          Optional.ofNullable(newBypassTime).map(Timestamp::toInstant));
    }
  }

  @Override
  public List<AccessModuleStatus> getClientAccessModuleStatus(DbUser user) {
    return userAccessModuleDao.getAllByUser(user).stream()
        .map(a -> userAccessModuleMapper.dbToModule(a, getExpirationTime(a).orElse(null)))
        .collect(Collectors.toList());
  }

  private static DbAccessModule getDbAccessModuleFromApi(
      List<DbAccessModule> dbAccessModules, AccessModule apiAccessModule) {
    return dbAccessModules.stream()
        .filter(a -> a.getName() == clientAccessModuleToStorage(apiAccessModule))
        .findFirst()
        .orElseThrow(
            () ->
                new BadRequestException(
                    "There is no access module named: " + apiAccessModule.toString()));
  }

  /**
   * Calculates the module expiration time.
   *
   * <p>The value is only present when:
   *
   * <ul>
   *   <li>The module is expirable.
   *   <li>The module was completed(CompletionTime is not null).
   *   <li>The module is not bypassed(BypassTime is null).
   *   <li>Access annual renewal is enabled(enableAccessRenewal is true).
   * </ul>
   */
  private Optional<Timestamp> getExpirationTime(DbUserAccessModule dbUserAccessModule) {
    // Don't set expiration time if CompletionTime is null OR BypassTime is NOT null OR
    // enableAccessRenewal not enabled.
    if (!dbUserAccessModule.getAccessModule().getExpirable()
        || dbUserAccessModule.getCompletionTime() == null
        || dbUserAccessModule.getBypassTime() != null
            && !configProvider.get().access.enableAccessRenewal) {
      return Optional.empty();
    }
    return Optional.of(
        extractExpirationTimestamp(
            dbUserAccessModule.getCompletionTime(), configProvider.get().accessRenewal.expiryDays));
  }

  /**
   * Extracts module expiration time from completionTime and expiry days: completionTime plus
   * expiryDays in millseconds.
   */
  public static Timestamp extractExpirationTimestamp(Timestamp completionTime, Long expiryDays) {
    Preconditions.checkNotNull(
        expiryDays, "expected value for config key accessRenewal.expiryDays.expiryDays");
    long expiryDaysInMs = TimeUnit.MILLISECONDS.convert(expiryDays, TimeUnit.DAYS);
    return new Timestamp(completionTime.getTime() + expiryDaysInMs);
  }
}
