package org.pmiops.workbench.access;

import static com.google.common.truth.Truth.assertWithMessage;
import static org.pmiops.workbench.access.AccessUtils.auditAccessModuleFromStorage;
import static org.pmiops.workbench.access.AccessUtils.auditAccessModuleToStorage;
import static org.pmiops.workbench.access.AccessUtils.clientAccessModuleToStorage;
import static org.pmiops.workbench.access.AccessUtils.storageAccessModuleToClient;
import static org.pmiops.workbench.utils.TestMockFactory.DEFAULT_ACCESS_MODULES;

import org.junit.jupiter.api.Test;
import org.pmiops.workbench.actionaudit.targetproperties.BypassTimeTargetProperty;
import org.pmiops.workbench.db.model.DbAccessModule;
import org.pmiops.workbench.db.model.DbAccessModule.AccessModuleName;
import org.pmiops.workbench.model.AccessModule;

/**
 * See org.pmiops.workbench.db.model.StorageEnumsTest for the inspiration for this test.
 *
 * <p>Ensure that all mappings in AccessUtils cover all enum values.
 */
public class AccessUtilsTest {
  @Test
  public void test_clientAccessModuleToStorage() {
    for (final AccessModule am : AccessModule.values()) {
      final AccessModuleName amn = clientAccessModuleToStorage(am);
      assertWithMessage(am.toString()).that(amn).isNotNull();
      final AccessModule roundTrip = storageAccessModuleToClient(amn);
      assertWithMessage(amn.toString()).that(roundTrip).isNotNull();
    }
  }

  @Test
  public void test_storageAccessModuleToClient() {
    for (final AccessModuleName amn : AccessModuleName.values()) {
      final AccessModule am = storageAccessModuleToClient(amn);
      assertWithMessage(amn.toString()).that(am).isNotNull();
      final AccessModuleName roundTrip = clientAccessModuleToStorage(am);
      assertWithMessage(am.toString()).that(roundTrip).isNotNull();
    }
  }

  @Test
  public void test_auditAccessModuleToStorage() {
    for (final BypassTimeTargetProperty bttp : BypassTimeTargetProperty.values()) {
      final AccessModuleName amn = auditAccessModuleToStorage(bttp);
      assertWithMessage(bttp.toString()).that(amn).isNotNull();
      final BypassTimeTargetProperty roundTrip = auditAccessModuleFromStorage(amn);
      assertWithMessage(amn.toString()).that(roundTrip).isNotNull();
    }
  }

  @Test
  public void test_auditAccessModuleFromStorage() {
    // not all modules can be bypassed; don't check those
    DEFAULT_ACCESS_MODULES.stream()
        .filter(DbAccessModule::getBypassable)
        .map(DbAccessModule::getName)
        .forEach(
            dam -> {
              final BypassTimeTargetProperty bttp = auditAccessModuleFromStorage(dam);
              assertWithMessage(dam.toString()).that(bttp).isNotNull();
              final AccessModuleName roundTrip = auditAccessModuleToStorage(bttp);
              assertWithMessage(bttp.toString()).that(roundTrip).isNotNull();
            });
  }
}
