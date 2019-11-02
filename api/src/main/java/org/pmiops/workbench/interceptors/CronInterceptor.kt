package org.pmiops.workbench.interceptors

import com.google.api.client.http.HttpMethods
import io.swagger.annotations.ApiOperation
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.stereotype.Service
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter

@Service
class CronInterceptor : HandlerInterceptorAdapter() {

    @Throws(Exception::class)
    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse?, handler: Any?): Boolean {
        if (request.method == HttpMethods.OPTIONS) {
            return true
        }

        val method = handler as HandlerMethod?
        val apiOp = AnnotationUtils.findAnnotation(method!!.method, ApiOperation::class.java)
                ?: return true

        var requireCronHeader = false
        for (tag in apiOp.tags()) {
            if (CRON_TAG == tag) {
                requireCronHeader = true
                break
            }
        }
        val hasCronHeader = "true" == request.getHeader(GAE_CRON_HEADER)
        if (requireCronHeader && !hasCronHeader) {
            response!!.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    String.format(
                            "cronjob endpoints are only invocable via app engine cronjob, and " + "require the '%s' header",
                            GAE_CRON_HEADER))
            return false
        }
        return true
    }

    companion object {
        val GAE_CRON_HEADER = "X-Appengine-Cron"
        private val CRON_TAG = "cron"
    }
}
