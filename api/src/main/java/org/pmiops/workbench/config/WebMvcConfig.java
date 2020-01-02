package org.pmiops.workbench.config;

import com.google.api.services.oauth2.model.Userinfoplus;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import javax.servlet.ServletContext;
import org.pmiops.workbench.auth.Constants;
import org.pmiops.workbench.auth.UserAuthentication;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.interceptors.AuthInterceptor;
import org.pmiops.workbench.interceptors.ClearCdrVersionContextInterceptor;
import org.pmiops.workbench.interceptors.CorsInterceptor;
import org.pmiops.workbench.interceptors.CronInterceptor;
import org.pmiops.workbench.interceptors.SecurityHeadersInterceptor;
import org.pmiops.workbench.interceptors.TracingInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@EnableWebMvc
@Configuration
@ComponentScan(basePackages = {"org.pmiops.workbench.interceptors", "org.pmiops.workbench.google"})
public class WebMvcConfig extends WebMvcConfigurerAdapter {

  @Autowired private AuthInterceptor authInterceptor;

  @Autowired private CorsInterceptor corsInterceptor;

  @Autowired private ClearCdrVersionContextInterceptor clearCdrVersionInterceptor;

  @Autowired private CronInterceptor cronInterceptor;

  @Autowired private SecurityHeadersInterceptor securityHeadersInterceptor;

  @Autowired private TracingInterceptor tracingInterceptor;

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
  public DbUser user(UserAuthentication userAuthentication) {
    return userAuthentication.getUser();
  }

  /**
   * Service account credentials for Gsuite administration, corresponding to the "gsuite-admin"
   * service account in each environment. Enabled for domain-wide delegation of authority.
   */
  @Lazy
  @Bean(name = Constants.GSUITE_ADMIN_CREDS)
  public GoogleCredentials gsuiteAdminCredential(CloudStorageService cloudStorageService)
      throws IOException {
    return cloudStorageService.getGSuiteAdminCredentials();
  }

  /**
   * Service account credentials for FireCloud administration. This Service Account has been enabled
   * for domain-wide delegation of authority.
   */
  @Lazy
  @Bean(name = Constants.FIRECLOUD_ADMIN_CREDS)
  public GoogleCredentials firecloudAdminCredential(CloudStorageService cloudStorageService)
      throws IOException {
    return cloudStorageService.getFireCloudAdminCredentials();
  }

  /**
   * Service account credentials for Cloud Resource Manager administration. This Service Account has
   * been enabled for domain-wide delegation of authority.
   */
  @Lazy
  @Bean(name = Constants.CLOUD_RESOURCE_MANAGER_ADMIN_CREDS)
  public GoogleCredentials cloudResourceManagerAdminCredential(
      CloudStorageService cloudStorageService) throws IOException {
    return cloudStorageService.getCloudResourceManagerAdminCredentials();
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(corsInterceptor);
    registry.addInterceptor(authInterceptor);
    registry.addInterceptor(tracingInterceptor);
    registry.addInterceptor(cronInterceptor);
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
