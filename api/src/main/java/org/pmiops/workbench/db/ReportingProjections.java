package org.pmiops.workbench.db;

import java.sql.Timestamp;

public class ReportingProjections {
  public interface Workspace {
    Long getWorkspaceId();

    String getName();

    // @Convert(converter = SqlTimestampAttributeConverter.class)
    // eric - based on this SO answer, it seems like the return type in the interface has to match
    // the underlying class
    // https://stackoverflow.com/questions/46825928/java-lang-illegalargumentexception-projection-type-must-be-an-interface-error
    Timestamp getCreationTime();
  }
}
