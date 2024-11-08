package org.pmiops.workbench.featuredworkspace;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.billing.InitialCreditsService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.FeaturedWorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbFeaturedWorkspace;
import org.pmiops.workbench.db.model.DbFeaturedWorkspace.DbFeaturedCategory;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.FeaturedWorkspaceCategory;
import org.pmiops.workbench.model.WorkspaceResponse;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceAccessLevel;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceListResponse;
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
  private List<RawlsWorkspaceListResponse> rawlsWorkspaces;

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
    InitialCreditsService.class,
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
    rawlsWorkspaces = new ArrayList<>();
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

  void mockFeaturedWorkspaces(
      String namespace, DbFeaturedCategory dbFeaturedCategory, String firecloudUuid) {
    DbWorkspace mockDbWorkspace =
        new DbWorkspace()
            .setWorkspaceNamespace(namespace)
            .setWorkspaceId(1)
            .setFirecloudUuid(firecloudUuid);

    DbFeaturedWorkspace dbFeaturedWorkspace =
        new DbFeaturedWorkspace().setWorkspace(mockDbWorkspace).setCategory(dbFeaturedCategory);

    when(mockFeaturedWorkspaceDao.findDbFeaturedWorkspacesByCategory(dbFeaturedCategory))
        .thenReturn(Collections.singletonList(dbFeaturedWorkspace));

    rawlsWorkspaces.add(
        new RawlsWorkspaceListResponse()
            .workspace(new RawlsWorkspaceDetails().workspaceId(firecloudUuid))
            .accessLevel(RawlsWorkspaceAccessLevel.OWNER));

    when(mockFireCloudService.getWorkspaces()).thenReturn(rawlsWorkspaces);
  }

  @Test
  public void testGetByFeaturedCategory() {
    mockFeaturedWorkspaces("Tutorial_namespace", DbFeaturedCategory.TUTORIAL_WORKSPACES, "one");
    mockFeaturedWorkspaces("Phenotype_namespace", DbFeaturedCategory.PHENOTYPE_LIBRARY, "two");
    mockFeaturedWorkspaces("Demo_namespace", DbFeaturedCategory.DEMO_PROJECTS, "three");
    mockFeaturedWorkspaces("Community_namespace", DbFeaturedCategory.COMMUNITY, "four");

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

  @Test
  public void testGetByFeaturedCategory_inaccessible() {
    mockFeaturedWorkspaces("Tutorial_namespace", DbFeaturedCategory.TUTORIAL_WORKSPACES, "one");

    // override the mock so that FC getWorkspaces() does not return the workspace
    // which simulates inaccessibility to my user

    when(mockFireCloudService.getWorkspaces()).thenReturn(Collections.emptyList());

    List<WorkspaceResponse> workspaceResponsesList =
        assertDoesNotThrow(
            () ->
                featuredWorkspaceService.getWorkspaceResponseByFeaturedCategory(
                    FeaturedWorkspaceCategory.TUTORIAL_WORKSPACES));

    assertThat(workspaceResponsesList).isEmpty();
  }

  @Test
  public void testGetByFeaturedCategory_noaccess() {
    String namespace = "Tutorial_namespace";
    mockFeaturedWorkspaces(namespace, DbFeaturedCategory.TUTORIAL_WORKSPACES, "one");

    // override the mock so that FC getWorkspaces() returns the workspace with NO_ACCESS
    // which simulates that my user is not in the workspace's tier auth domain

    var workspaceWithoutAccess =
        rawlsWorkspaces.stream()
            .map(ws -> ws.accessLevel(RawlsWorkspaceAccessLevel.NO_ACCESS))
            .toList();

    when(mockFireCloudService.getWorkspaces()).thenReturn(workspaceWithoutAccess);

    List<WorkspaceResponse> workspaceResponsesList =
        assertDoesNotThrow(
            () ->
                featuredWorkspaceService.getWorkspaceResponseByFeaturedCategory(
                    FeaturedWorkspaceCategory.TUTORIAL_WORKSPACES));

    assertThat(workspaceResponsesList.size()).isEqualTo(1);
    assertThat(workspaceResponsesList.get(0).getWorkspace().getNamespace()).isEqualTo(namespace);
  }
}
