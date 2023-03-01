package org.pmiops.workbench.access;

import com.google.common.annotations.VisibleForTesting;
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
import org.pmiops.workbench.db.model.DbAccessModule.DbAccessModuleName;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserAccessModule;
import org.pmiops.workbench.db.model.DbUserCodeOfConductAgreement;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.model.AccessBypassRequest;
import org.pmiops.workbench.model.AccessModule;
import org.pmiops.workbench.model.AccessModuleStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccessModuleServiceImpl implements AccessModuleService {
  private static final Logger logger = Logger.getLogger(AccessModuleServiceImpl.class.getName());

  private final Provider<List<DbAccessModule>> dbAccessModulesProvider;
  private final Clock clock;

  private final AccessModuleNameMapper accessModuleNameMapper;
  private final Provider<WorkbenchConfig> configProvider;
  private final UserAccessModuleDao userAccessModuleDao;
  private final UserAccessModuleMapper userAccessModuleMapper;
  private final UserDao userDao;
  private final UserServiceAuditor userServiceAuditor;

  @Autowired
  public AccessModuleServiceImpl(
      AccessModuleNameMapper accessModuleNameMapper,
      Clock clock,
      Provider<List<DbAccessModule>> dbAccessModulesProvider,
      Provider<WorkbenchConfig> configProvider,
      UserAccessModuleDao userAccessModuleDao,
      UserAccessModuleMapper userAccessModuleMapper,
      UserDao userDao,
      UserServiceAuditor userServiceAuditor) {
    this.accessModuleNameMapper = accessModuleNameMapper;
    this.clock = clock;
    this.configProvider = configProvider;
    this.dbAccessModulesProvider = dbAccessModulesProvider;
    this.userAccessModuleDao = userAccessModuleDao;
    this.userAccessModuleMapper = userAccessModuleMapper;
    this.userDao = userDao;
    this.userServiceAuditor = userServiceAuditor;
  }

  @Override
  public void updateAllBypassTimes(long userId) {
    dbAccessModulesProvider.get().stream()
        .filter(DbAccessModule::getBypassable)
        .forEach(module -> updateBypassTime(userId, module.getName(), true));
  }

  @Override
  public void updateBypassTime(long userId, AccessBypassRequest accessBypassRequest) {
    updateBypassTime(
        userId,
        accessModuleNameMapper.clientAccessModuleToStorage(accessBypassRequest.getModuleName()),
        accessBypassRequest.getIsBypassed());
  }

  @Override
  public void updateBypassTime(
      long userId, DbAccessModuleName accessModuleName, boolean isBypassed) {
    DbAccessModule accessModule =
        getDbAccessModuleOrThrow(dbAccessModulesProvider.get(), accessModuleName);
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
    userServiceAuditor.fireAdministrativeBypassTime(
        user.getUserId(),
        accessModuleNameMapper.bypassAuditPropertyFromStorage(accessModule.getName()),
        Optional.ofNullable(previousBypassTime).map(Timestamp::toInstant),
        Optional.ofNullable(newBypassTime).map(Timestamp::toInstant));
  }

  @Override
  public void updateCompletionTime(
      DbUser dbUser, DbAccessModuleName accessModuleName, @Nullable Timestamp timestamp) {
    DbAccessModule dbAccessModule =
        getDbAccessModuleOrThrow(dbAccessModulesProvider.get(), accessModuleName);
    DbUserAccessModule userAccessModuleToUpdate =
        retrieveUserAccessModuleOrCreate(dbUser, dbAccessModule);
    userAccessModuleDao.save(userAccessModuleToUpdate.setCompletionTime(timestamp));
  }

  @Override
  public List<AccessModuleStatus> getAccessModuleStatus(DbUser user) {
    return userAccessModuleDao.getAllByUser(user).stream()
        .map(this::maybeReturnAccessModule)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toList());
  }

  @Override
  public Optional<AccessModuleStatus> getAccessModuleStatus(
      DbUser user, DbAccessModuleName accessModuleName) {
    DbAccessModule dbAccessModule =
        getDbAccessModuleOrThrow(dbAccessModulesProvider.get(), accessModuleName);
    DbUserAccessModule userAccessModule = retrieveUserAccessModuleOrCreate(user, dbAccessModule);
    return maybeReturnAccessModule(userAccessModule);
  }

  private Optional<AccessModuleStatus> maybeReturnAccessModule(
      DbUserAccessModule dbUserAccessModule) {
    return Optional.of(dbUserAccessModule)
        .map(a -> userAccessModuleMapper.dbToModule(a, getExpirationTime(a).orElse(null)))
        .filter(a -> isModuleEnabledInEnvironment(a.getModuleName()));
  }

  @VisibleForTesting
  @Override
  public boolean isSignedDuccVersionCurrent(Integer signedVersion) {
    return configProvider.get().access.currentDuccVersions.contains(signedVersion);
  }

  @Override
  public boolean hasUserSignedACurrentDucc(DbUser targetUser) {
    final DbUserCodeOfConductAgreement duccAgreement = targetUser.getDuccAgreement();
    if (duccAgreement == null) {
      return false;
    }

    return isSignedDuccVersionCurrent(duccAgreement.getSignedVersion());
  }

  @Override
  public boolean isModuleCompliant(DbUser dbUser, DbAccessModuleName accessModuleName) {
    // if the module is not required, the user is always compliant
    if (!isModuleEnabledInEnvironment(
        accessModuleNameMapper.storageAccessModuleToClient(accessModuleName))) {
      return true;
    }

    DbAccessModule dbAccessModule =
        getDbAccessModuleOrThrow(dbAccessModulesProvider.get(), accessModuleName);
    DbUserAccessModule userAccessModule = retrieveUserAccessModuleOrCreate(dbUser, dbAccessModule);
    boolean isBypassed = dbAccessModule.getBypassable() && userAccessModule.getBypassTime() != null;
    boolean isCompleted = userAccessModule.getCompletionTime() != null;

    // we have an additional check before considering DUCC "complete"
    if (isCompleted && accessModuleName == DbAccessModuleName.DATA_USER_CODE_OF_CONDUCT) {
      isCompleted = hasUserSignedACurrentDucc(dbUser);
    }

    boolean isExpired =
        getExpirationTime(userAccessModule)
            .map(x -> x.before(new Timestamp(clock.millis())))
            .orElse(false);

    // A module is completed when it is bypassed OR (completed but not expired).
    return isBypassed || (isCompleted && !isExpired);
  }

  @Override
  public boolean isModuleBypassed(DbUser dbUser, DbAccessModuleName accessModuleName) {
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
            dbUserAccessModule.getCompletionTime(),
            configProvider.get().access.renewal.expiryDays));
  }

  private static DbAccessModule getDbAccessModuleOrThrow(
      List<DbAccessModule> dbAccessModules, DbAccessModuleName accessModuleName) {
    return dbAccessModules.stream()
        .filter(a -> a.getName() == accessModuleName)
        .findFirst()
        .orElseThrow(
            () ->
                new BadRequestException(
                    "There is no access module named: " + accessModuleName.toString()));
  }

  /**
   * Extracts module expiration time from completionTime and expiry days: completionTime plus
   * expiryDays in millseconds.
   */
  public static Timestamp deriveExpirationTimestamp(Timestamp completionTime, Long expiryDays) {
    Preconditions.checkNotNull(
        expiryDays, "expected value for config key access.renewal.expiryDays");
    long expiryDaysInMs = TimeUnit.MILLISECONDS.convert(expiryDays, TimeUnit.DAYS);
    return new Timestamp(completionTime.getTime() + expiryDaysInMs);
  }

  // Do we require this module's completion for users to be compliant with the appropriate tier(s)?
  private boolean isModuleEnabledInEnvironment(AccessModule module) {
    final AccessConfig accessConfig = configProvider.get().access;

    switch (module) {
      case ERA_COMMONS:
        return accessConfig.enableEraCommons;
        // RT Compliance training  and CT Compliance training modules are
        // controlled by the same feature flag COMPLIANCE_TRAINING
      case COMPLIANCE_TRAINING:
      case CT_COMPLIANCE_TRAINING:
        return accessConfig.enableComplianceTraining;
      case RAS_LINK_LOGIN_GOV:
        return accessConfig.enableRasLoginGovLinking;
      default:
        return true;
    }
  }
}
