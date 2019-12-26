package org.pmiops.workbench.db.dao;

import java.math.BigInteger;
import java.util.List;
import org.pmiops.workbench.db.model.DbRDRExport;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface RDRExportDao extends CrudRepository<DbRDRExport, Long> {

  @Query(
      nativeQuery = true,
      value =
          "select u.user_id from user u LEFT JOIN rdr_export rdr on"
              + " u.user_id = rdr.export_id and rdr.entity = 1 where u.last_modified_time > rdr.last_Export_date or rdr.export_id is null")
  List<BigInteger> findDbUserToExport();

  @Query(
      nativeQuery = true,
      value =
          "select w.workspace_id from  workspace w LEFT JOIN "
              + "rdr_export rdr on w.workspace_id = rdr.entity_id and rdr.entity = 2 where "
              + "w.last_modified_time > rdr.last_export_date or rdr.entity_id IS NULL")
  List<BigInteger> findDbWorkspaceToExport();

  DbRDRExport findByEntityAndId(short entity, int id);
}
