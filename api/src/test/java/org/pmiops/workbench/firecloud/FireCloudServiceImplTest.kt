package org.pmiops.workbench.firecloud

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.fail
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import java.io.IOException
import java.util.Arrays
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.pmiops.workbench.auth.ServiceAccounts
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.exceptions.BadRequestException
import org.pmiops.workbench.exceptions.ForbiddenException
import org.pmiops.workbench.exceptions.NotFoundException
import org.pmiops.workbench.exceptions.ServerErrorException
import org.pmiops.workbench.exceptions.UnauthorizedException
import org.pmiops.workbench.firecloud.api.BillingApi
import org.pmiops.workbench.firecloud.api.GroupsApi
import org.pmiops.workbench.firecloud.api.NihApi
import org.pmiops.workbench.firecloud.api.ProfileApi
import org.pmiops.workbench.firecloud.api.StaticNotebooksApi
import org.pmiops.workbench.firecloud.api.StatusApi
import org.pmiops.workbench.firecloud.api.WorkspacesApi
import org.pmiops.workbench.firecloud.auth.OAuth
import org.pmiops.workbench.firecloud.model.CreateRawlsBillingProjectFullRequest
import org.pmiops.workbench.firecloud.model.ManagedGroupWithMembers
import org.pmiops.workbench.firecloud.model.NihStatus
import org.pmiops.workbench.firecloud.model.SystemStatus
import org.pmiops.workbench.test.Providers
import org.springframework.retry.backoff.NoBackOffPolicy

class FireCloudServiceImplTest {

    private var service: FireCloudServiceImpl? = null

    private var workbenchConfig: WorkbenchConfig? = null

    @Mock
    private val profileApi: ProfileApi? = null
    @Mock
    private val billingApi: BillingApi? = null
    @Mock
    private val workspacesApi: WorkspacesApi? = null
    @Mock
    private val workspaceAclsApi: WorkspacesApi? = null
    @Mock
    private val groupsApi: GroupsApi? = null
    @Mock
    private val nihApi: NihApi? = null
    @Mock
    private val statusApi: StatusApi? = null
    @Mock
    private val staticNotebooksApi: StaticNotebooksApi? = null
    @Mock
    private val fireCloudCredential: GoogleCredential? = null
    @Mock
    private val serviceAccounts: ServiceAccounts? = null
    @Mock
    private val impersonatedCredential: GoogleCredential? = null

    @Rule
    var mockitoRule = MockitoJUnit.rule()

    @Before
    fun setUp() {
        workbenchConfig = WorkbenchConfig.createEmptyConfig()
        workbenchConfig!!.firecloud.baseUrl = "https://api.firecloud.org"
        workbenchConfig!!.firecloud.debugEndpoints = true
        workbenchConfig!!.firecloud.timeoutInSeconds = 20
        workbenchConfig!!.billing.accountId = "test-billing-account"

        service = FireCloudServiceImpl(
                Providers.of(workbenchConfig),
                Providers.of<ProfileApi>(profileApi),
                Providers.of<BillingApi>(billingApi),
                Providers.of<GroupsApi>(groupsApi),
                Providers.of<NihApi>(nihApi),
                Providers.of<WorkspacesApi>(workspacesApi),
                Providers.of<WorkspacesApi>(workspaceAclsApi),
                Providers.of<StatusApi>(statusApi),
                Providers.of<StaticNotebooksApi>(staticNotebooksApi),
                FirecloudRetryHandler(NoBackOffPolicy()),
                serviceAccounts,
                Providers.of(fireCloudCredential))
    }

    @Test
    @Throws(ApiException::class)
    fun testStatus_success() {
        `when`(statusApi!!.status()).thenReturn(SystemStatus())
        assertThat(service!!.firecloudStatus).isTrue()
    }

    @Test
    @Throws(ApiException::class)
    fun testStatus_handleApiException() {
        `when`(statusApi!!.status()).thenThrow(ApiException(500, null, "{\"ok\": false}"))
        assertThat(service!!.firecloudStatus).isFalse()
    }

    @Test
    @Throws(ApiException::class)
    fun testStatus_handleJsonException() {
        `when`(statusApi!!.status()).thenThrow(ApiException(500, null, "unparseable response"))
        assertThat(service!!.firecloudStatus).isFalse()
    }

    @Test(expected = NotFoundException::class)
    @Throws(ApiException::class)
    fun testGetMe_throwsNotFound() {
        `when`(profileApi!!.me()).thenThrow(ApiException(404, "blah"))
        service!!.me
    }

    @Test(expected = ForbiddenException::class)
    @Throws(ApiException::class)
    fun testGetMe_throwsForbidden() {
        `when`(profileApi!!.me()).thenThrow(ApiException(403, "blah"))
        service!!.me
    }

    @Test(expected = UnauthorizedException::class)
    @Throws(ApiException::class)
    fun testGetMe_throwsUnauthorized() {
        `when`(profileApi!!.me()).thenThrow(ApiException(401, "blah"))
        service!!.me
    }

    @Test
    @Throws(Exception::class)
    fun testIsUserMemberOfGroup_none() {
        `when`(groupsApi!!.getGroup("group")).thenReturn(ManagedGroupWithMembers())
        assertThat(service!!.isUserMemberOfGroup(EMAIL_ADDRESS, "group")).isFalse()
    }

    @Test
    @Throws(Exception::class)
    fun testIsUserMemberOfGroup_noNameMatch() {
        val group = ManagedGroupWithMembers()
        group.setMembersEmails(Arrays.asList<T>("asdf@fake-research-aou.org"))
        `when`(groupsApi!!.getGroup("group")).thenReturn(group)
        assertThat(service!!.isUserMemberOfGroup(EMAIL_ADDRESS, "group")).isFalse()
    }

    @Test
    @Throws(Exception::class)
    fun testIsUserMemberOfGroup_matchInAdminList() {
        val group = ManagedGroupWithMembers()
        group.setAdminsEmails(Arrays.asList<T>(EMAIL_ADDRESS))

        `when`(groupsApi!!.getGroup("group")).thenReturn(group)
        assertThat(service!!.isUserMemberOfGroup(EMAIL_ADDRESS, "group")).isTrue()
    }

    @Test
    @Throws(Exception::class)
    fun testIsUserMemberOfGroup_matchInMemberList() {
        val group = ManagedGroupWithMembers()
        group.setMembersEmails(Arrays.asList<T>(EMAIL_ADDRESS))

        `when`(groupsApi!!.getGroup("group")).thenReturn(group)
        assertThat(service!!.isUserMemberOfGroup(EMAIL_ADDRESS, "group")).isTrue()
    }

    @Test
    @Throws(Exception::class)
    fun testNihStatus() {
        val status = NihStatus().linkedNihUsername("test").linkExpireTime(500L)
        `when`(nihApi!!.nihStatus()).thenReturn(status)
        assertThat(service!!.nihStatus).isNotNull()
        assertThat(service!!.nihStatus).isEqualTo(status)
    }

    @Test
    @Throws(Exception::class)
    fun testNihStatusNotFound() {
        `when`(nihApi!!.nihStatus()).thenThrow(ApiException(404, "Not Found"))
        assertThat(service!!.nihStatus).isNull()
    }

    @Test(expected = ServerErrorException::class)
    @Throws(Exception::class)
    fun testNihStatusException() {
        `when`(nihApi!!.nihStatus()).thenThrow(ApiException(500, "Internal Server Error"))
        service!!.nihStatus
    }

    @Test
    @Throws(Exception::class)
    fun testNihCallback() {
        `when`(nihApi!!.nihCallback(any<T>()))
                .thenReturn(NihStatus().linkedNihUsername("test").linkExpireTime(500L))
        try {
            service!!.postNihCallback(any<Any>())
        } catch (e: Exception) {
            fail()
        }

    }

    @Test(expected = BadRequestException::class)
    @Throws(Exception::class)
    fun testNihCallbackBadRequest() {
        `when`(nihApi!!.nihCallback(any<T>())).thenThrow(ApiException(400, "Bad Request"))
        service!!.postNihCallback(any<Any>())
    }

    @Test(expected = ServerErrorException::class)
    @Throws(Exception::class)
    fun testNihCallbackServerError() {
        `when`(nihApi!!.nihCallback(any<T>())).thenThrow(ApiException(500, "Internal Server Error"))
        service!!.postNihCallback(any<Any>())
    }

    @Test
    @Throws(IOException::class)
    fun testGetApiClientWithImpersonation() {
        `when`(serviceAccounts!!.getImpersonatedCredential(any(), eq("asdf@fake-research-aou.org"), any()))
                .thenReturn(impersonatedCredential)

        // Pretend we retrieved the given access token.
        `when`(impersonatedCredential!!.accessToken).thenReturn("impersonated-access-token")

        val apiClient = service!!.getApiClientWithImpersonation("asdf@fake-research-aou.org")

        // The impersonated access token should be assigned to the generated API client.
        val oauth = apiClient.getAuthentication("googleoauth") as OAuth
        assertThat(oauth.getAccessToken()).isEqualTo("impersonated-access-token")
    }

    @Test
    @Throws(Exception::class)
    fun testCreateAllOfUsBillingProject() {
        workbenchConfig!!.featureFlags.enableVpcFlowLogs = false
        workbenchConfig!!.featureFlags.enableVpcServicePerimeter = false
        workbenchConfig!!.firecloud.vpcServicePerimeterName = "this will be ignored"

        service!!.createAllOfUsBillingProject("project-name")

        val captor = ArgumentCaptor.forClass(CreateRawlsBillingProjectFullRequest::class.java)
        verify<Any>(billingApi).createBillingProjectFull(captor.capture())
        val request = captor.getValue()

        // N.B. FireCloudServiceImpl doesn't add the project prefix; this is done by callers such
        // as BillingProjectBufferService.
        assertThat(request.getProjectName()).isEqualTo("project-name")
        // FireCloudServiceImpl always adds the "billingAccounts/" prefix to the billing account
        // from config.
        assertThat(request.getBillingAccount()).isEqualTo("billingAccounts/test-billing-account")
        assertThat(request.getEnableFlowLogs()).isFalse()
        assertThat(request.getHighSecurityNetwork()).isFalse()
        assertThat(request.getServicePerimeter()).isNull()
    }

    @Test
    @Throws(Exception::class)
    fun testVpcFlowLogsParams() {
        workbenchConfig!!.featureFlags.enableVpcFlowLogs = true

        service!!.createAllOfUsBillingProject("project-name")

        val captor = ArgumentCaptor.forClass(CreateRawlsBillingProjectFullRequest::class.java)
        verify<Any>(billingApi).createBillingProjectFull(captor.capture())
        val request = captor.getValue()

        assertThat(request.getEnableFlowLogs()).isTrue()
        assertThat(request.getHighSecurityNetwork()).isTrue()
    }

    @Test
    @Throws(Exception::class)
    fun testVpcServicePerimeterParams() {
        val servicePerimeter = "a-cloud-with-a-fence-around-it"

        workbenchConfig!!.featureFlags.enableVpcServicePerimeter = true
        workbenchConfig!!.firecloud.vpcServicePerimeterName = servicePerimeter

        service!!.createAllOfUsBillingProject("project-name")

        val captor = ArgumentCaptor.forClass(CreateRawlsBillingProjectFullRequest::class.java)
        verify<Any>(billingApi).createBillingProjectFull(captor.capture())
        val request = captor.getValue()

        assertThat(request.getServicePerimeter()).isEqualTo(servicePerimeter)
    }

    companion object {

        private val EMAIL_ADDRESS = "abc@fake-research-aou.org"
    }
}
