package org.pmiops.workbench.reporting.insertion;

import java.util.function.Function;
import org.pmiops.workbench.model.ReportingUserPartnerDiscoverySource;

/*
 * Column data and metadata convertors for BigQuery user_partner_discovery_source table in reporting
 * dataset.
 */
public enum UserPartnerDiscoverySourceColumnValueExtractor
    implements ColumnValueExtractor<ReportingUserPartnerDiscoverySource> {
  USER_ID("user_id", ReportingUserPartnerDiscoverySource::getUserId),
  ANSWER("answer", ReportingUserPartnerDiscoverySource::getAnswer),
  OTHER_TEXT("other_text", ReportingUserPartnerDiscoverySource::getOtherText);

  public static final String TABLE_NAME = "user_partner_discovery_source";

  private final String parameterName;
  private final Function<ReportingUserPartnerDiscoverySource, Object> rowToInsertValueFunction;

  UserPartnerDiscoverySourceColumnValueExtractor(
      String parameterName,
      Function<ReportingUserPartnerDiscoverySource, Object> rowToInsertValueFunction) {
    this.parameterName = parameterName;
    this.rowToInsertValueFunction = rowToInsertValueFunction;
  }

  @Override
  public String getParameterName() {
    return parameterName;
  }

  @Override
  public Function<ReportingUserPartnerDiscoverySource, Object> getRowToInsertValueFunction() {
    return rowToInsertValueFunction;
  }
}
