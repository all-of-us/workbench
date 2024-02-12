package org.pmiops.workbench.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "config")
public class DbConfig {

  public static final String MAIN_CONFIG_ID = "main";
  // TODO: consider whether we need different CDR schema config for different CDR versions in future
  public static final String CDR_BIGQUERY_SCHEMA_CONFIG_ID = "cdrBigQuerySchema";
  public static final String FEATURED_WORKSPACES_CONFIG_ID = "featuredWorkspaces";

  private String configId;
  private String configuration;

  @Id
  @Column(name = "config_id")
  public String getConfigId() {
    return configId;
  }

  public DbConfig setConfigId(String configId) {
    this.configId = configId;
    return this;
  }

  @Column(name = "configuration")
  public String getConfiguration() {
    return configuration;
  }

  public DbConfig setConfiguration(String configuration) {
    this.configuration = configuration;
    return this;
  }
}
