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
public class CronInterceptor extends HandlerInterceptorAdapter {
  public static final String GAE_CRON_HEADER = "X-Appengine-Cron";
  private static final String CRON_TAG = "cron";

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

    boolean requireCronHeader = false;
    for (String tag : apiOp.tags()) {
      if (CRON_TAG.equals(tag)) {
        requireCronHeader = true;
        break;
      }
    }
    boolean hasCronHeader = "true".equals(request.getHeader(GAE_CRON_HEADER));
    if (requireCronHeader && !hasCronHeader) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN,
          String.format("cronjob endpoints are only invocable via app engine cronjob, and " +
              "require the '%s' header", GAE_CRON_HEADER));
      return false;
    }
    return true;
  }
}
