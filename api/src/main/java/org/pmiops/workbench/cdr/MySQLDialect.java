package org.pmiops.workbench.cdr;

import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.type.StandardBasicTypes;

public class MySQLDialect extends org.hibernate.community.dialect.MySQL5Dialect {

  public MySQLDialect() {
    super();
    // Define the custom "match" function to use "match(column) against (<string> in boolean mode)"
    // for MySQL.
    // Because MATCH returns a number, we need to have this function use DOUBLE.
    registerFunction(
        "myFunction",
        new StandardSQLFunction("match(?1) against  (?2 in boolean mode)", StandardBasicTypes.DOUBLE));

    // Define matchConcept to use "match(<concept>.concept_name, <concept>.concept_code,
    // <concept>.vocabulary_id,
    // <concept>.synonyms) against (<string> in boolean mode)". Clients must pass in each of the
    // corresponding fields -- conceptName, conceptCode, vocabularyId, synonymsStr --
    // to make JQL alias resolution work properly. Example:
    // matchConcept(c.conceptName, c.conceptCode, c.vocabularyId, c.synonymsStr, ?1) > 0
    registerFunction(
        "myFunction",
        new StandardSQLFunction("match(?1, ?2, ?3, ?4) against (?5 in boolean mode)", StandardBasicTypes.DOUBLE));
  }
}
