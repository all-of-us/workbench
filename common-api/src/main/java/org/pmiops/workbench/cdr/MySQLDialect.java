package org.pmiops.workbench.cdr;

import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.type.StandardBasicTypes;

public class MySQLDialect extends org.hibernate.dialect.MySQLDialect {

  public MySQLDialect() {
    super();
    // Define the custom "match" function to use "match(column) against (<string> in boolean mode)"
    // for MySQL.
    // For some weird reason, we need to have this function use DOUBLE; see
    // https://pavelmakhov.com/2016/09/jpa-custom-function
    registerFunction("match", new SQLFunctionTemplate(StandardBasicTypes.DOUBLE,
        "match(?1) against  (?2 in boolean mode)"));
  }

}
