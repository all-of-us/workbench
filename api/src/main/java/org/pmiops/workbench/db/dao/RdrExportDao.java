package org.pmiops.workbench.db.dao;

import java.math.BigInteger;
import java.util.List;
import javax.transaction.Transactional;
import org.pmiops.workbench.db.model.DbRdrExport;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface RdrExportDao extends CrudRepository<DbRdrExport, Long> {

  static final String WORKSPACE_IDS_TO_EXPORT_QUERY =
      "select w.workspace_id from  workspace w LEFT JOIN "
          + "rdr_export rdr on w.workspace_id = rdr.entity_id and rdr.entity_type = 2 where "
          + "w.last_modified_time > rdr.last_export_date or rdr.entity_id IS NULL";
  static final String UNCHANGED_WORKSPACE_IDS_QUERY =
      "select entity_id from rdr_export where entity_type = 2 and entity_id not in ("
          + WORKSPACE_IDS_TO_EXPORT_QUERY
          + ")";

  @Query(
      nativeQuery = true,
      value =
          "select u.user_id from user u LEFT JOIN rdr_export rdr on"
              + " u.user_id = rdr.entity_id and rdr.entity_type = 1 where "
              + "u.last_modified_time > rdr.last_Export_date or rdr.export_id is null")
  List<BigInteger> findDbUserIdsToExport();

  @Query(nativeQuery = true, value = WORKSPACE_IDS_TO_EXPORT_QUERY)
  List<BigInteger> findDbWorkspaceIdsToExport();

  @Query(value = UNCHANGED_WORKSPACE_IDS_QUERY, nativeQuery = true)
  List<BigInteger> findAllUnchangedDbWorkspaceIds();

  @Query(value = UNCHANGED_WORKSPACE_IDS_QUERY + " limit :limit", nativeQuery = true)
  List<BigInteger> findTopUnchangedDbWorkspaceIds(@Param("limit") Integer limit);

  List<DbRdrExport> findAllByEntityType(short entityType);

  DbRdrExport findByEntityTypeAndEntityId(short entityType, long entityId);

  @Transactional
  void deleteDbRdrExportsByEntityTypeAndEntityId(short entityType, Long entityId);
}
