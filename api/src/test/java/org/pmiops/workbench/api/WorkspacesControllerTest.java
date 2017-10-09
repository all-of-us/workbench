package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import javax.inject.Provider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.Workspace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
public class WorkspacesControllerTest {

  @Autowired
  WorkspaceDao workspaceDao;
  @Autowired
  CdrVersionDao cdrVersionDao;
  @Autowired
  UserDao userDao;
  @Mock
  Provider<User> userProvider;
  @Mock
  FireCloudService fireCloudService;

  private WorkspacesController workspacesController;

  private static final Instant NOW = Instant.now();
  private static final long NOW_TIME = Timestamp.from(NOW).getTime();

  @Before
  public void setUp() {
    User user = new User();
    user.setEmail("bob@gmail.com");
    user.setUserId(123L);
    user = userDao.save(user);
    when(userProvider.get()).thenReturn(user);

    this.workspacesController = new WorkspacesController(workspaceDao, cdrVersionDao,
        userProvider, fireCloudService, Clock.fixed(NOW, ZoneId.systemDefault()));
  }

  public Workspace createDefaultWorkspace() {
    ResearchPurpose researchPurpose = new ResearchPurpose();
    researchPurpose.setDiseaseFocusedResearch(true);
    researchPurpose.setDiseaseOfFocus("cancer");
    researchPurpose.setMethodsDevelopment(true);
    researchPurpose.setControlSet(true);
    researchPurpose.setAggregateAnalysis(true);
    researchPurpose.setAncestry(true);
    researchPurpose.setCommercialPurpose(true);
    researchPurpose.setPopulation(true);
    researchPurpose.setPopulationOfFocus("population");
    researchPurpose.setAdditionalNotes("additional notes");

    Workspace workspace = new Workspace();
    workspace.setName("name");
    workspace.setNamespace("namespace");
    workspace.setDescription("description");
    workspace.setDataAccessLevel(Workspace.DataAccessLevelEnum.PROTECTED);
    workspace.setResearchPurpose(researchPurpose);

    return workspace;
  }

  @Test
  public void testCreateWorkspace() throws Exception {
    Workspace workspace = createDefaultWorkspace();
    workspacesController.createWorkspace(workspace);
    verify(fireCloudService).createWorkspace("namespace", "name");

    Workspace workspace2 =
        workspacesController.getWorkspace("namespace", "name")
            .getBody();
    assertThat(workspace2.getCreationTime()).isEqualTo(NOW_TIME);
    assertThat(workspace2.getLastModifiedTime()).isEqualTo(NOW_TIME);
    assertThat(workspace2.getCdrVersionId()).isNull();
    assertThat(workspace2.getCreator()).isEqualTo("bob@gmail.com");
    assertThat(workspace2.getDataAccessLevel()).isEqualTo(Workspace.DataAccessLevelEnum.PROTECTED);
    assertThat(workspace2.getDescription()).isEqualTo("description");
    assertThat(workspace2.getId()).isEqualTo("name");
    assertThat(workspace2.getName()).isEqualTo("name");
    assertThat(workspace2.getResearchPurpose().getDiseaseFocusedResearch()).isTrue();
    assertThat(workspace2.getResearchPurpose().getDiseaseOfFocus()).isEqualTo("cancer");
    assertThat(workspace2.getResearchPurpose().getMethodsDevelopment()).isTrue();
    assertThat(workspace2.getResearchPurpose().getControlSet()).isTrue();
    assertThat(workspace2.getResearchPurpose().getAggregateAnalysis()).isTrue();
    assertThat(workspace2.getResearchPurpose().getAncestry()).isTrue();
    assertThat(workspace2.getResearchPurpose().getCommercialPurpose()).isTrue();
    assertThat(workspace2.getResearchPurpose().getPopulation()).isTrue();
    assertThat(workspace2.getResearchPurpose().getPopulationOfFocus()).isEqualTo("population");
    assertThat(workspace2.getResearchPurpose().getAdditionalNotes()).isEqualTo("additional notes");
    assertThat(workspace2.getNamespace()).isEqualTo("namespace");
  }
}
