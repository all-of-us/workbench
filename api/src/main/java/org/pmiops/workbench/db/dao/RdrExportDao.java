package org.pmiops.workbench.db.dao;

import java.math.BigInteger;
import java.util.List;
import javax.transaction.Transactional;
import org.pmiops.workbench.db.model.DbRdrExport;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface RdrExportDao extends CrudRepository<DbRdrExport, Long> {

  public final String workspaceIdsToExportQuery =
      "select w.workspace_id from  workspace w LEFT JOIN "
          + "rdr_export rdr on w.workspace_id = rdr.entity_id and rdr.entity_type = 2 where "
          + "w.last_modified_time > rdr.last_export_date or rdr.entity_id IS NULL";
  public final String unchangedWorkspaceIdsQuery =
      "select workspace_id from workspace where workspace_id not in ("
          + workspaceIdsToExportQuery
          + ")";

  @Query(nativeQuery = true, value = workspaceIdsToExportQuery)
  List<BigInteger> findDbUserIdsToExport();

  @Query(
      nativeQuery = true,
      value =
          "select w.workspace_id from  workspace w LEFT JOIN "
              + "rdr_export rdr on w.workspace_id = rdr.entity_id and rdr.entity_type = 2 where "
              + "w.last_modified_time > rdr.last_export_date or rdr.entity_id IS NULL")
  List<BigInteger> findDbWorkspaceIdsToExport();

  @Query(value = unchangedWorkspaceIdsQuery, nativeQuery = true)
  List<BigInteger> findAllUnchangedDbWorkspaceIds();

  @Query(value = unchangedWorkspaceIdsQuery + " limit :limit", nativeQuery = true)
  List<BigInteger> findTopUnchangedDbWorkspaceIds(@Param("limit") Integer limit);

  DbRdrExport findByEntityTypeAndEntityId(short entity_type, long entity_id);

  @Transactional
  void deleteDbRdrExportsByEntityTypeAndEntityId(short entity_type, Long entity_id);
}
