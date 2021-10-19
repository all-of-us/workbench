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
    for (final AccessModule original : AccessModule.values()) {
      final AccessModuleName converted = clientAccessModuleToStorage(original);
      assertWithMessage(original.toString()).that(converted).isNotNull();
      final AccessModule roundTrip = storageAccessModuleToClient(converted);
      assertWithMessage(converted.toString()).that(roundTrip).isEqualTo(original);
    }
  }

  @Test
  public void test_storageAccessModuleToClient() {
    for (final AccessModuleName original : AccessModuleName.values()) {
      final AccessModule converted = storageAccessModuleToClient(original);
      assertWithMessage(original.toString()).that(converted).isNotNull();
      final AccessModuleName roundTrip = clientAccessModuleToStorage(converted);
      assertWithMessage(converted.toString()).that(roundTrip).isEqualTo(original);
    }
  }

  @Test
  public void test_auditAccessModuleToStorage() {
    for (final BypassTimeTargetProperty original : BypassTimeTargetProperty.values()) {
      final AccessModuleName converted = auditAccessModuleToStorage(original);
      assertWithMessage(original.toString()).that(converted).isNotNull();
      final BypassTimeTargetProperty roundTrip = auditAccessModuleFromStorage(converted);
      assertWithMessage(converted.toString()).that(roundTrip).isEqualTo(original);
    }
  }

  @Test
  public void test_auditAccessModuleFromStorage() {
    // not all modules can be bypassed; don't check those
    DEFAULT_ACCESS_MODULES.stream()
        .filter(DbAccessModule::getBypassable)
        .map(DbAccessModule::getName)
        .forEach(
            original -> {
              final BypassTimeTargetProperty converted = auditAccessModuleFromStorage(original);
              assertWithMessage(original.toString()).that(converted).isNotNull();
              final AccessModuleName roundTrip = auditAccessModuleToStorage(converted);
              assertWithMessage(converted.toString()).that(roundTrip).isEqualTo(original);
            });
  }
}
