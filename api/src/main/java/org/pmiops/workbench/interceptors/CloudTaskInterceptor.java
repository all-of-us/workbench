package org.pmiops.workbench.interceptors;

import com.google.api.client.http.HttpMethods;
import io.swagger.annotations.ApiOperation;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Interceptor for endpoints with tag cloudTask. All such endpoints should have valid value for
 * request hander X-AppEngine-QueueName
 */
@Service
public class CloudTaskInterceptor extends HandlerInterceptorAdapter {
  public static final String QUEUE_NAME_REQUEST_HEADER = "X-AppEngine-QueueName";
  private static final String CLOUD_TASK_TAG = "cloudTask";
  public static final Set<String> VALID_QUEUE_NAME_SET = new HashSet<>(Arrays.asList("rdrQueueTest"));

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    if (request.getMethod().equals(HttpMethods.OPTIONS)) {
      return true;
    }

    HandlerMethod method = (HandlerMethod) handler;
    ApiOperation apiOp = AnnotationUtils.findAnnotation(method.getMethod(), ApiOperation.class);
    if (apiOp == null) {
      return true;
    }

    boolean requireCloudTaskHeader = false;
    for (String tag : apiOp.tags()) {
      if (CLOUD_TASK_TAG.equals(tag)) {
        requireCloudTaskHeader = true;
        break;
      }
    }

    boolean hasQueueNameHeader = VALID_QUEUE_NAME_SET.contains(request.getHeader(QUEUE_NAME_REQUEST_HEADER));
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
