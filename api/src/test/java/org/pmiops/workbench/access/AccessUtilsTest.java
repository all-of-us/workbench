package org.pmiops.workbench.access;

import static com.google.common.truth.Truth.assertWithMessage;
import static org.pmiops.workbench.access.AccessUtils.AUDIT_TO_STORAGE_ACCESS_MODULE;
import static org.pmiops.workbench.access.AccessUtils.CLIENT_TO_STORAGE_ACCESS_MODULE;
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
  public void test_CLIENT_TO_STORAGE_ACCESS_MODULE() {
    for (AccessModule m : AccessModule.values()) {
      assertWithMessage(m.toString()).that(CLIENT_TO_STORAGE_ACCESS_MODULE.get(m)).isNotNull();
    }
    for (AccessModuleName m : AccessModuleName.values()) {
      assertWithMessage(m.toString())
          .that(CLIENT_TO_STORAGE_ACCESS_MODULE.inverse().get(m))
          .isNotNull();
    }
  }

  @Test
  public void test_AUDIT_TO_STORAGE_ACCESS_MODULE() {
    for (BypassTimeTargetProperty b : BypassTimeTargetProperty.values()) {
      assertWithMessage(b.toString()).that(AUDIT_TO_STORAGE_ACCESS_MODULE.get(b)).isNotNull();
    }

    // not all modules can be bypassed; don't check those
    DEFAULT_ACCESS_MODULES.stream()
        .filter(DbAccessModule::getBypassable)
        .map(DbAccessModule::getName)
        .forEach(
            m ->
                assertWithMessage(m.toString())
                    .that(AUDIT_TO_STORAGE_ACCESS_MODULE.inverse().get(m))
                    .isNotNull());
  }
}
