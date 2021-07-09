package org.pmiops.workbench.access;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditor;
import org.pmiops.workbench.actionaudit.targetproperties.BypassTimeTargetProperty;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserAccessModuleDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbAccessModule;
import org.pmiops.workbench.db.model.DbAccessModule.AccessModuleName;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserAccessModule;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.model.AccessModule;
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

  private static final BiMap<AccessModule, AccessModuleName> CLIENT_TO_STORAGE_ACCESS_MODULE =
      ImmutableBiMap.<AccessModule, AccessModuleName>builder()
          .put(AccessModule.TWO_FACTOR_AUTH, AccessModuleName.TWO_FACTOR_AUTH)
          .put(AccessModule.ERA_COMMONS, AccessModuleName.ERA_COMMONS)
          .put(AccessModule.COMPLIANCE_TRAINING, AccessModuleName.RT_COMPLIANCE_TRAINING)
          .put(AccessModule.RAS_LINK_LOGIN_GOV, AccessModuleName.RAS_LOGIN_GOV)
          .put(AccessModule.DATA_USE_AGREEMENT, AccessModuleName.DATA_USER_CODE_OF_CONDUCT)
          .put(AccessModule.PUBLICATION_CONFIRMATION, AccessModuleName.PUBLICATION_CONFIRMATION)
          .put(AccessModule.PROFILE_CONFIRMATION, AccessModuleName.PROFILE_CONFIRMATION)
          .build();

  private static final BiMap<BypassTimeTargetProperty, AccessModuleName>
      AUDIT_TO_STORAGE_ACCESS_MODULE =
          ImmutableBiMap.<BypassTimeTargetProperty, AccessModuleName>builder()
              .put(BypassTimeTargetProperty.ERA_COMMONS_BYPASS_TIME, AccessModuleName.ERA_COMMONS)
              .put(
                  BypassTimeTargetProperty.COMPLIANCE_TRAINING_BYPASS_TIME,
                  AccessModuleName.RT_COMPLIANCE_TRAINING)
              .put(
                  BypassTimeTargetProperty.TWO_FACTOR_AUTH_BYPASS_TIME,
                  AccessModuleName.TWO_FACTOR_AUTH)
              .put(BypassTimeTargetProperty.RAS_LINK_LOGIN_GOV, AccessModuleName.RAS_LOGIN_GOV)
              .put(
                  BypassTimeTargetProperty.DATA_USE_AGREEMENT_BYPASS_TIME,
                  AccessModuleName.DATA_USER_CODE_OF_CONDUCT)
              .build();

  @Autowired
  public AccessModuleServiceImpl(
      Provider<List<DbAccessModule>> dbAccessModulesProvider,
      Clock clock,
      UserAccessModuleDao userAccessModuleDao,
      UserDao userDao,
      UserServiceAuditor userServiceAuditor,
      Provider<WorkbenchConfig> configProvider) {
    this.dbAccessModulesProvider = dbAccessModulesProvider;
    this.clock = clock;
    this.userAccessModuleDao = userAccessModuleDao;
    this.userDao = userDao;
    this.userServiceAuditor = userServiceAuditor;
    this.configProvider = configProvider;
  }

  @Override
  public void updateBypassTime(long userId, AccessModule accessModuleName, boolean isBypassed) {
    DbAccessModule accessModule =
        dbAccessModulesProvider.get().stream()
            .filter(a -> a.getName() == clientAccessModuleToStorage(accessModuleName))
            .findFirst()
            .orElseThrow(
                () ->
                    new BadRequestException(
                        "There is no access module named: " + accessModuleName.toString()));
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
            "Bypassing %s(uid: %d) for module %s",
            user.getContactEmail(), userId, accessModule.getName()));
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

  private static AccessModuleName clientAccessModuleToStorage(AccessModule s) {
    return CLIENT_TO_STORAGE_ACCESS_MODULE.get(s);
  }

  private static BypassTimeTargetProperty auditAccessModuleFromStorage(AccessModuleName s) {
    return AUDIT_TO_STORAGE_ACCESS_MODULE.inverse().get(s);
  }
}
