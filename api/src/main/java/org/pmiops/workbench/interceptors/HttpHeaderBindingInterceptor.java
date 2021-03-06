package org.pmiops.workbench.interceptors;

import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

@Service
public class HttpHeaderBindingInterceptor extends HandlerInterceptorAdapter {

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {

    HttpHeaders httpHeaders =
        Collections.list(request.getHeaderNames()).stream()
            .collect(
                Collectors.toMap(
                    Function.identity(),
                    h -> Collections.list(request.getHeaders(h)),
                    // This shouldn't happen, since headerNames should return unique items.
                    (prev, next) -> next,
                    HttpHeaders::new));
    HttpHeadersContext.setHttpHeaders(httpHeaders);
    return true;
  }

  @Override
  public void postHandle(
      HttpServletRequest request,
      HttpServletResponse response,
      Object handler,
      ModelAndView modelAndView) {
    HttpHeadersContext.clearHttpHeaders();
  }
}
