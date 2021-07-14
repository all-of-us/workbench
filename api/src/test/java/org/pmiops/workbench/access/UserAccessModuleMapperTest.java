package org.pmiops.workbench.access;

import static com.google.common.truth.Truth.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.SpringTest;
import org.pmiops.workbench.db.model.DbAccessModule;
import org.pmiops.workbench.db.model.DbAccessModule.AccessModuleName;
import org.pmiops.workbench.db.model.DbUserAccessModule;
import org.pmiops.workbench.model.AccessModule;
import org.pmiops.workbench.model.AccessModuleStatus;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

@Import({UserAccessModuleMapperImpl.class, CommonMappers.class})
public class UserAccessModuleMapperTest extends SpringTest {
  @Autowired private UserAccessModuleMapper mapper;

  private static final Instant NOW = Instant.now();

  @Test
  public void testDbToModule() {
    Timestamp bypassTime = Timestamp.from(NOW.minusSeconds(100));
    Timestamp completionTime = Timestamp.from(NOW.minusSeconds(200));
    Timestamp expirationTime = Timestamp.from(NOW.minusSeconds(300));
    DbUserAccessModule dbUserAccessModule =
        new DbUserAccessModule()
            .setBypassTime(bypassTime)
            .setAccessModule(new DbAccessModule().setName(AccessModuleName.ERA_COMMONS))
            .setCompletionTime(completionTime);

    assertThat(mapper.dbToModule(dbUserAccessModule, expirationTime))
        .isEqualTo(
            new AccessModuleStatus()
                .moduleName(AccessModule.ERA_COMMONS)
                .bypassEpochMillis(bypassTime.getTime())
                .expirationEpochMillis(expirationTime.getTime())
                .completionEpochMillis(completionTime.getTime()));
  }

  @Test
  public void testDbToModule_withNullValue() {
    Timestamp bypassTime = null;
    Timestamp completionTime = Timestamp.from(NOW.minusSeconds(200));
    Timestamp expirationTime = null;
    DbUserAccessModule dbUserAccessModule =
        new DbUserAccessModule()
            .setBypassTime(bypassTime)
            .setAccessModule(new DbAccessModule().setName(AccessModuleName.ERA_COMMONS))
            .setCompletionTime(completionTime);

    assertThat(mapper.dbToModule(dbUserAccessModule, expirationTime))
        .isEqualTo(
            new AccessModuleStatus()
                .moduleName(AccessModule.ERA_COMMONS)
                .expirationEpochMillis(expirationTime.getTime())
                .completionEpochMillis(completionTime.getTime()));
  }
}
