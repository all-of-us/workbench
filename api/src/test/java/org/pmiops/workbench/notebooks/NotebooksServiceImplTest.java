package org.pmiops.workbench.notebooks;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.storage.Blob;
import java.time.Clock;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.FailedPreconditionException;
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

  @Mock private Blob mockBlob;
  @Autowired private NotebooksService notebooksService;
  @Autowired private FireCloudService firecloudService;
  @Autowired private CloudStorageService cloudStorageService;

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
    verify(firecloudService, never()).staticNotebooksConvert(any());
  }

  @Test
  public void testGetReadOnlyHtml_basicContent() {
    stubNotebookToJson();
    when(firecloudService.staticNotebooksConvert(any()))
        .thenReturn("<html><body><div>asdf</div></body></html>");

    String html = new String(notebooksService.getReadOnlyHtml("", "", "").getBytes());
    assertThat(html).contains("div");
    assertThat(html).contains("asdf");
  }

  @Test
  public void testGetReadOnlyHtml_scriptSanitization() {
    stubNotebookToJson();
    when(firecloudService.staticNotebooksConvert(any()))
        .thenReturn("<html><script>window.alert('hacked');</script></html>");

    String html = new String(notebooksService.getReadOnlyHtml("", "", "").getBytes());
    assertThat(html).doesNotContain("script");
    assertThat(html).doesNotContain("alert");
  }

  @Test
  public void testGetReadOnlyHtml_styleSanitization() {
    stubNotebookToJson();
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
    stubNotebookToJson();
    String dataUri = "data:image/png;base64,MTIz";
    when(firecloudService.staticNotebooksConvert(any()))
        .thenReturn("<img src=\"" + dataUri + "\" />\n");

    String html = new String(notebooksService.getReadOnlyHtml("", "", "").getBytes());
    assertThat(html).contains(dataUri);
  }

  @Test
  public void testGetReadOnlyHtml_disallowsRemoteImage() {
    stubNotebookToJson();
    when(firecloudService.staticNotebooksConvert(any()))
        .thenReturn("<img src=\"https://eviltrackingpixel.com\" />\n");

    String html = new String(notebooksService.getReadOnlyHtml("", "", "").getBytes());
    assertThat(html).doesNotContain("eviltrackingpixel.com");
  }

  private void stubNotebookToJson() {
    when(firecloudService.getWorkspace(any(), any()))
        .thenReturn(
            new FirecloudWorkspaceResponse().workspace(new FirecloudWorkspace().bucketName("bkt")));
    when(cloudStorageService.getBlob(any(), any())).thenReturn(mockBlob);
    when(cloudStorageService.readBlobAsJson(mockBlob)).thenReturn(new JSONObject());
  }
}
