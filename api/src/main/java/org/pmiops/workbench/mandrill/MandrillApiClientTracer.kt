package org.pmiops.workbench.mandrill

import com.squareup.okhttp.Call
import com.squareup.okhttp.Response
import io.opencensus.common.Scope
import io.opencensus.trace.Tracer
import io.opencensus.trace.Tracing
import java.io.IOException
import java.lang.reflect.Type

class MandrillApiClientTracer : ApiClient() {

    @Throws(ApiException::class)
    fun <T> execute(call: Call, returnType: Type): ApiResponse<T> {
        var response: Response
        var data: T
        try {
            tracer
                    .spanBuilderWithExplicitParent("MandrillApiCall", tracer.currentSpan)
                    .startScopedSpan().use { ss ->
                        response = call.execute()
                        data = handleResponseWithTracing(response, returnType)
                    }
        } catch (e: IOException) {
            throw ApiException(e)
        }

        return ApiResponse(response.code(), response.headers().toMultimap(), data)
    }

    @Throws(ApiException::class)
    private fun <T> handleResponseWithTracing(response: Response, returnType: Type): T {
        val targetUrl = response.request().httpUrl().encodedPath()
        val urlSpan = tracer
                .spanBuilderWithExplicitParent(
                        "Response Received: $targetUrl", tracer.currentSpan)
                .startScopedSpan()
        urlSpan.close()
        return super.handleResponse(response, returnType)
    }

    companion object {
        private val tracer = Tracing.getTracer()
    }
}
