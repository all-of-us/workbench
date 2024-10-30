package org.pmiops.workbench.featuredworkspace;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.FeaturedWorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbFeaturedWorkspace;
import org.pmiops.workbench.db.model.DbFeaturedWorkspace.DbFeaturedCategory;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.initialcredits.InitialCreditsExpirationService;
import org.pmiops.workbench.model.FeaturedWorkspaceCategory;
import org.pmiops.workbench.model.WorkspaceResponse;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceAccessLevel;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceResponse;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.FeaturedWorkspaceMapperImpl;
import org.pmiops.workbench.utils.mappers.FirecloudMapperImpl;
import org.pmiops.workbench.utils.mappers.WorkspaceMapperImpl;
import org.pmiops.workbench.workspaceadmin.WorkspaceAdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@DataJpaTest
public class FeaturedWorkspaceTest {

  @MockBean private FeaturedWorkspaceDao mockFeaturedWorkspaceDao;
  @MockBean private FireCloudService mockFireCloudService;

  @Autowired private FeaturedWorkspaceService featuredWorkspaceService;

  private DbWorkspace dbWorkspace;

  @TestConfiguration
  @Import({
    CommonMappers.class,
    FakeClockConfiguration.class,
    FeaturedWorkspaceMapperImpl.class,
    FeaturedWorkspaceServiceImpl.class,
    FirecloudMapperImpl.class,
    WorkspaceMapperImpl.class,
  })
  @MockBean({
    AccessTierService.class,
    InitialCreditsExpirationService.class,
    WorkspaceAdminService.class,
    WorkspaceDao.class,
  })
  static class Configuration {
    @Bean
    public WorkbenchConfig getConfig() {
      return WorkbenchConfig.createEmptyConfig();
    }
  }

  @BeforeEach
  public void setUp() {
    dbWorkspace = new DbWorkspace().setWorkspaceNamespace("ns").setName("name");
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
    DbWorkspace mockDbWorkspace =
        new DbWorkspace().setWorkspaceNamespace(namespace).setWorkspaceId(1);

    DbFeaturedWorkspace dbFeaturedWorkspace =
        new DbFeaturedWorkspace().setWorkspace(mockDbWorkspace).setCategory(dbFeaturedCategory);

    RawlsWorkspaceResponse rawlsWorkspaceResponse = new RawlsWorkspaceResponse();
    rawlsWorkspaceResponse.workspace(new RawlsWorkspaceDetails().workspaceId("1"));
    rawlsWorkspaceResponse.accessLevel(RawlsWorkspaceAccessLevel.OWNER);
    when(mockFireCloudService.getWorkspace(
            mockDbWorkspace.getWorkspaceNamespace(), mockDbWorkspace.getFirecloudName()))
        .thenReturn(rawlsWorkspaceResponse);

    when(mockFeaturedWorkspaceDao.findDbFeaturedWorkspacesByCategory(dbFeaturedCategory))
        .thenReturn(Collections.singletonList(dbFeaturedWorkspace));
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
        .thenReturn(Collections.emptyList());
    List<WorkspaceResponse> workspaceResponsesList =
        featuredWorkspaceService.getWorkspaceResponseByFeaturedCategory(
            FeaturedWorkspaceCategory.TUTORIAL_WORKSPACES);
    assertThat(workspaceResponsesList.size()).isEqualTo(0);
  }
}
