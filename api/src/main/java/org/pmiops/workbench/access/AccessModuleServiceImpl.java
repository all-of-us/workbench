package org.pmiops.workbench.access;

import static org.pmiops.workbench.access.AccessUtils.auditAccessModuleFromStorage;
import static org.pmiops.workbench.access.AccessUtils.clientAccessModuleToStorage;
import static org.pmiops.workbench.access.AccessUtils.storageAccessModuleToClient;

import com.google.common.base.Preconditions;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.inject.Provider;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditor;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.AccessConfig;
import org.pmiops.workbench.db.dao.UserAccessModuleDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbAccessModule;
import org.pmiops.workbench.db.model.DbAccessModule.AccessModuleName;
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
    DbUserAccessModule userAccessModuleToUpdate =
        retrieveUserAccessModuleOrCreate(user, accessModule);
    final Timestamp newBypassTime =
        isBypassed ? new Timestamp(clock.instant().toEpochMilli()) : null;
    final Timestamp previousBypassTime = userAccessModuleToUpdate.getBypassTime();

    logger.info(
        String.format(
            "Setting %s(uid: %d) for module %s bypass status to %s",
            user.getUsername(), userId, accessModule.getName(), isBypassed));

    userAccessModuleToUpdate.setBypassTime(newBypassTime);
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
  public void updateCompletionTime(
      DbUser dbUser, AccessModuleName accessModuleName, @Nullable Timestamp timestamp) {
    DbAccessModule dbAccessModule =
        getDbAccessModuleOrThrow(dbAccessModulesProvider.get(), accessModuleName);
    DbUserAccessModule userAccessModuleToUpdate =
        retrieveUserAccessModuleOrCreate(dbUser, dbAccessModule);
    userAccessModuleDao.save(userAccessModuleToUpdate.setCompletionTime(timestamp));
  }

  @Override
  public List<AccessModuleStatus> getAccessModuleStatus(DbUser user) {
    return userAccessModuleDao.getAllByUser(user).stream()
        .map(this::mapToEnabledAccessModule)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
  }

  @Override
  public Optional<AccessModuleStatus> getAccessModuleStatus(
      DbUser user, AccessModuleName accessModuleName) {
    DbAccessModule dbAccessModule =
        getDbAccessModuleOrThrow(dbAccessModulesProvider.get(), accessModuleName);
    DbUserAccessModule userAccessModule = retrieveUserAccessModuleOrCreate(user, dbAccessModule);
    return mapToEnabledAccessModule(userAccessModule);
  }

  private Optional<AccessModuleStatus> mapToEnabledAccessModule(
      DbUserAccessModule dbUserAccessModule) {
    return Optional.of(dbUserAccessModule)
        .map(a -> userAccessModuleMapper.dbToModule(a, getExpirationTime(a).orElse(null)))
        .filter(a -> isModuleEnabledInEnvironment(a.getModuleName()));
  }

  @Override
  public boolean isModuleCompliant(DbUser dbUser, AccessModuleName accessModuleName) {
    DbAccessModule dbAccessModule =
        getDbAccessModuleOrThrow(dbAccessModulesProvider.get(), accessModuleName);
    // if the module is not enabled, the user is always compliant
    if (!isModuleEnabledInEnvironment(storageAccessModuleToClient(dbAccessModule.getName()))) {
      return true;
    }
    DbUserAccessModule userAccessModule = retrieveUserAccessModuleOrCreate(dbUser, dbAccessModule);
    boolean isBypassed = dbAccessModule.getBypassable() && userAccessModule.getBypassTime() != null;
    boolean isCompleted = userAccessModule.getCompletionTime() != null;
    boolean isExpired =
        getExpirationTime(userAccessModule)
            .map(x -> x.before(new Timestamp(clock.millis())))
            .orElse(false);

    // A module is completed when it is bypassed OR (completed but not expired).
    return isBypassed || (isCompleted && !isExpired);
  }

  @Override
  public boolean isModuleBypassed(DbUser dbUser, AccessModuleName accessModuleName) {
    DbAccessModule dbAccessModule =
        getDbAccessModuleOrThrow(dbAccessModulesProvider.get(), accessModuleName);
    return dbAccessModule.getBypassable()
        && retrieveUserAccessModuleOrCreate(dbUser, dbAccessModule).getBypassTime() != null;
  }

  /**
   * Retrieves the existing {@link DbUserAccessModule} by user and access module. Create a new one
   * if not existing in DB.
   */
  private DbUserAccessModule retrieveUserAccessModuleOrCreate(
      DbUser user, DbAccessModule dbAccessModule) {
    return userAccessModuleDao
        .getByUserAndAccessModule(user, dbAccessModule)
        .orElse(new DbUserAccessModule().setUser(user).setAccessModule(dbAccessModule));
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
   * </ul>
   */
  private Optional<Timestamp> getExpirationTime(DbUserAccessModule dbUserAccessModule) {
    if (!dbUserAccessModule.getAccessModule().getExpirable()
        || dbUserAccessModule.getCompletionTime() == null
        || dbUserAccessModule.getBypassTime() != null) {
      return Optional.empty();
    }
    return Optional.of(
        deriveExpirationTimestamp(
            dbUserAccessModule.getCompletionTime(), configProvider.get().accessRenewal.expiryDays));
  }

  private static DbAccessModule getDbAccessModuleOrThrow(
      List<DbAccessModule> dbAccessModules, AccessModuleName accessModuleName) {
    return dbAccessModules.stream()
        .filter(a -> a.getName() == accessModuleName)
        .findFirst()
        .orElseThrow(
            () ->
                new BadRequestException(
                    "There is no access module named: " + accessModuleName.toString()));
  }

  private static DbAccessModule getDbAccessModuleFromApi(
      List<DbAccessModule> dbAccessModules, AccessModule apiAccessModule) {
    return getDbAccessModuleOrThrow(dbAccessModules, clientAccessModuleToStorage(apiAccessModule));
  }

  /**
   * Extracts module expiration time from completionTime and expiry days: completionTime plus
   * expiryDays in millseconds.
   */
  public static Timestamp deriveExpirationTimestamp(Timestamp completionTime, Long expiryDays) {
    Preconditions.checkNotNull(
        expiryDays, "expected value for config key accessRenewal.expiryDays.expiryDays");
    long expiryDaysInMs = TimeUnit.MILLISECONDS.convert(expiryDays, TimeUnit.DAYS);
    return new Timestamp(completionTime.getTime() + expiryDaysInMs);
  }

  private boolean isModuleEnabledInEnvironment(AccessModule module) {
    final AccessConfig accessConfig = configProvider.get().access;

    switch (module) {
      case ERA_COMMONS:
        return accessConfig.enableEraCommons;
      case COMPLIANCE_TRAINING:
        return accessConfig.enableComplianceTraining;
      case RAS_LINK_LOGIN_GOV:
        return accessConfig.enforceRasLoginGovLinking || accessConfig.enableRasLoginGovLinking;
      default:
        return true;
    }
  }
}
