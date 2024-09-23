package org.pmiops.workbench.config;

import com.google.api.services.oauth2.model.Userinfo;
import jakarta.servlet.ServletContext;
import java.util.Optional;
import org.pmiops.workbench.auth.UserAuthentication;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.interceptors.AuthInterceptor;
import org.pmiops.workbench.interceptors.BlockAllTrafficInterceptor;
import org.pmiops.workbench.interceptors.ClearCdrVersionContextInterceptor;
import org.pmiops.workbench.interceptors.CloudTaskInterceptor;
import org.pmiops.workbench.interceptors.CronInterceptor;
import org.pmiops.workbench.interceptors.SecurityHeadersInterceptor;
import org.pmiops.workbench.interceptors.TracingInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@EnableWebMvc
@Configuration
@ComponentScan(basePackages = {"org.pmiops.workbench.interceptors", "org.pmiops.workbench.google"})
public class WebMvcConfig implements WebMvcConfigurer {

  private static final Logger log = LoggerFactory.getLogger(WebMvcConfig.class);
  @Autowired private AuthInterceptor authInterceptor;

  @Autowired private ClearCdrVersionContextInterceptor clearCdrVersionInterceptor;

  @Autowired private CloudTaskInterceptor cloudTaskInterceptor;

  @Autowired private CronInterceptor cronInterceptor;

  @Autowired private SecurityHeadersInterceptor securityHeadersInterceptor;

  @Autowired private TracingInterceptor tracingInterceptor;

  @Autowired private BlockAllTrafficInterceptor blockAllTrafficInterceptor;

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
    // Blocks traffic if isDownForMaintenance is true in environment config
    registry.addInterceptor(blockAllTrafficInterceptor);
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

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/**")
        .allowedMethods("GET", "HEAD", "POST", "PUT", "DELETE", "PATCH", "TRACE", "OPTIONS")
        .allowedOrigins("*")
        .allowedHeaders("*")
        .exposedHeaders("Origin, X-Requested-With, Content-Type, Accept, Authorization");
  }
}
