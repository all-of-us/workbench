package org.pmiops.workbench.google;

import com.google.cloud.iam.credentials.v1.IamCredentialsClient;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class GoogleConfig {

  @Bean
  @Lazy
  public IamCredentialsClient getIamCredentialsClient() throws IOException {
    return IamCredentialsClient.create();
  }
}
