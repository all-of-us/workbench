package org.pmiops.workbench.actionaudit.bucket;

import static org.pmiops.workbench.exfiltration.ExfiltrationUtils.THRESHOLD_MB;

import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.TableResult;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.inject.Provider;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchConfig.BucketAuditConfig;
import org.pmiops.workbench.model.BucketAuditEntry;
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
          + "  resource.labels.project_id AS project_id,\n"
          + "  resource.labels.bucket_name AS bucket_name, \n"
          + "  min(timestamp) AS min_time, \n"
          + "  max(timestamp) AS max_time \n"
          + "FROM \n"
          + "  %s \n"
          + "WHERE \n"
          + "  datetime(timestamp) > datetime_sub(CURRENT_DATETIME(), INTERVAL 8 HOUR) \n"
          + "  AND protopayload_auditlog.methodName=\"storage.objects.create\" \n"
          + "  AND protopayload_auditlog.resourceName LIKE \"projects/_/buckets/%%\" \n"
          + "GROUP BY \n"
          + " pet_account, \n"
          + " project_id, \n"
          + " bucket_name \n"
          + "HAVING \n"
          + " file_lengths > @THRESHOLD ";

  @Autowired
  public BucketAuditQueryServiceImpl(
      Provider<WorkbenchConfig> workbenchConfigProvider, BigQueryService bigQueryService) {
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.bigQueryService = bigQueryService;
  }

  @Override
  public List<BucketAuditEntry> queryBucketFileInformationGroupedByPetAccount() {

    final String queryString = String.format(QUERY, getTableName());

    final QueryJobConfiguration queryJobConfiguration =
        QueryJobConfiguration.newBuilder(queryString)
            .addNamedParameter("THRESHOLD", QueryParameterValue.int64(THRESHOLD_MB))
            .build();

    final TableResult result = bigQueryService.executeQuery(queryJobConfiguration);
    return tableResultToLogEntries(result);
  }

  private String getTableName() {
    final BucketAuditConfig bucketAuditConfig = workbenchConfigProvider.get().bucketAudit;
    return String.format(
        "`%s.%s.%s`",
        bucketAuditConfig.logProjectId,
        bucketAuditConfig.bigQueryDataset,
        bucketAuditConfig.bigQueryTable);
  }

  private List<BucketAuditEntry> tableResultToLogEntries(TableResult tableResult) {
    return StreamSupport.stream(tableResult.iterateAll().spliterator(), false)
        .map(this::fieldValueListToBucketAuditLogEntry)
        .collect(Collectors.toList());
  }

  private BucketAuditEntry fieldValueListToBucketAuditLogEntry(FieldValueList row) {
    BucketAuditEntry bucketAuditEntry = new BucketAuditEntry();
    FieldValues.getString(row, "pet_account").ifPresent(bucketAuditEntry::setPetAccount);
    FieldValues.getLong(row, "file_lengths").ifPresent(bucketAuditEntry::setFileLengths);
    FieldValues.getString(row, "project_id").ifPresent(bucketAuditEntry::setGoogleProjectId);
    FieldValues.getString(row, "bucket_name").ifPresent(bucketAuditEntry::setBucketName);

    FieldValues.getDateTime(row, "min_time")
        .map(OffsetDateTime::toString)
        .ifPresent(bucketAuditEntry::setMinTime);
    FieldValues.getDateTime(row, "max_time")
        .map(OffsetDateTime::toString)
        .ifPresent(bucketAuditEntry::setMaxTime);
    return bucketAuditEntry;
  }
}
