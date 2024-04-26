package org.pmiops.workbench.cdr;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.community.dialect.MySQL5Dialect;
import org.hibernate.type.BasicTypeRegistry;
import org.hibernate.type.StandardBasicTypes;

public class CommonTestDialect extends MySQL5Dialect {
  public CommonTestDialect() {
    super();
  }

  @Override
  public void initializeFunctionRegistry(FunctionContributions functionContributions) {
    // For in-memory tests, use LOCATE for full text searches, replacing the "+" chars we
    // added with nothing; this will work for single-word query patterns only.
    // Because LOCATE / MATCH returns a number, we need to have this function use DOUBLE.

    super.initializeFunctionRegistry(functionContributions);
    BasicTypeRegistry basicTypeRegistry =
        functionContributions.getTypeConfiguration().getBasicTypeRegistry();

    functionContributions
        .getFunctionRegistry()
        .registerPattern(
            "match",
            "LOCATE(REPLACE(?2, '+'), ?1)",
            basicTypeRegistry.resolve(StandardBasicTypes.DOUBLE));

    functionContributions
        .getFunctionRegistry()
        .registerPattern(
            "matchConcept",
            "LOCATE(REPLACE(REPLACE(?5, '+'),'*'), CONCAT_WS(' ', ?1, ?2, ?3, ?4))",
            basicTypeRegistry.resolve(StandardBasicTypes.DOUBLE));
  }

  @Override
  public boolean dropConstraints() {
    // We don't need to drop constraints before dropping tables, that just leads to error
    // messages about missing tables when we don't have a schema in the database
    return false;
  }
}
