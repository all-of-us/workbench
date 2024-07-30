package org.pmiops.workbench.featuredworkspace;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.access.AccessTierServiceImpl;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.FeaturedWorkspaceDao;
import org.pmiops.workbench.db.model.DbFeaturedWorkspace;
import org.pmiops.workbench.db.model.DbFeaturedWorkspace.DbFeaturedCategory;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.FeaturedWorkspaceCategory;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceResponse;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceResponse;
import org.pmiops.workbench.utils.mappers.FeaturedWorkspaceMapper;
import org.pmiops.workbench.utils.mappers.WorkspaceMapper;
import org.pmiops.workbench.workspaceadmin.WorkspaceAdminServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@DataJpaTest
public class FeaturedWorkspaceTest {

  @MockBean private FeaturedWorkspaceDao mockFeaturedWorkspaceDao;
  @MockBean private FeaturedWorkspaceMapper mockFeaturedWorkspaceMapper;
  @MockBean private FireCloudService mockFireCloudService;
  @MockBean private WorkspaceMapper mockWorkspaceMapper;

  @Autowired private FeaturedWorkspaceService featuredWorkspaceService;

  private DbWorkspace dbWorkspace;

  @TestConfiguration
  @Import({
    FakeClockConfiguration.class,
    FeaturedWorkspaceServiceImpl.class,
  })
  @MockBean({AccessTierServiceImpl.class, WorkspaceAdminServiceImpl.class})
  static class Configuration {
    @Bean
    public WorkbenchConfig getConfig() {
      return WorkbenchConfig.createEmptyConfig();
    }
  }

  @BeforeEach
  public void setUp() {
    dbWorkspace = new DbWorkspace().setWorkspaceNamespace("ns").setName("name");
    when(mockFeaturedWorkspaceMapper.toFeaturedWorkspaceCategory(
            DbFeaturedCategory.TUTORIAL_WORKSPACES))
        .thenReturn(FeaturedWorkspaceCategory.TUTORIAL_WORKSPACES);
  }

  @Test
  public void testFeaturedCategory_EmptyIfWorkspaceDoesntExist() {
    assertThat(featuredWorkspaceService.getFeaturedCategory(dbWorkspace)).isEmpty();
  }

  @Test
  public void testFeaturedCategory_getFeaturedCategoryForFeaturedWorkspace() {
    DbFeaturedWorkspace dbFeaturedWorkspace =
        new DbFeaturedWorkspace()
            .setWorkspace(dbWorkspace)
            .setCategory(DbFeaturedCategory.TUTORIAL_WORKSPACES);

    when(mockFeaturedWorkspaceDao.findByWorkspace(dbWorkspace))
        .thenReturn(Optional.of(dbFeaturedWorkspace));
    assertThat(featuredWorkspaceService.getFeaturedCategory(dbWorkspace))
        .hasValue(FeaturedWorkspaceCategory.TUTORIAL_WORKSPACES);
  }

  void mockFeaturedWorkspaces(String namespace, DbFeaturedCategory dbFeaturedCategory) {
    DbWorkspace mockdbWorkspace =
        new DbWorkspace().setWorkspaceNamespace(namespace).setWorkspaceId(1);

    DbFeaturedWorkspace dbFeaturedWorkspace =
        new DbFeaturedWorkspace().setWorkspace(mockdbWorkspace).setCategory(dbFeaturedCategory);

    RawlsWorkspaceResponse rawlsWorkspaceResponse = new RawlsWorkspaceResponse();
    rawlsWorkspaceResponse.workspace(new RawlsWorkspaceDetails().workspaceId(namespace));
    when(mockFireCloudService.getWorkspace(
            mockdbWorkspace.getWorkspaceNamespace(), mockdbWorkspace.getFirecloudName()))
        .thenReturn(rawlsWorkspaceResponse);

    when(mockFeaturedWorkspaceMapper.toDbFeaturedCategory(
            FeaturedWorkspaceCategory.valueOf(dbFeaturedCategory.toString())))
        .thenReturn(dbFeaturedCategory);
    when(mockFeaturedWorkspaceDao.findDbFeaturedWorkspacesByCategory(dbFeaturedCategory))
        .thenReturn(Optional.of(Collections.singletonList(dbFeaturedWorkspace)));
    Workspace mockWorkspace =
        new Workspace()
            .namespace(namespace)
            .featuredCategory(FeaturedWorkspaceCategory.valueOf(dbFeaturedCategory.toString()));
    when(mockWorkspaceMapper.toApiWorkspace(mockdbWorkspace, rawlsWorkspaceResponse.getWorkspace()))
        .thenReturn(mockWorkspace);
  }

  @Test
  public void testGetByFeaturedCategory() {
    mockFeaturedWorkspaces("Tutorial_namespace", DbFeaturedCategory.TUTORIAL_WORKSPACES);
    mockFeaturedWorkspaces("Phenotype_namespace", DbFeaturedCategory.PHENOTYPE_LIBRARY);
    mockFeaturedWorkspaces("Demo_namespace", DbFeaturedCategory.DEMO_PROJECTS);
    mockFeaturedWorkspaces("Community_namespace", DbFeaturedCategory.COMMUNITY);

    List<WorkspaceResponse> workspaceResponsesList =
        featuredWorkspaceService.getWorkspaceResponseByFeaturedCategory(
            FeaturedWorkspaceCategory.TUTORIAL_WORKSPACES);
    assertThat(workspaceResponsesList.size()).isEqualTo(1);
    assertThat(workspaceResponsesList.get(0).getWorkspace().getNamespace())
        .isEqualTo("Tutorial_namespace");

    workspaceResponsesList =
        featuredWorkspaceService.getWorkspaceResponseByFeaturedCategory(
            FeaturedWorkspaceCategory.PHENOTYPE_LIBRARY);
    assertThat(workspaceResponsesList.size()).isEqualTo(1);
    assertThat(workspaceResponsesList.get(0).getWorkspace().getNamespace())
        .isEqualTo("Phenotype_namespace");

    workspaceResponsesList =
        featuredWorkspaceService.getWorkspaceResponseByFeaturedCategory(
            FeaturedWorkspaceCategory.DEMO_PROJECTS);
    assertThat(workspaceResponsesList.size()).isEqualTo(1);
    assertThat(workspaceResponsesList.get(0).getWorkspace().getNamespace())
        .isEqualTo("Demo_namespace");

    workspaceResponsesList =
        featuredWorkspaceService.getWorkspaceResponseByFeaturedCategory(
            FeaturedWorkspaceCategory.COMMUNITY);
    assertThat(workspaceResponsesList.size()).isEqualTo(1);
    assertThat(workspaceResponsesList.get(0).getWorkspace().getNamespace())
        .isEqualTo("Community_namespace");
  }

  @Test
  public void testGetByFeaturedCategory_none() {
    when(mockFeaturedWorkspaceDao.findDbFeaturedWorkspacesByCategory(
            DbFeaturedCategory.TUTORIAL_WORKSPACES))
        .thenReturn(Optional.empty());
    List<WorkspaceResponse> workspaceResponsesList =
        featuredWorkspaceService.getWorkspaceResponseByFeaturedCategory(
            FeaturedWorkspaceCategory.TUTORIAL_WORKSPACES);
    assertThat(workspaceResponsesList.size()).isEqualTo(0);
  }
}
