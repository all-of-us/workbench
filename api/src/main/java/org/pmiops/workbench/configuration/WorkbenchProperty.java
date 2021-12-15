package org.pmiops.workbench.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/** Workbench API server properities. */

@Configuration
@EnableConfigurationProperties
@EnableTransactionManagement
@ConfigurationProperties(prefix = "workbench.property")
public class WorkbenchProperty {
  /** Whether to run the scheduler to periodically. */
  private boolean isLocal = false;

  public boolean isLocal() {
    System.out.println("~~~~!!!!!!~~~~!!!!");
    System.out.println("~~~~!!!!!!~~~~!!!!");
    System.out.println("~~~~!!!!!!~~~~!!!!");
    System.out.println("~~~~!!!!!!~~~~!!!!");
    System.out.println("~~~~!!!!!!~~~~!!!!");
    System.out.println("~~~~!!!!!!~~~~!!!!");
    System.out.println("~~~~!!!!!!~~~~!!!!");
    System.out.println(System.getenv("WORKBENCH_PROPERTY_LOCAL"));
    System.out.println(System.getenv());
    return isLocal;
  }

  public void setIsLocal(boolean isLocal) {
    isLocal = isLocal;
  }
}
