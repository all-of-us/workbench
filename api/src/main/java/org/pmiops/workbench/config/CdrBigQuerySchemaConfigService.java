package org.pmiops.workbench.config;

import javax.inject.Provider;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfig.ColumnConfig;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfig.TableConfig;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CdrBigQuerySchemaConfigService {

  private static final String DOMAIN_CONCEPT_STANDARD = "standard";
  private static final String DOMAIN_CONCEPT_SOURCE = "source";

  public static class ConceptColumns {
    private final ColumnConfig standardConceptColumn;
    private final ColumnConfig sourceConceptColumn;

    public ConceptColumns(ColumnConfig standardConceptColumn, ColumnConfig sourceConceptColumn) {
      this.standardConceptColumn = standardConceptColumn;
      this.sourceConceptColumn = sourceConceptColumn;
    }

    public ColumnConfig getStandardConceptColumn() {
      return standardConceptColumn;
    }

    public ColumnConfig getSourceConceptColumn() {
      return sourceConceptColumn;
    }
  }

  private final Provider<CdrBigQuerySchemaConfig> configProvider;

  @Autowired
  public CdrBigQuerySchemaConfigService(Provider<CdrBigQuerySchemaConfig> configProvider) {
    this.configProvider = configProvider;
  }

  public CdrBigQuerySchemaConfig getConfig() {
    return configProvider.get();
  }

  public ConceptColumns getConceptColumns(String tableName) {
    TableConfig tableConfig = getConfig().cohortTables.get(tableName);
    if (tableConfig == null) {
      throw new ServerErrorException("Couldn't find table config for " + tableName);
    }
    return getConceptColumns(tableConfig, tableName);
  }

  public ConceptColumns getConceptColumns(TableConfig tableConfig, String tableName) {
    ColumnConfig standardConceptColumn = null;
    ColumnConfig sourceConceptColumn = null;
    for (ColumnConfig columnConfig : tableConfig.columns) {
      if (DOMAIN_CONCEPT_STANDARD.equals(columnConfig.domainConcept)) {
        standardConceptColumn = columnConfig;
        if (sourceConceptColumn != null) {
          break;
        }
      } else if (DOMAIN_CONCEPT_SOURCE.equals(columnConfig.domainConcept)) {
        sourceConceptColumn = columnConfig;
        if (standardConceptColumn != null) {
          break;
        }
      }
    }
    if (standardConceptColumn == null || sourceConceptColumn == null) {
      throw new ServerErrorException(
          "Could not find standard and source concept columns for table " + tableName);
    }
    return new ConceptColumns(standardConceptColumn, sourceConceptColumn);
  }
}
