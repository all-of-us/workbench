package org.pmiops.workbench.featuredworkspace;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

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
}