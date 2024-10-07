package org.pmiops.workbench;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.FirecloudApiClientFactory;
import org.pmiops.workbench.google.StorageConfig;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.leonardo.LeonardoApiClientFactory;
import org.pmiops.workbench.leonardo.LeonardoApiClientImpl;
import org.pmiops.workbench.utils.mappers.LeonardoMapper;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.pmiops.workbench.workspaces.resources.UserRecentResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

public class NotebooksIntegrationTest extends BaseIntegrationTest {
  @Autowired private LeonardoApiClient leonardoApiClient;

  // Provide mock beans for dependencies of NotebooksServiceImpl (which is loaded as a bean within
  // this test due to the @ComponentScan on the o.p.w.notebooks package.
  @MockBean FireCloudService mockFireCloudService;
  @MockBean UserRecentResourceService mockUserRecentResourceService;
  @MockBean LeonardoMapper leonardoMapper;
  @MockBean WorkspaceDao workspaceDao;
  @MockBean WorkspaceAuthService workspaceAuthService;

  @TestConfiguration
  // N.B. in the other integration test classes we add a @ComponentScan which scans the package
  // where the class under test is defined. Adding that annotation
  @ComponentScan(basePackageClasses = LeonardoApiClientImpl.class)
  @Import({
    FirecloudApiClientFactory.class,
    LeonardoApiClientImpl.class,
    LeonardoApiClientFactory.class,
    StorageConfig.class,
    BaseIntegrationTest.Configuration.class
  })
  static class Configuration {}

  @Test
  public void testStatus() {
    assertThat(leonardoApiClient.getLeonardoStatus()).isTrue();
  }
}
