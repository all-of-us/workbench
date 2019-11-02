package org.pmiops.workbench.notebooks

import com.google.common.truth.Truth.assertThat
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.`when`

import java.time.Clock
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.pmiops.workbench.db.dao.UserRecentResourceService
import org.pmiops.workbench.db.model.User
import org.pmiops.workbench.firecloud.FireCloudService
import org.pmiops.workbench.firecloud.model.Workspace
import org.pmiops.workbench.firecloud.model.WorkspaceResponse
import org.pmiops.workbench.google.CloudStorageService
import org.pmiops.workbench.test.FakeClock
import org.pmiops.workbench.workspaces.WorkspaceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner::class)
class NotebooksServiceImplTest {

    @Autowired
    private val notebooksService: NotebooksService? = null
    @Autowired
    private val firecloudService: FireCloudService? = null
    @Autowired
    private val cloudStorageService: CloudStorageService? = null

    @TestConfiguration
    @Import(NotebooksServiceImpl::class)
    @MockBean(CloudStorageService::class, FireCloudService::class, WorkspaceService::class, UserRecentResourceService::class)
    internal class Configuration {

        @Bean
        fun clock(): Clock {
            return FakeClock()
        }

        @Bean
        fun user(): User? {
            return null
        }
    }

    @Test
    fun testGetReadOnlyHtml_basicContent() {
        `when`<Any>(firecloudService!!.getWorkspace(any(), any()))
                .thenReturn(WorkspaceResponse().workspace(Workspace().bucketName("bkt")))
        `when`(cloudStorageService!!.getFileAsJson(any(), any())).thenReturn(JSONObject())
        `when`(firecloudService.staticNotebooksConvert(any()))
                .thenReturn("<html><body><div>asdf</div></body></html>")

        val html = String(notebooksService!!.getReadOnlyHtml("", "", "").toByteArray())
        assertThat(html).contains("div")
        assertThat(html).contains("asdf")
    }

    @Test
    fun testGetReadOnlyHtml_scriptSanitization() {
        `when`<Any>(firecloudService!!.getWorkspace(any(), any()))
                .thenReturn(WorkspaceResponse().workspace(Workspace().bucketName("bkt")))
        `when`(cloudStorageService!!.getFileAsJson(any(), any())).thenReturn(JSONObject())
        `when`(firecloudService.staticNotebooksConvert(any()))
                .thenReturn("<html><script>window.alert('hacked');</script></html>")

        val html = String(notebooksService!!.getReadOnlyHtml("", "", "").toByteArray())
        assertThat(html).doesNotContain("script")
        assertThat(html).doesNotContain("alert")
    }

    @Test
    fun testGetReadOnlyHtml_styleSanitization() {
        `when`<Any>(firecloudService!!.getWorkspace(any(), any()))
                .thenReturn(WorkspaceResponse().workspace(Workspace().bucketName("bkt")))
        `when`(cloudStorageService!!.getFileAsJson(any(), any())).thenReturn(JSONObject())
        `when`(firecloudService.staticNotebooksConvert(any()))
                .thenReturn(
                        "<STYLE type=\"text/css\">BODY{background:url(\"javascript:alert('XSS')\")} div {color: 'red'}</STYLE>\n")

        val html = String(notebooksService!!.getReadOnlyHtml("", "", "").toByteArray())
        assertThat(html).contains("style")
        assertThat(html).contains("color")
        // This behavior is not desired, but this test is in place to enshrine current expected
        // behavior. Style tags can introduce vulnerabilities as demonstrated in the test case - we
        // expect that the only style tags produced in the preview are produced by nbconvert, and are
        // therefore safe. Ideally we would keep the style tag, but sanitize the contents.
        assertThat(html).contains("XSS")
    }

    @Test
    fun testGetReadOnlyHtml_allowsDataImage() {
        `when`<Any>(firecloudService!!.getWorkspace(any(), any()))
                .thenReturn(WorkspaceResponse().workspace(Workspace().bucketName("bkt")))
        `when`(cloudStorageService!!.getFileAsJson(any(), any())).thenReturn(JSONObject())

        val dataUri = "data:image/png;base64,MTIz"
        `when`(firecloudService.staticNotebooksConvert(any()))
                .thenReturn("<img src=\"$dataUri\" />\n")

        val html = String(notebooksService!!.getReadOnlyHtml("", "", "").toByteArray())
        assertThat(html).contains(dataUri)
    }

    @Test
    fun testGetReadOnlyHtml_disallowsRemoteImage() {
        `when`<Any>(firecloudService!!.getWorkspace(any(), any()))
                .thenReturn(WorkspaceResponse().workspace(Workspace().bucketName("bkt")))
        `when`(cloudStorageService!!.getFileAsJson(any(), any())).thenReturn(JSONObject())
        `when`(firecloudService.staticNotebooksConvert(any()))
                .thenReturn("<img src=\"https://eviltrackingpixel.com\" />\n")

        val html = String(notebooksService!!.getReadOnlyHtml("", "", "").toByteArray())
        assertThat(html).doesNotContain("eviltrackingpixel.com")
    }
}
