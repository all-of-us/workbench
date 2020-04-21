package org.pmiops.workbench.workspaceadmin;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.model.AdminWorkspaceCloudStorageCounts;
import org.pmiops.workbench.notebooks.NotebooksService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
public class WorkspaceAdminServiceTest {
  @TestConfiguration
  @MockBean({
    CloudStorageService.class,
    FireCloudService.class,
    NotebooksService.class,
  })
  @Import(WorkspaceAdminServiceImpl.class)
  static class Configuration {}

  @Autowired FireCloudService mockFirecloudService;
  @Autowired NotebooksService mockNotebooksService;

  @Autowired WorkspaceAdminService workspaceAdminService;

  @Before
  public void setUp() {
    when(mockFirecloudService.getWorkspaceAsService(any(), any()))
        .thenReturn(
            new FirecloudWorkspaceResponse()
                .workspace(new FirecloudWorkspace().bucketName("bucket")));
  }

  @Test
  public void testGetAdminWorkspaceCloudStorageCounts() {
    AdminWorkspaceCloudStorageCounts resp =
        workspaceAdminService.getAdminWorkspaceCloudStorageCounts("foo", "bar");
    assertThat(resp)
        .isEqualTo(
            new AdminWorkspaceCloudStorageCounts()
                .nonNotebookFileCount(0)
                .notebookFileCount(0)
                .storageBytesUsed(0L));
    verify(mockNotebooksService, atLeastOnce()).getNotebooksAsService(any());

    // Regression check: the admin service should never call the end-user variants of these methods.
    verify(mockNotebooksService, never()).getNotebooks(any(), any());
    verify(mockFirecloudService, never()).getWorkspace(any(), any());
  }
}
