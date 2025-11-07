package org.pmiops.workbench.reporting.insertion;

import java.util.function.Function;
import org.pmiops.workbench.model.ReportingWorkspaceUser;

public enum WorkspaceUserColumnValueExtractor
    implements ColumnValueExtractor<ReportingWorkspaceUser> {
  WORKSPACE_ID("workspace_id", ReportingWorkspaceUser::getWorkspaceId),
  USER_ID("user_id", ReportingWorkspaceUser::getUserId),
  ROLE("role", ReportingWorkspaceUser::getRole);

  public static final String TABLE_NAME = "workspace_user";

  private final String parameterName;
  private final Function<ReportingWorkspaceUser, Object> rowToInsertValueFunction;

  WorkspaceUserColumnValueExtractor(
      String parameterName, Function<ReportingWorkspaceUser, Object> rowToInsertValueFunction) {
    this.parameterName = parameterName;
    this.rowToInsertValueFunction = rowToInsertValueFunction;
  }

  @Override
  public String getParameterName() {
    return parameterName;
  }

  @Override
  public Function<ReportingWorkspaceUser, Object> getRowToInsertValueFunction() {
    return rowToInsertValueFunction;
  }
}
