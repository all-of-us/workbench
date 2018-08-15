package org.pmiops.workbench.db.model;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class WorkspaceUserRoleTest {
  @Test
  public void testWorkspaceAccessLevelConversion() {
    for (WorkspaceAccessLevel level : WorkspaceAccessLevel.values()) {
      Short storageLevel = WorkspaceUserRole.accessLevelToStorage(level);
      assertThat(storageLevel).isNotNull();
      assertThat(level).isEqualTo(WorkspaceUserRole.accessLevelFromStorage(storageLevel));
    }
  }
}
