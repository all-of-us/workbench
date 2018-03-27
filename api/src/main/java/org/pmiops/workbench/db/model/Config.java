package org.pmiops.workbench.db.model;

import com.google.common.collect.ImmutableMap;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import org.pmiops.workbench.config.CdrSchemaConfig;
import org.pmiops.workbench.config.WorkbenchConfig;

@Entity
@Table(name = "config")
public class Config {

  public static final String MAIN_CONFIG_ID = "main";
  // TODO: consider whether we need different CDR schema config for different CDR versions in future
  public static final String CDR_SCHEMA_CONFIG_ID = "cdrSchema";

  private String configId;
  private String configuration;

  @Id
  @Column(name = "config_id")
  public String getConfigId() {
    return configId;
  }

  public void setConfigId(String configId) {
    this.configId = configId;
  }

  @Column(name = "configuration")
  public String getConfiguration() {
    return configuration;
  }

  public void setConfiguration(String configuration) {
    this.configuration = configuration;
  }
}
