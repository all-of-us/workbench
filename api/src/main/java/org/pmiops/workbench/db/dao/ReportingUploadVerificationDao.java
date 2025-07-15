package org.pmiops.workbench.db.dao;

import java.util.List;
import org.pmiops.workbench.db.model.DbReportingUploadVerification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface ReportingUploadVerificationDao
    extends CrudRepository<DbReportingUploadVerification, Long> {
  List<DbReportingUploadVerification> findBySnapshotTimestamp(Long snapshotTimestamp);

  // Update the uploaded status for a specific table and timestamp
  @Modifying
  @Query(
      "UPDATE DbReportingUploadVerification r SET r.uploaded = :uploaded WHERE r.tableName = :tableName AND r.snapshotTimestamp = :snapshotTimestamp")
  int updateUploadedStatus(
      @Param("tableName") String tableName,
      @Param("snapshotTimestamp") Long snapshotTimestamp,
      @Param("uploaded") Boolean uploaded);

  // Create a new entry for a given table and timestamp
  @Modifying
  @Query(
      "INSERT INTO DbReportingUploadVerification (tableName, snapshotTimestamp) VALUES (:tableName, :snapshotTimestamp)")
  int createVerificationEntry(
      @Param("tableName") String tableName, @Param("snapshotTimestamp") Long snapshotTimestamp);
}
