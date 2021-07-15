package org.pmiops.workbench.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.api.services.cloudresourcemanager.model.Project;
import com.google.api.services.cloudresourcemanager.model.ResourceId;
import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.SpringTest;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.google.CloudResourceManagerService;
import org.pmiops.workbench.model.AuditProjectAccessRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class CloudTaskUserControllerTest extends SpringTest {
  @Autowired private CloudResourceManagerService mockCloudResourceManagerService;

  private long incrementedUserId = 1L;
  private DbUser userA;
  private DbUser userB;

  @Autowired private CloudTaskUserController controller;

  @TestConfiguration
  @Import({CloudTaskUserController.class})
  @MockBean({CloudResourceManagerService.class})
  static class Configuration {}

  @BeforeEach
  public void setUp() {
    incrementedUserId = 1L;
    userA = createUser("a@fake-research-aou.org");
    userB = createUser("b@fake-research-aou.org");
  }

  private DbUser createUser(String email) {
    DbUser user = new DbUser();
    user.setUsername(email);
    user.setUserId(incrementedUserId);
    incrementedUserId++;
    return user;
  }

  @Test
  public void testBulkProjectAudit() throws Exception {
    doReturn(ImmutableList.of()).when(mockCloudResourceManagerService).getAllProjectsForUser(userA);
    doReturn(
            ImmutableList.of(
                new Project()
                    .setName("aou-rw-test-123")
                    .setParent(new ResourceId().setType("folder").setId("123"))))
        .when(mockCloudResourceManagerService)
        .getAllProjectsForUser(userB);
    controller.auditProjectAccess(
        new AuditProjectAccessRequest()
            .addUserIdsItem(userA.getUserId())
            .addUserIdsItem(userB.getUserId()));
    verify(mockCloudResourceManagerService, times(2)).getAllProjectsForUser(any());
  }
}
