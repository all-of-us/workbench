package org.pmiops.workbench.interceptors;

import java.lang.annotation.Annotation;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;



/**
 * Intercepts all API requests to ensure they have appropriate CORS headers.
 */
@Service
public class CorsInterceptor extends HandlerInterceptorAdapter {
  private static final Logger log = Logger.getLogger(CorsInterceptor.class.getName());


  public static final String CREDENTIALS_NAME = "Access-Control-Allow-Credentials";
  public static final String ORIGIN_NAME = "Access-Control-Allow-Origin";
  public static final String METHODS_NAME = "Access-Control-Allow-Methods";
  public static final String HEADERS_NAME = "Access-Control-Allow-Headers";
  public static final String MAX_AGE_NAME = "Access-Control-Max-Age";

  /**
   * Assigns cors headers to a response object.
   * @param handler The Swagger-generated ApiController. It contains our handler as a private
   *     delegate.
   */
  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
      throws Exception {
    response.setHeader(CREDENTIALS_NAME, "true");
    response.setHeader(ORIGIN_NAME, "*");
    response.setHeader(METHODS_NAME, "GET, HEAD, POST, PUT, DELETE, PATCH, TRACE, OPTIONS");
    response.setHeader(HEADERS_NAME, "Origin, X-Requested-With, Content-Type, Accept, Authorization");

    return true;
  }


}
