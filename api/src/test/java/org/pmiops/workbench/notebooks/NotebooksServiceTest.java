package org.pmiops.workbench.notebooks;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.storage.Blob;
import java.time.Clock;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.FailedPreconditionException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.monitoring.MonitoringService;
import org.pmiops.workbench.monitoring.views.MonitoringViews;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class NotebooksServiceTest {
  private static final JSONObject NOTEBOOK_CONTENTS =
      new JSONObject().put("who", "I'm a notebook!");
  private static final String BUCKET_NAME = "notebook.bucket";
  private static final FirecloudWorkspaceResponse WORKSPACE_RESPONSE =
      new FirecloudWorkspaceResponse().workspace(new FirecloudWorkspace().bucketName(BUCKET_NAME));
  private static final String NOTEBOOK_NAME = "my first notebook";
  private static final String NAMESPACE_NAME = "namespace_name";
  private static final String WORKSPACE_NAME = "workspace_name";
  private static final DbWorkspace WORKSPACE = new DbWorkspace();
  private static final String PREVIOUS_NOTEBOOK = "previous notebook";

  private static DbUser DB_USER;

  @Autowired private MonitoringService mockMonitoringService;
  @Autowired private FireCloudService mockFirecloudService;
  @Autowired private CloudStorageService mockCloudStorageService;
  @Autowired private WorkspaceService mockWorkspaceService;

  @Autowired private NotebooksService notebooksService;

  @TestConfiguration
  @Import({NotebooksServiceImpl.class})
  @MockBean({
    CloudStorageService.class,
    FireCloudService.class,
    WorkspaceService.class,
    UserRecentResourceService.class,
    MonitoringService.class
  })
  static class Configuration {

    @Bean
    Clock clock() {
      return new FakeClock();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    DbUser getDbUser() {
      return DB_USER;
    }
  }

  @Before
  public void setup() {
    DB_USER = new DbUser();
    DB_USER.setUserId(101L);
    DB_USER.setEmail("panic@thedis.co");
  }

  @Mock private Blob mockBlob;

  @Test
  public void testGetReadOnlyHtml_tooBig() {
    when(mockBlob.getSize()).thenReturn(50L * 1000 * 1000); // 50MB
    stubNotebookToJson();

    try {
      notebooksService.getReadOnlyHtml("", "", "").getBytes();
      fail("expected 412 exception");
    } catch (FailedPreconditionException e) {
      // expected
    }
    verify(mockFirecloudService, never()).staticNotebooksConvert(any());
  }

  @Test
  public void testGetReadOnlyHtml_basicContent() {
    stubNotebookToJson();
    when(mockFirecloudService.staticNotebooksConvert(any()))
        .thenReturn("<html><body><div>asdf</div></body></html>");

    String html = new String(notebooksService.getReadOnlyHtml("", "", "").getBytes());
    assertThat(html).contains("div");
    assertThat(html).contains("asdf");
  }

  @Test
  public void testGetReadOnlyHtml_scriptSanitization() {
    stubNotebookToJson();
    when(mockFirecloudService.staticNotebooksConvert(any()))
        .thenReturn("<html><script>window.alert('hacked');</script></html>");

    String html = new String(notebooksService.getReadOnlyHtml("", "", "").getBytes());
    assertThat(html).doesNotContain("script");
    assertThat(html).doesNotContain("alert");
  }

  @Test
  public void testGetReadOnlyHtml_styleSanitization() {
    stubNotebookToJson();
    when(mockFirecloudService.staticNotebooksConvert(any()))
        .thenReturn(
            "<STYLE type=\"text/css\">BODY{background:url(\"javascript:alert('XSS')\")} div {color: 'red'}</STYLE>\n");

    String html = new String(notebooksService.getReadOnlyHtml("", "", "").getBytes());
    assertThat(html).contains("style");
    assertThat(html).contains("color");
    // This behavior is not desired, but this test is in place to enshrine current expected
    // behavior. Style tags can introduce vulnerabilities as demonstrated in the test case - we
    // expect that the only style tags produced in the preview are produced by nbconvert, and are
    // therefore safe. Ideally we would keep the style tag, but sanitize the contents.
    assertThat(html).contains("XSS");
  }

  @Test
  public void testGetReadOnlyHtml_allowsDataImage() {
    stubNotebookToJson();
    String dataUri = "data:image/png;base64,MTIz";
    when(mockFirecloudService.staticNotebooksConvert(any()))
        .thenReturn("<img src=\"" + dataUri + "\" />\n");

    String html = new String(notebooksService.getReadOnlyHtml("", "", "").getBytes());
    assertThat(html).contains(dataUri);
  }

  @Test
  public void testGetReadOnlyHtml_disallowsRemoteImage() {
    stubNotebookToJson();
    when(mockFirecloudService.staticNotebooksConvert(any()))
        .thenReturn("<img src=\"https://eviltrackingpixel.com\" />\n");

    String html = new String(notebooksService.getReadOnlyHtml("", "", "").getBytes());
    assertThat(html).doesNotContain("eviltrackingpixel.com");
  }

  @Test
  public void testSaveNotebook_firesMetric() {
    notebooksService.saveNotebook(BUCKET_NAME, NOTEBOOK_NAME, NOTEBOOK_CONTENTS);
    verify(mockMonitoringService).recordIncrement(MonitoringViews.NOTEBOOK_SAVE);
  }

  @Test
  public void testDeleteNotebook_firesMetric() {
    doReturn(WORKSPACE_RESPONSE).when(mockFirecloudService).getWorkspace(anyString(), anyString());
    doReturn(WORKSPACE).when(mockWorkspaceService).getRequired(anyString(), anyString());

    notebooksService.deleteNotebook(NAMESPACE_NAME, WORKSPACE_NAME, NOTEBOOK_NAME);
    verify(mockMonitoringService).recordIncrement(MonitoringViews.NOTEBOOK_DELETE);
  }

  @Test
  public void testCloneNotebook_firesMetric() {
    doReturn(WORKSPACE_RESPONSE).when(mockFirecloudService).getWorkspace(anyString(), anyString());
    doReturn(WORKSPACE).when(mockWorkspaceService).getRequired(anyString(), anyString());

    notebooksService.cloneNotebook(NAMESPACE_NAME, WORKSPACE_NAME, PREVIOUS_NOTEBOOK);
    verify(mockMonitoringService).recordIncrement(MonitoringViews.NOTEBOOK_CLONE);
  }

  private void stubNotebookToJson() {
    when(mockFirecloudService.getWorkspace(anyString(), anyString()))
        .thenReturn(
            new FirecloudWorkspaceResponse()
                .workspace(new FirecloudWorkspace().bucketName("bkt ")));
    when(mockBlob.getContent()).thenReturn("{}".getBytes());
    when(mockCloudStorageService.getBlob(anyString(), anyString())).thenReturn(mockBlob);
  }
}
