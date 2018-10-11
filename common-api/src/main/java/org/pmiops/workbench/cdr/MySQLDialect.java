package org.pmiops.workbench.cdr;

import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.type.StandardBasicTypes;

public class MySQLDialect extends org.hibernate.dialect.MySQL57InnoDBDialect {

  public MySQLDialect() {
    super();
    // Define the custom "match" function to use "match(column) against (<string> in boolean mode)"
    // for MySQL.
    // Because MATCH returns a number, we need to have this function use DOUBLE.

    registerFunction("match", new SQLFunctionTemplate(StandardBasicTypes.DOUBLE,
        "match(?1) against  (?2 in boolean mode)"));

    registerFunction("matchConcept", new SQLFunctionTemplate(StandardBasicTypes.DOUBLE,
        "match(concept_name, concept_code, vocabulary_id, synonyms) against (?1 in boolean mode)"));
  }

}
