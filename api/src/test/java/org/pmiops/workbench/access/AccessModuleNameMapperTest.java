package org.pmiops.workbench.access;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.compliance.ComplianceService.BadgeName;
import org.pmiops.workbench.db.model.DbAccessModule.DbAccessModuleName;
import org.pmiops.workbench.model.AccessModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@Import(AccessModuleNameMapperImpl.class)
@SpringJUnitConfig
public class AccessModuleNameMapperTest {
  @Autowired private AccessModuleNameMapper mapper;

  @Test
  public void testStorageToClientMapping() {
    for (DbAccessModuleName name : DbAccessModuleName.values()) {
      AccessModule clientName = mapper.storageAccessModuleToClient(name);
      assertThat(clientName).isNotNull();
      assertThat(mapper.clientAccessModuleToStorage(clientName)).isEqualTo(name);
    }
  }

  @Test
  public void testClientToStorageMapping() {
    for (AccessModule name : AccessModule.values()) {
      DbAccessModuleName dbName = mapper.clientAccessModuleToStorage(name);
      assertThat(dbName).isNotNull();
      assertThat(mapper.storageAccessModuleToClient(dbName)).isEqualTo(name);
    }
  }

  @Test
  public void testStorageToBypassPropertyMapping() {
    // these can't be bypassed, so they don't have a mapping
    List<DbAccessModuleName> unbypassable =
        ImmutableList.of(
            DbAccessModuleName.PROFILE_CONFIRMATION, DbAccessModuleName.PUBLICATION_CONFIRMATION);
    unbypassable.forEach(name -> assertThat(mapper.bypassAuditPropertyFromStorage(name)).isNull());

    assertThat(
            Arrays.stream(DbAccessModuleName.values())
                .filter(name -> !unbypassable.contains(name))
                .allMatch(name -> mapper.bypassAuditPropertyFromStorage(name) != null))
        .isTrue();
  }

  @Test
  public void testBadgeMapping() {
    for (BadgeName name : BadgeName.values()) {
      DbAccessModuleName dbName = mapper.moduleFromBadge(name);
      assertThat(dbName).isNotNull();
      assertThat(mapper.badgeFromModule(dbName)).isEqualTo(name);
    }
  }

  @Test
  public void testBadgesForComplianceOnly() {
    List<DbAccessModuleName> compliance =
        ImmutableList.of(
            DbAccessModuleName.RT_COMPLIANCE_TRAINING, DbAccessModuleName.CT_COMPLIANCE_TRAINING);

    for (DbAccessModuleName name : DbAccessModuleName.values()) {
      BadgeName badgeName = mapper.badgeFromModule(name);
      if (compliance.contains(name)) {
        assertThat(badgeName).isNotNull();
        assertThat(mapper.badgeFromModule(name)).isEqualTo(badgeName);
      } else {
        assertThat(badgeName).isNull();
      }
    }
  }
}
