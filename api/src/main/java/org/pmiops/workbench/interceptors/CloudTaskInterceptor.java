package org.pmiops.workbench.interceptors;

import com.google.api.client.http.HttpMethods;
import com.google.common.base.Strings;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

/**
 * Interceptor for endpoints with tag cloudTask. All such endpoints should have tagged as
 * "cloudTask" and a valid value for request hander X-AppEngine-QueueName which app engine itself
 * only sets (and overrides internally) when invoking cloud task. See
 * https://cloud.google.com/tasks/docs/creating-appengine-handlers#reading_app_engine_task_request_headers
 */
@Service
public class CloudTaskInterceptor implements AsyncHandlerInterceptor {
  public static final String QUEUE_NAME_REQUEST_HEADER = "X-AppEngine-QueueName";

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    if (request.getMethod().equals(HttpMethods.OPTIONS)) {
      return true;
    }

    HandlerMethod method = (HandlerMethod) handler;
    Operation apiOp = AnnotationUtils.findAnnotation(method.getMethod(), Operation.class);
    if (apiOp == null) {
      return true;
    }

    boolean requireCloudTaskHeader = InterceptorUtils.isCloudTaskRequest(apiOp);

    boolean hasQueueNameHeader =
        !Strings.isNullOrEmpty(request.getHeader(QUEUE_NAME_REQUEST_HEADER));
    if (requireCloudTaskHeader && !hasQueueNameHeader) {
      response.sendError(
          HttpServletResponse.SC_FORBIDDEN,
          String.format(
              "cloud task endpoints are only invocable via app engine cloudTask, and "
                  + "require the '%s' header",
              QUEUE_NAME_REQUEST_HEADER));
      return false;
    }
    return true;
  }
}
