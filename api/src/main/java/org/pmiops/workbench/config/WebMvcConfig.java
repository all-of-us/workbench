package org.pmiops.workbench.config;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.HttpMethods;
import com.google.api.services.oauth2.model.Userinfoplus;
import com.google.apphosting.api.ApiProxy;
import java.io.IOException;
import java.io.InputStream;
import javax.servlet.ServletContext;
import org.pmiops.workbench.auth.UserAuthentication;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.interceptors.AuthInterceptor;
import org.pmiops.workbench.interceptors.CorsInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@EnableWebMvc
@Configuration
public class WebMvcConfig extends WebMvcConfigurerAdapter {

  @Autowired
  private AuthInterceptor authInterceptor;

  @Autowired
  private CorsInterceptor corsInterceptor;

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public UserAuthentication userAuthentication() {
    return (UserAuthentication) SecurityContextHolder.getContext().getAuthentication();
  }

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public Userinfoplus userInfo(UserAuthentication userAuthentication) {
    return userAuthentication.getPrincipal();
  }

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public User user(UserAuthentication userAuthentication) {
    return userAuthentication.getUser();
  }

  @Bean("apiHostName")
  public String getHostName() {
    ApiProxy.Environment env = ApiProxy.getCurrentEnvironment();
    return (String) env.getAttributes().get("com.google.appengine.runtime.default_version_hostname");
  }

  @Bean
  public WorkbenchEnvironment workbenchEnvironment() {
    return new WorkbenchEnvironment();
  }

  /**
   * Service account credentials for the AofU server. These are derived from a key JSON file
   * copied from GCS deployed to /WEB-INF/sa-key.json during the build step. They can be used
   * to make API calls on behalf of AofU (as opposed to using end user credentials.)
   *
   * We may in future rotate key files in production, but will be sure to keep the ones currently
   * in use in cloud environments working when that happens.
   */
  @Lazy
  @Bean
  public GoogleCredential serviceAccountCredential() {
    ServletContext context = getRequestServletContext();
    InputStream saFileAsStream = context.getResourceAsStream("/WEB-INF/sa-key.json");
    GoogleCredential credential = null;
    try {
      return GoogleCredential.fromStream(saFileAsStream);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(corsInterceptor);
    registry.addInterceptor(authInterceptor);
  }

  @Override
  public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
    configurer.defaultContentType(MediaType.APPLICATION_JSON);
  }

  static ServletContext getRequestServletContext() {
    return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
      .getRequest().getServletContext();
  }
}
