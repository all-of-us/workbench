package org.pmiops.workbench.access;

import static com.google.common.truth.Truth.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
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
    assertThat(
            Arrays.stream(DbAccessModuleName.values())
                .allMatch(name -> mapper.bypassAuditPropertyFromStorage(name) != null))
        .isTrue();
  }
}
