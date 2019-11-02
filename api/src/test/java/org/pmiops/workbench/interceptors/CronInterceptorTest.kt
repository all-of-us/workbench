package org.pmiops.workbench.interceptors

import com.google.common.truth.Truth.assertThat
import org.mockito.Mockito.`when`

import com.google.api.client.http.HttpMethods
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.pmiops.workbench.api.OfflineAuditApi
import org.pmiops.workbench.api.WorkspacesApi
import org.springframework.web.method.HandlerMethod

class CronInterceptorTest {
    @Rule
    var mockitoRule = MockitoJUnit.rule()

    @Mock
    private val handler: HandlerMethod? = null
    @Mock
    private val request: HttpServletRequest? = null
    @Mock
    private val response: HttpServletResponse? = null

    private var interceptor: CronInterceptor? = null

    @Before
    fun setUp() {
        interceptor = CronInterceptor()
    }

    @Test
    @Throws(Exception::class)
    fun preHandleOptions_OPTIONS() {
        `when`(request!!.method).thenReturn(HttpMethods.OPTIONS)
        assertThat(interceptor!!.preHandle(request, response, handler)).isTrue()
    }

    @Test
    @Throws(Exception::class)
    fun prehandleForCronNoHeader() {
        `when`(request!!.method).thenReturn(HttpMethods.GET)
        `when`<Method>(handler!!.method).thenReturn(OfflineAuditApi::class.java!!.getMethod("auditBigQuery"))
        assertThat(interceptor!!.preHandle(request, response, handler)).isFalse()
    }

    @Test
    @Throws(Exception::class)
    fun prehandleForCronWithBadHeader() {
        `when`(request!!.method).thenReturn(HttpMethods.GET)
        `when`<Method>(handler!!.method).thenReturn(OfflineAuditApi::class.java!!.getMethod("auditBigQuery"))
        `when`(request.getHeader(CronInterceptor.GAE_CRON_HEADER)).thenReturn("asdf")
        assertThat(interceptor!!.preHandle(request, response, handler)).isFalse()
    }

    @Test
    @Throws(Exception::class)
    fun prehandleForCronWithHeader() {
        `when`(request!!.method).thenReturn(HttpMethods.GET)
        `when`<Method>(handler!!.method).thenReturn(OfflineAuditApi::class.java!!.getMethod("auditBigQuery"))
        `when`(request.getHeader(CronInterceptor.GAE_CRON_HEADER)).thenReturn("true")
        assertThat(interceptor!!.preHandle(request, response, handler)).isTrue()
    }

    @Test
    @Throws(Exception::class)
    fun prehandleForNonCron() {
        `when`(request!!.method).thenReturn(HttpMethods.GET)
        `when`<Method>(handler!!.method).thenReturn(WorkspacesApi::class.java!!.getMethod("getWorkspaces"))
        assertThat(interceptor!!.preHandle(request, response, handler)).isTrue()
    }
}
