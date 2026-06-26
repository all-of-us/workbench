package org.pmiops.workbench.reporting.insertion;

import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.toInsertRowString;

import java.util.function.Function;
import org.pmiops.workbench.model.ReportingWorkspaceBucketArchive;

public enum WorkspaceBucketArchiveColumnValueExtractor
    implements ColumnValueExtractor<ReportingWorkspaceBucketArchive> {
  LEGACY_WORKSPACE_ID("legacy_workspace_id", ReportingWorkspaceBucketArchive::getLegacyWorkspaceId),
  GCS_PATH("gcs_path", ReportingWorkspaceBucketArchive::getGcsPath),
  CREATED("created", fs -> toInsertRowString(fs.getCreated())),
  STATUS("status", ReportingWorkspaceBucketArchive::getStatus);

  private final String parameterName;
  private final Function<ReportingWorkspaceBucketArchive, Object> rowToInsertValueFunction;

  WorkspaceBucketArchiveColumnValueExtractor(
      String parameterName,
      Function<ReportingWorkspaceBucketArchive, Object> rowToInsertValueFunction) {
    this.parameterName = parameterName;
    this.rowToInsertValueFunction = rowToInsertValueFunction;
  }

  @Override
  public String getParameterName() {
    return parameterName;
  }

  @Override
  public Function<ReportingWorkspaceBucketArchive, Object> getRowToInsertValueFunction() {
    return rowToInsertValueFunction;
  }
}
