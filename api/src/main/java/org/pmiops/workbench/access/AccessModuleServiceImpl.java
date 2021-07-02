package org.pmiops.workbench.access;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.UserAccessTierDao;
import org.pmiops.workbench.db.model.DbAccessModule;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbUserAccessTier;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.AccessBypassRequest;
import org.pmiops.workbench.model.TierAccessStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccessModuleServiceImpl implements AccessModuleService {
  private final Provider<WorkbenchConfig> configProvider;
  private final Clock clock;

  private final AccessTierDao accessTierDao;
  private final UserAccessTierDao userAccessTierDao;

  private final FireCloudService fireCloudService;

  private static final Logger log = Logger.getLogger(AccessModuleServiceImpl.class.getName());

  @Autowired
  public AccessModuleServiceImpl(
      Provider<WorkbenchConfig> configProvider,
      Provider<List<DbAccessModule>> accessModules,
      Clock clock,
      AccessTierDao accessTierDao,
      UserAccessTierDao userAccessTierDao,
      FireCloudService fireCloudService) {
    this.configProvider = configProvider;
    this.clock = clock;
    this.accessTierDao = accessTierDao;
    this.userAccessTierDao = userAccessTierDao;
    this.fireCloudService = fireCloudService;
  }

  @Override
  public void updateBypassTime(long userDatabaseId, AccessBypassRequest accessBypassRequest) {

  }
  private Timestamp now() {
    return new Timestamp(clock.instant().toEpochMilli());
  }

}
