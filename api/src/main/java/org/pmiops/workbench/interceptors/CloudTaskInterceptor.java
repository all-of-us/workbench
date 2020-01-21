package org.pmiops.workbench.interceptors;

import com.google.api.client.http.HttpMethods;
import io.swagger.annotations.ApiOperation;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

@Service
public class CloudTaskInterceptor extends HandlerInterceptorAdapter {
  public static final String QUEUE_NAME_HEADER = "X-AppEngine-QueueName";
  private static final String CLOUD_TASK_TAG = "cloudTask";
  public static final String RDR_QUEUE_NAME_HEADER = "rdrQueueTest";

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

    boolean hasQueueNameHeader = RDR_QUEUE_NAME_HEADER.equals(request.getHeader(QUEUE_NAME_HEADER));
    if (requireCloudTaskHeader && !hasQueueNameHeader) {
      response.sendError(
          HttpServletResponse.SC_FORBIDDEN,
          String.format(
              "cloud task endpoints are only invocable via app engine cloudTask, and "
                  + "require the '%s' header",
              QUEUE_NAME_HEADER));
      return false;
    }
    return true;
  }
}
