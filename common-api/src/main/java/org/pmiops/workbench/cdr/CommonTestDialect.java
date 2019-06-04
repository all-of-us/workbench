package org.pmiops.workbench.cdr;

import org.hibernate.dialect.MySQL57InnoDBDialect;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.type.StandardBasicTypes;

public class CommonTestDialect extends MySQL57InnoDBDialect {

  public CommonTestDialect() {
    super();
    // For in-memory tests, use LOCATE for full text searches, replacing the "+" chars we
    // added with nothing; this will work for single-word query patterns only.
    // Because LOCATE / MATCH returns a number, we need to have this function use DOUBLE.

    registerFunction(
        "match",
        new SQLFunctionTemplate(StandardBasicTypes.DOUBLE, "LOCATE(REPLACE(?2, '+'), ?1)"));

    registerFunction(
        "matchConcept",
        new SQLFunctionTemplate(
            StandardBasicTypes.DOUBLE,
            "LOCATE(REPLACE(REPLACE(?5, '+'),'*'), CONCAT_WS(' ', ?1, ?2, ?3, ?4))"));
  }

  @Override
  public boolean dropConstraints() {
    // We don't need to drop constraints before dropping tables, that just leads to error
    // messages about missing tables when we don't have a schema in the database
    return false;
  }
}
