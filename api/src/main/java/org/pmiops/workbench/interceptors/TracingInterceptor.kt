package org.pmiops.workbench.interceptors

import io.opencensus.common.Scope
import io.opencensus.exporter.trace.stackdriver.StackdriverTraceConfiguration
import io.opencensus.exporter.trace.stackdriver.StackdriverTraceExporter
import io.opencensus.trace.SpanBuilder
import io.opencensus.trace.Tracer
import io.opencensus.trace.Tracing
import io.opencensus.trace.samplers.Samplers
import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger
import javax.inject.Provider
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.pmiops.workbench.config.WorkbenchConfig
import org.pmiops.workbench.firecloud.FirecloudApiClientTracer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter

// Interceptor to create a trace of the lifecycle of api calls.
@Service
class TracingInterceptor @Autowired
constructor(private val workbenchConfigProvider: Provider<WorkbenchConfig>) : HandlerInterceptorAdapter() {

    init {
        try {
            StackdriverTraceExporter.createAndRegister(StackdriverTraceConfiguration.builder().build())
        } catch (e: IOException) {
            log.log(Level.WARNING, "Failed to setup tracing", e)
        }

    }

    /**
     * @param handler The Swagger-generated ApiController. It contains our handler as a private
     * delegate.
     */
    @Throws(Exception::class)
    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse?, handler: Any?): Boolean {

        val requestSpanBuilder = tracer.spanBuilder(
                String.format(
                        "%s/%s%s",
                        workbenchConfigProvider.get().server.shortName,
                        request.method,
                        request.requestURI))

        if (workbenchConfigProvider.get().server.traceAllRequests) {
            requestSpanBuilder.setSampler(Samplers.alwaysSample())
        }

        val requestSpan = requestSpanBuilder.startScopedSpan()
        request.setAttribute(TRACE_ATTRIBUTE_KEY, requestSpan)
        return true
    }

    @Throws(Exception::class)
    override fun afterCompletion(
            request: HttpServletRequest, response: HttpServletResponse?, handler: Any?, ex: Exception?) {
        (request.getAttribute(TRACE_ATTRIBUTE_KEY) as Scope).close()
    }

    companion object {
        private val tracer = Tracing.getTracer()
        private val log = Logger.getLogger(FirecloudApiClientTracer::class.java.name)
        private val TRACE_ATTRIBUTE_KEY = "Tracing Span"
    }
}
