package org.pmiops.workbench.actionaudit.bucket;

import java.util.List;

public interface BucketAuditQueryService {

  /**
   * Query BQ for file information of a specific bucket grouped by the pet account
   *
   * @param bucket Google project bucket name
   * @param googleProjectId The google project ID
   * @return A List of entries containing the pet account and the sum of the length of the created
   *     files for each pet account
   */
  List<BucketAuditEntry> queryBucketFileInformationGroupedByPetAccount(
      String bucket, String googleProjectId);
}
