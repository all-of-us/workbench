package org.pmiops.workbench.api;

import static org.mockito.Mockito.when;
import static com.google.common.truth.Truth.assertThat;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.model.Workspace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import javax.inject.Provider;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
public class WorkspaceControllerTest {

  @Autowired
  WorkspaceDao workspaceDao;
  @Autowired
  CdrVersionDao cdrVersionDao;
  @Autowired
  UserDao userDao;
  @Mock
  Provider<User> userProvider;

  private WorkspaceController workspaceController;

  private static final Instant NOW = Instant.now();
  private static final DateTime NOW_TIME = new DateTime(Timestamp.from(NOW), DateTimeZone.UTC);

  @Before
  public void setUp() {
    User user = new User();
    user.setEmail("bob@gmail.com");
    user.setUserId(123L);
    user = userDao.save(user);
    when(userProvider.get()).thenReturn(user);

    this.workspaceController = new WorkspaceController(workspaceDao, cdrVersionDao,
        userProvider, Clock.fixed(NOW, ZoneId.systemDefault()));
  }

  @Test
  public void testCreateWorkspace() throws Exception {
    Workspace workspace = new Workspace();
    workspace.setName("name");
    workspace.setDescription("description");
    workspace.setDataAccessLevel(Workspace.DataAccessLevelEnum.PROTECTED);
    workspaceController.createWorkspace(workspace);

    Workspace workspace2 =
        workspaceController.getWorkspace("allofus-name", "name").getBody();
    assertThat(workspace2.getCreationTime()).isEqualTo(NOW_TIME);
    assertThat(workspace2.getLastModifiedTime()).isEqualTo(NOW_TIME);
    assertThat(workspace2.getCdrVersionId()).isNull();
    assertThat(workspace2.getCreator()).isEqualTo("bob@gmail.com");
    assertThat(workspace2.getDataAccessLevel()).isEqualTo(Workspace.DataAccessLevelEnum.PROTECTED);
    assertThat(workspace2.getDescription()).isEqualTo("description");
    assertThat(workspace2.getId()).isEqualTo("name");
    assertThat(workspace2.getName()).isEqualTo("name");
    assertThat(workspace2.getNamespace()).isEqualTo("allofus-name");
  }
}
