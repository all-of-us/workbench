package org.pmiops.workbench.db.dao;

import java.sql.Timestamp;
import java.util.List;
import org.pmiops.workbench.db.model.DbReportingUploadVerification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ReportingUploadVerificationDao extends CrudRepository<DbReportingUploadVerification, Long> {
  List<DbReportingUploadVerification> findBySnapshotTimestamp(Timestamp snapshotTimestamp);

  // Update the uploaded status for a specific table and timestamp
  @Modifying
  @Query("UPDATE DbReportingUploadVerification r SET r.uploaded = :uploaded WHERE r.tableName = :tableName AND r.snapshotTimestamp = :snapshotTimestamp")
  int updateUploadedStatus(@Param("tableName") String tableName, @Param("snapshotTimestamp") Timestamp snapshotTimestamp, @Param("uploaded") Boolean uploaded);
}
