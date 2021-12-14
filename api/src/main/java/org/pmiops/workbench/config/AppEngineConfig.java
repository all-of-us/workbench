package org.pmiops.workbench.config;

import com.google.api.client.extensions.appengine.http.UrlFetchTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppEngineConfig {
  private static final Logger log = Logger.getLogger(AppEngineConfig.class.getName());

/*  @Bean
  HttpTransport httpTransport(Provider<WorkbenchConfig> workbenchConfigProvider) {
    // UrlFetchTransport only works in Appengine environment, when running local server, use
    // NetHttpTransport
    // See
    // https://github.com/googleapis/google-http-java-client/blob/main/google-http-client/src/main/java/com/google/api/client/http/HttpTransport.java#L31L59
    if (workbenchConfigProvider.get().server.shortName.equals("Local")) {
      log.log(Level.INFO, "Use NetHttpTransport in local server.");
      return new NetHttpTransport();
    } else {
      return UrlFetchTransport.getDefaultInstance();
    }
  }*/

  @Bean
  HttpTransport httpTransport() {
    // UrlFetchTransport only works in Appengine environment, when running local server, use
    // NetHttpTransport
    // See
    // https://github.com/googleapis/google-http-java-client/blob/main/google-http-client/src/main/java/com/google/api/client/http/HttpTransport.java#L31L59
      return new NetHttpTransport();
  }
}
