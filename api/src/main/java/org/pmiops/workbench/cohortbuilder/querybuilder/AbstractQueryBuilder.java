package org.pmiops.workbench.cohortbuilder.querybuilder;

import com.google.cloud.bigquery.QueryJobConfiguration;
import org.pmiops.workbench.model.Modifier;

import java.util.List;
import java.util.UUID;

/**
 * AbstractQueryBuilder is an object that builds {@link QueryJobConfiguration}
 * for BigQuery.
 */
public abstract class AbstractQueryBuilder {

  /**
   * Build a {@link QueryJobConfiguration} from the specified
   * {@link QueryParameters} provided.
   *
   * @param parameters
   * @return
   */
  public abstract QueryJobConfiguration buildQueryJobConfig(QueryParameters parameters);

  public String buildModifierSql(String query, List<Modifier> modifiers, FactoryKey key) {
    if (modifiers.isEmpty()) {
      return query.replace("${modifierDistinct}", "")
        .replace("${modifierColumns}", "");
    }
    return "";
  }

  public abstract FactoryKey getType();

  protected String getUniqueNamedParameterPostfix() {
    return UUID.randomUUID().toString().replaceAll("-", "");
  }
}
