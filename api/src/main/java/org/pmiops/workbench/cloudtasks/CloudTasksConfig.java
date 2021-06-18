package org.pmiops.workbench.cloudtasks;

import com.google.cloud.tasks.v2.CloudTasksClient;
import com.google.common.base.Strings;
import java.io.IOException;
import java.util.logging.Logger;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.web.context.annotation.RequestScope;

@Configuration
public class CloudTasksConfig {

  private static Logger log = Logger.getLogger(CloudTasksConfig.class.getName());

  @Bean(destroyMethod = "close")
  @RequestScope(proxyMode = ScopedProxyMode.DEFAULT)
  CloudTasksClient cloudTasksClient(WorkbenchConfig workbenchConfig) throws IOException {
    if (!Strings.isNullOrEmpty(workbenchConfig.offlineBatch.unsafeCloudTasksForwardingHost)) {
      log.warning(
          String.format(
              "Cloud Tasks will be forwarded synchronously to '%s'. This should only happen during "
                  + "local development. If you are seeing this in a cloud environment, please "
                  + "investigate immediately.",
              workbenchConfig.offlineBatch.unsafeCloudTasksForwardingHost));
      return CloudTasksClient.create(
          new ForwardingCloudTasksStub(
              workbenchConfig.offlineBatch.unsafeCloudTasksForwardingHost));
    }
    return CloudTasksClient.create();
  }
}
