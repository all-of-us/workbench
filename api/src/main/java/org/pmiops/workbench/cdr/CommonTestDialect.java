package org.pmiops.workbench.cdr;

import org.hibernate.community.dialect.MySQL5Dialect;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.type.StandardBasicTypes;

public class CommonTestDialect extends MySQL5Dialect {
  public CommonTestDialect() {
    super();
    // For in-memory tests, use LOCATE for full text searches, replacing the "+" chars we
    // added with nothing; this will work for single-word query patterns only.
    // Because LOCATE / MATCH returns a number, we need to have this function use DOUBLE.

    registerFunction(
        "match",
        new StandardSQLFunction("LOCATE(REPLACE(?2, '+'), ?1)", StandardBasicTypes.DOUBLE));

    registerFunction(
        "matchConcept",
        new StandardSQLFunction(
            "LOCATE(REPLACE(REPLACE(?5, '+'),'*'), CONCAT_WS(' ', ?1, ?2, ?3, ?4))",
            StandardBasicTypes.DOUBLE));
  }

  @Override
  public void initializeFunctionRegistry(FunctionContributions functionContributions) {
    super.initializeFunctionRegistry(functionContributions);

    BasicTypeRegistry basicTypeRegistry = functionContributions.getTypeConfiguration().getBasicTypeRegistry();

    functionContributions.getFunctionRegistry().registerPattern(
        "hstore_find",
        "(?1 -> ?2 = ?3)",
        basicTypeRegistry.resolve( StandardBasicTypes.BOOLEAN ));
    // ...
  }

  @Override
  public boolean dropConstraints() {
    // We don't need to drop constraints before dropping tables, that just leads to error
    // messages about missing tables when we don't have a schema in the database
    return false;
  }
}
