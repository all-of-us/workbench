package org.pmiops.workbench.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/** Workbench API server properities. */

@Component
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "workbench.property")
public class WorkbenchProperty {
  /** Whether to run the scheduler to periodically. */
  private boolean local = false;

  public boolean isLocal() {
    System.out.println("~~~~!!!!!!~~~~!!!!");
    System.out.println("~~~~!!!!!!~~~~!!!!");
    System.out.println("~~~~!!!!!!~~~~!!!!");
    System.out.println("~~~~!!!!!!~~~~!!!!");
    System.out.println("~~~~!!!!!!~~~~!!!!");
    System.out.println("~~~~!!!!!!~~~~!!!!");
    System.out.println("~~~~!!!!!!~~~~!!!!");
    System.out.println(System.getenv("WORKBENCH_PROPERTY_LOCAL"));
    System.out.println(System.getProperties());
    return local;
  }

  public void setLocal(boolean local) {
    local = local;
  }
}
