package org.pmiops.workbench.featuredworkspace;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.NoSuchElementException;
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
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceListResponse;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.FeaturedWorkspaceMapper;
import org.pmiops.workbench.utils.mappers.FirecloudMapperImpl;
import org.pmiops.workbench.utils.mappers.WorkspaceMapperImpl;
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

  @Autowired private FeaturedWorkspaceService featuredWorkspaceService;

  private DbWorkspace dbWorkspace;

  @TestConfiguration
  @Import({
    CommonMappers.class,
    FakeClockConfiguration.class,
    FeaturedWorkspaceServiceImpl.class,
    FirecloudMapperImpl.class,
    WorkspaceMapperImpl.class,
  })
  @MockBean({
    AccessTierServiceImpl.class,
    WorkspaceAdminServiceImpl.class,
  })
  static class Configuration {
    @Bean
    public WorkbenchConfig getConfig() {
      return WorkbenchConfig.createEmptyConfig();
    }
  }

  @BeforeEach
  public void setUp() {
    dbWorkspace = new DbWorkspace().setFirecloudUuid("fc-uuid-123");

    when(mockFeaturedWorkspaceMapper.toFeaturedWorkspaceCategory(
            DbFeaturedCategory.TUTORIAL_WORKSPACES))
        .thenReturn(FeaturedWorkspaceCategory.TUTORIAL_WORKSPACES);
  }

  @Test
  public void testIsFeaturedWorkspace_workspaceExist() {
    when(mockFeaturedWorkspaceDao.existsByWorkspace(dbWorkspace)).thenReturn(true);
    assertThat(featuredWorkspaceService.isFeaturedWorkspace(dbWorkspace)).isTrue();
  }

  @Test
  public void testIsFeaturedWorkspace_workspaceDoesNotExist() {
    when(mockFeaturedWorkspaceDao.existsByWorkspace(dbWorkspace)).thenReturn(false);
    assertThat(featuredWorkspaceService.isFeaturedWorkspace(dbWorkspace)).isFalse();
  }

  @Test
  public void testFeaturedCategory_ThrowExceptionIfWorkspaceDoesntExist() {
    NoSuchElementException exception =
        assertThrows(
            NoSuchElementException.class,
            () -> featuredWorkspaceService.getFeaturedCategory(dbWorkspace));

    assertThat(exception.getMessage()).isEqualTo("No value present");
  }

  @Test
  public void testFeaturedCategory_getFeaturedCategoryForFeaturedWorkspace() {
    DbFeaturedWorkspace dbFeaturedWorkspace =
        new DbFeaturedWorkspace()
            .setWorkspace(dbWorkspace)
            .setCategory(DbFeaturedWorkspace.DbFeaturedCategory.TUTORIAL_WORKSPACES);

    when(mockFeaturedWorkspaceDao.findByWorkspace(dbWorkspace))
        .thenReturn(Optional.of(dbFeaturedWorkspace));
    assertThat(featuredWorkspaceService.getFeaturedCategory(dbWorkspace))
        .isEqualTo(FeaturedWorkspaceCategory.TUTORIAL_WORKSPACES);
  }

  @Test
  public void testGetFeaturedWorkspaces() {
    var mockDbFeaturedWorkspace =
        new DbFeaturedWorkspace()
            .setWorkspace(dbWorkspace)
            .setCategory(DbFeaturedCategory.TUTORIAL_WORKSPACES);
    when(mockFeaturedWorkspaceDao.findAll()).thenReturn(List.of(mockDbFeaturedWorkspace));
    when(mockFeaturedWorkspaceDao.existsByWorkspace(dbWorkspace)).thenReturn(true);
    when(mockFeaturedWorkspaceDao.findByWorkspace(dbWorkspace))
        .thenReturn(Optional.of(mockDbFeaturedWorkspace));

    var mockRawlsWorkspace =
        new RawlsWorkspaceDetails().workspaceId(dbWorkspace.getFirecloudUuid());
    when(mockFireCloudService.getWorkspaces())
        .thenReturn(List.of(new RawlsWorkspaceListResponse().workspace(mockRawlsWorkspace)));

    var result = featuredWorkspaceService.getFeaturedWorkspaces();
    assertThat(result).hasSize(1);
    var resultWorkspace = result.get(0).getWorkspace();
    assertThat(resultWorkspace.getFeaturedCategory())
        .isEqualTo(FeaturedWorkspaceCategory.TUTORIAL_WORKSPACES);
  }
}
