package org.pmiops.workbench.config;

import com.google.api.services.oauth2.model.Userinfo;
import java.util.Optional;
import jakarta.servlet.ServletContext;
import org.pmiops.workbench.auth.UserAuthentication;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.interceptors.AuthInterceptor;
import org.pmiops.workbench.interceptors.ClearCdrVersionContextInterceptor;
import org.pmiops.workbench.interceptors.CloudTaskInterceptor;
import org.pmiops.workbench.interceptors.CorsInterceptor;
import org.pmiops.workbench.interceptors.CronInterceptor;
import org.pmiops.workbench.interceptors.SecurityHeadersInterceptor;
import org.pmiops.workbench.interceptors.TracingInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@EnableWebMvc
@Configuration
@ComponentScan(basePackages = {"org.pmiops.workbench.interceptors", "org.pmiops.workbench.google"})
public class WebMvcConfig implements WebMvcConfigurer {

  @Autowired private AuthInterceptor authInterceptor;

  @Autowired private CorsInterceptor corsInterceptor;

  @Autowired private ClearCdrVersionContextInterceptor clearCdrVersionInterceptor;

  @Autowired private CloudTaskInterceptor cloudTaskInterceptor;

  @Autowired private CronInterceptor cronInterceptor;

  @Autowired private SecurityHeadersInterceptor securityHeadersInterceptor;

  @Autowired private TracingInterceptor tracingInterceptor;

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public UserAuthentication userAuthentication() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) {
      return null;
    }
    return (UserAuthentication) auth;
  }

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public Userinfo userInfo(Optional<UserAuthentication> userAuthentication) {
    return userAuthentication.map(UserAuthentication::getPrincipal).orElse(null);
  }

  @Bean
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  public DbUser user(Optional<UserAuthentication> userAuthentication) {
    return userAuthentication.map(UserAuthentication::getUser).orElse(null);
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(corsInterceptor);
    registry.addInterceptor(authInterceptor);
    registry.addInterceptor(tracingInterceptor);
    registry.addInterceptor(cronInterceptor);
    registry.addInterceptor(cloudTaskInterceptor);
    registry.addInterceptor(clearCdrVersionInterceptor);
    registry.addInterceptor(securityHeadersInterceptor);
  }

  @Override
  public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
    configurer.defaultContentType(MediaType.APPLICATION_JSON);
  }

  static ServletContext getRequestServletContext() {
    return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
        .getRequest()
        .getServletContext();
  }
}
