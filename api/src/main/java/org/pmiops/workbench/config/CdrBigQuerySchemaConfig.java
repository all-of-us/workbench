package org.pmiops.workbench.config;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

public class CdrBigQuerySchemaConfig {

  public String description;
  public String version;
  public Map<String, TableConfig> metadataTables;
  public Map<String, TableConfig> cohortTables;

  public static class TableConfig {
    public List<ColumnConfig> columns;
  }

  public static class ColumnConfig {
    public ColumnType type;
    public String name;
    public ColumnMode mode;
    public String description;
    public Boolean primaryKey;
    public String foreignKey;
  }

  public static enum ColumnMode {
    @SerializedName("nullable") NULLABLE,
    @SerializedName("required") REQUIRED
  }

  public static enum ColumnType {
    @SerializedName("string") STRING,
    @SerializedName("integer") INTEGER,
    @SerializedName("date") DATE,
    @SerializedName("timestamp") TIMESTAMP,
    @SerializedName("float") FLOAT
  }

}
