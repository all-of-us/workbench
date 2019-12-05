package org.pmiops.workbench.notebooks;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Clock;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class NotebooksServiceImplTest {

  @TestConfiguration
  @Import({NotebooksServiceImpl.class})
  @MockBean({
    CloudStorageService.class,
    FireCloudService.class,
    WorkspaceService.class,
    UserRecentResourceService.class
  })
  static class Configuration {

    @Bean
    Clock clock() {
      return new FakeClock();
    }

    @Bean
    DbUser user() {
      return null;
    }
  }

  @Autowired private NotebooksService notebooksService;
  @Autowired private FireCloudService firecloudService;
  @Autowired private CloudStorageService cloudStorageService;

  @Test
  public void testGetReadOnlyHtml_basicContent() {
    when(firecloudService.getWorkspace(any(), any()))
        .thenReturn(
            new FirecloudWorkspaceResponse().workspace(new FirecloudWorkspace().bucketName("bkt")));
    when(cloudStorageService.getFileAsJson(any(), any())).thenReturn(new JSONObject());
    when(firecloudService.staticNotebooksConvert(any()))
        .thenReturn("<html><body><div>asdf</div></body></html>");

    String html = new String(notebooksService.getReadOnlyHtml("", "", "").getBytes());
    assertThat(html).contains("div");
    assertThat(html).contains("asdf");
  }

  @Test
  public void testGetReadOnlyHtml_scriptSanitization() {
    when(firecloudService.getWorkspace(any(), any()))
        .thenReturn(
            new FirecloudWorkspaceResponse().workspace(new FirecloudWorkspace().bucketName("bkt")));
    when(cloudStorageService.getFileAsJson(any(), any())).thenReturn(new JSONObject());
    when(firecloudService.staticNotebooksConvert(any()))
        .thenReturn("<html><script>window.alert('hacked');</script></html>");

    String html = new String(notebooksService.getReadOnlyHtml("", "", "").getBytes());
    assertThat(html).doesNotContain("script");
    assertThat(html).doesNotContain("alert");
  }

  @Test
  public void testGetReadOnlyHtml_styleSanitization() {
    when(firecloudService.getWorkspace(any(), any()))
        .thenReturn(
            new FirecloudWorkspaceResponse().workspace(new FirecloudWorkspace().bucketName("bkt")));
    when(cloudStorageService.getFileAsJson(any(), any())).thenReturn(new JSONObject());
    when(firecloudService.staticNotebooksConvert(any()))
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
    when(firecloudService.getWorkspace(any(), any()))
        .thenReturn(
            new FirecloudWorkspaceResponse().workspace(new FirecloudWorkspace().bucketName("bkt")));
    when(cloudStorageService.getFileAsJson(any(), any())).thenReturn(new JSONObject());

    String dataUri = "data:image/png;base64,MTIz";
    when(firecloudService.staticNotebooksConvert(any()))
        .thenReturn("<img src=\"" + dataUri + "\" />\n");

    String html = new String(notebooksService.getReadOnlyHtml("", "", "").getBytes());
    assertThat(html).contains(dataUri);
  }

  @Test
  public void testGetReadOnlyHtml_disallowsRemoteImage() {
    when(firecloudService.getWorkspace(any(), any()))
        .thenReturn(
            new FirecloudWorkspaceResponse().workspace(new FirecloudWorkspace().bucketName("bkt")));
    when(cloudStorageService.getFileAsJson(any(), any())).thenReturn(new JSONObject());
    when(firecloudService.staticNotebooksConvert(any()))
        .thenReturn("<img src=\"https://eviltrackingpixel.com\" />\n");

    String html = new String(notebooksService.getReadOnlyHtml("", "", "").getBytes());
    assertThat(html).doesNotContain("eviltrackingpixel.com");
  }
}
