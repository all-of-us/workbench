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
      "SELECT u.user_id FROM user u LEFT JOIN "
          + "rdr_export rdr ON (u.user_id = rdr.entity_id AND rdr.entity_type = 1) where "
          + "(u.last_modified_time > rdr.last_export_date OR rdr.entity_id IS NULL) "
          + "AND u.email NOT IN :excludeUsers";
  String UNCHANGED_USER_IDS_QUERY =
      "SELECT entity_id FROM rdr_export where entity_type = 1 AND entity_id NOT IN ("
          + USER_IDS_TO_EXPORT_QUERY
          + ")";
  String WORKSPACE_IDS_TO_EXPORT_QUERY =
      "SELECT w.workspace_id FROM workspace w LEFT JOIN "
          + "rdr_export rdr ON (w.workspace_id = rdr.entity_id AND rdr.entity_type = 2) where "
          + "w.last_modified_time > rdr.last_export_date OR rdr.entity_id IS NULL";
  String UNCHANGED_WORKSPACE_IDS_QUERY =
      "SELECT entity_id FROM rdr_export where entity_type = 2 AND entity_id NOT IN ("
          + WORKSPACE_IDS_TO_EXPORT_QUERY
          + ")";

  @Query(nativeQuery = true, value = USER_IDS_TO_EXPORT_QUERY)
  List<BigInteger> findDbUserIdsToExport(@Param("excludeUsers") List<String> excludeUsers);

  @Query(value = UNCHANGED_USER_IDS_QUERY, nativeQuery = true)
  List<BigInteger> findAllUnchangedDbUserIds(@Param("excludeUsers") List<String> excludeUsers);

  @Query(nativeQuery = true, value = WORKSPACE_IDS_TO_EXPORT_QUERY)
  List<BigInteger> findDbWorkspaceIdsToExport();

  @Query(value = UNCHANGED_WORKSPACE_IDS_QUERY, nativeQuery = true)
  List<BigInteger> findAllUnchangedDbWorkspaceIds();

  List<DbRdrExport> findAllByEntityType(short entityType);

  DbRdrExport findByEntityTypeAndEntityId(short entityType, long entityId);

  @Transactional
  void deleteDbRdrExportsByEntityTypeAndEntityId(short entityType, Long entityId);
}
