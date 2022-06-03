package org.pmiops.workbench.db.dao;

import java.math.BigInteger;
import java.util.List;
import javax.transaction.Transactional;
import org.pmiops.workbench.db.model.DbRdrExport;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface RdrExportDao extends CrudRepository<DbRdrExport, Long> {

  String USER_IDS_TO_EXPORT_QUERY =
      "select u.user_id from user u LEFT JOIN "
          + "rdr_export rdr on w.workspace_id = rdr.entity_id and rdr.entity_type = 1 where "
          + "u.last_modified_time > rdr.last_export_date or rdr.entity_id IS NULL "
          + "and NOT u.username IN :excludeUsers";
  String UNCHANGED_USER_IDS_QUERY =
      "select entity_id from rdr_export where entity_type = 1 and entity_id not in ("
          + USER_IDS_TO_EXPORT_QUERY
          + ")";
  String WORKSPACE_IDS_TO_EXPORT_QUERY =
      "select w.workspace_id from workspace w LEFT JOIN "
          + "rdr_export rdr on w.workspace_id = rdr.entity_id and rdr.entity_type = 2 where "
          + "w.last_modified_time > rdr.last_export_date or rdr.entity_id IS NULL";
  String UNCHANGED_WORKSPACE_IDS_QUERY =
      "select entity_id from rdr_export where entity_type = 2 and entity_id not in ("
          + WORKSPACE_IDS_TO_EXPORT_QUERY
          + ")";

  @Query(nativeQuery = true, value = USER_IDS_TO_EXPORT_QUERY)
  List<BigInteger> findDbUserIdsToExport(@Param("excludeUsers") List<String> excludeUsers);

  @Query(value = UNCHANGED_USER_IDS_QUERY, nativeQuery = true)
  List<BigInteger> findAllUnchangedDbUserIds(@Param("excludeUsers") List<String> excludeUsers);

  @Query(value = UNCHANGED_USER_IDS_QUERY + " limit :limit", nativeQuery = true)
  List<BigInteger> findTopUnchangedDbUserIds(
      @Param("excludeUsers") List<String> excludeUsers, @Param("limit") Integer limit);

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
