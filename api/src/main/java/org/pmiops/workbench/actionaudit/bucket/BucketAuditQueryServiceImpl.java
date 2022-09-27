package org.pmiops.workbench.actionaudit.bucket;

import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import javax.inject.Provider;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.BucketAuditConfig;
import org.pmiops.workbench.utils.FieldValues;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BucketAuditQueryServiceImpl implements BucketAuditQueryService {

  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final BigQueryService bigQueryService;

  private static final String QUERY =
      "SELECT \n"
          + "  protopayload_auditlog.authenticationInfo.principalEmail AS pet_account, \n"
          + "  sum(CHARACTER_LENGTH(REGEXP_EXTRACT(protopayload_auditlog.resourceName, r'/([^/]+)/?$'))) as file_lengths, \n"
          + "  min(timestamp) AS MIN_TIME, \n"
          + "  max(timestamp) AS MAX_TIME \n"
          + "FROM \n"
          + "  %s \n"
          + "WHERE \n"
          + "  datetime(timestamp) > datetime_sub(CURRENT_DATETIME(), INTERVAL 24 HOUR) \n"
          + "  AND protopayload_auditlog.methodName=\"storage.objects.create\" \n"
          + "  AND protopayload_auditlog.resourceName LIKE @bucket_name \n"
          + "  AND resource.labels.project_id = @google_project\n"
          + "GROUP BY \n"
          + " pet_account ";

  @Autowired
  public BucketAuditQueryServiceImpl(
      Provider<WorkbenchConfig> workbenchConfigProvider, BigQueryService bigQueryService) {
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.bigQueryService = bigQueryService;
  }

  @Override
  public List<BucketAuditEntry> queryBucketFileInformationGroupedByPetAccount(
      String bucket, String googleProjectId) {

    final String queryString = String.format(QUERY, getTableName());
    final String bucketName = String.format("%%projects/_/buckets/%s%%", bucket);

    final QueryJobConfiguration queryJobConfiguration =
        QueryJobConfiguration.newBuilder(queryString)
            .setNamedParameters(getNamedParametersMap(bucketName, googleProjectId))
            .build();

    final TableResult result = bigQueryService.executeQuery(queryJobConfiguration);
    return tableResultToLogEntries(result);
  }

  private Map<String, QueryParameterValue> getNamedParametersMap(
      String bucketName, String googleProject) {
    return ImmutableMap.<String, QueryParameterValue>builder()
        .put("bucket_name", QueryParameterValue.string(bucketName))
        .put("google_project", QueryParameterValue.string(googleProject))
        .build();
  }

  private String getTableName() {
    final BucketAuditConfig bucketAuditConfig = workbenchConfigProvider.get().bucketAudit;
    return String.format(
        "`%s.%s.%s`",
        bucketAuditConfig.logProjectId,
        bucketAuditConfig.bigQueryDataset,
        bucketAuditConfig.bigQueryTable);
  }

  private ImmutableList<BucketAuditEntry> tableResultToLogEntries(TableResult tableResult) {
    return StreamSupport.stream(tableResult.iterateAll().spliterator(), false)
        .map(this::fieldValueListToBucketAuditLogEntry)
        .collect(ImmutableList.toImmutableList());
  }

  private BucketAuditEntry fieldValueListToBucketAuditLogEntry(FieldValueList row) {
    BucketAuditEntry bucketAuditEntry = new BucketAuditEntry();
    FieldValues.getString(row, "pet_account").ifPresent(bucketAuditEntry::setPetAccount);
    FieldValues.getLong(row, "file_lengths").ifPresent(bucketAuditEntry::setFileLengths);
    FieldValues.getDateTime(row, "MIN_TIME").ifPresent(bucketAuditEntry::setMinTime);
    FieldValues.getDateTime(row, "MAX_TIME").ifPresent(bucketAuditEntry::setMaxTime);
    return bucketAuditEntry;
  }
}
