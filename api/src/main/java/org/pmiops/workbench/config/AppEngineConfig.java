package org.pmiops.workbench.config;

import com.google.api.client.extensions.appengine.http.UrlFetchTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppEngineConfig {
  private static final Logger log = Logger.getLogger(AppEngineConfig.class.getName());

  private static final String GAE_INSTANCE_ENV_VAR = "GAE_INSTANCE";

  @Bean
  HttpTransport httpTransport() {
    // UrlFetchTransport only works in Appengine environment, when running local server, use
    // NetHttpTransport
    // https://github.com/googleapis/google-http-java-client/blob/main/google-http-client/src/main/java/com/google/api/client/http/HttpTransport.java#L31L59
    if (getGaeNodeId().isPresent()) {
      log.log(Level.INFO, "Running in GAE environment, use UrlFetchTransport");
      return UrlFetchTransport.getDefaultInstance();
    } else {
      log.log(Level.INFO, "Running in non-GAE environment, use NetHttpTransport.");
      return new NetHttpTransport();
    }
  }

  /**
   * Gets the GAE instance ID from environment variable. If null, then the server is not running in
   * GAE environment.
   *
   * <p>See https://cloud.google.com/appengine/docs/standard/java11/runtime#environment_variables
   */
  public static Optional<String> getGaeNodeId() {
    return Optional.ofNullable(System.getenv(GAE_INSTANCE_ENV_VAR));
  }
}
