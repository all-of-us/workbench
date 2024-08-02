package org.pmiops.workbench.featuredworkspace;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static org.mockito.Mockito.when;

import jakarta.inject.Provider;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.access.AccessTierServiceImpl;
import org.pmiops.workbench.config.FeaturedWorkspacesConfig;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.FeaturedWorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbFeaturedWorkspace;
import org.pmiops.workbench.db.model.DbFeaturedWorkspace.DbFeaturedCategory;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.FeaturedWorkspaceCategory;
import org.pmiops.workbench.utils.mappers.FeaturedWorkspaceMapper;
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
  @MockBean private FeaturedWorkspaceMapper featuredWorkspaceMapper;

  // Delete this once published flag is on
  @MockBean private Provider<FeaturedWorkspacesConfig> featuredWorkspacesConfigProvider;

  @Autowired private FeaturedWorkspaceService featuredWorkspaceService;

  private DbWorkspace dbWorkspace;

  @TestConfiguration
  @Import({
    FakeClockConfiguration.class,
    FeaturedWorkspaceServiceImpl.class,
  })
  @MockBean({
    AccessTierServiceImpl.class,
    WorkspaceAdminServiceImpl.class,
    // Delete this once enablePublishedWorkspacesViaDb is on
    FireCloudService.class,
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
    dbWorkspace = new DbWorkspace();
    when(featuredWorkspaceMapper.toFeaturedWorkspaceCategory(
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
            .setCategory(DbFeaturedWorkspace.DbFeaturedCategory.TUTORIAL_WORKSPACES);

    when(mockFeaturedWorkspaceDao.findByWorkspace(dbWorkspace))
        .thenReturn(Optional.of(dbFeaturedWorkspace));
    assertThat(featuredWorkspaceService.getFeaturedCategory(dbWorkspace))
        .hasValue(FeaturedWorkspaceCategory.TUTORIAL_WORKSPACES);
  }
}
