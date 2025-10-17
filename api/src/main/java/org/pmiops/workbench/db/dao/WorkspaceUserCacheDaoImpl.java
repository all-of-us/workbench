package org.pmiops.workbench.db.dao;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.stream.StreamSupport;
import org.pmiops.workbench.db.model.DbWorkspaceUserCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class WorkspaceUserCacheDaoImpl implements WorkspaceUserCacheDaoCustom {
  private final JdbcTemplate jdbcTemplate;

  @Autowired
  public WorkspaceUserCacheDaoImpl(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Override
  public int upsertAll(Iterable<DbWorkspaceUserCache> entries) {
    String sql =
        "INSERT INTO workspace_user_cache (workspace_id, user_id, role, last_updated) "
            + "VALUES (?, ?, ?, ?) "
            + "ON DUPLICATE KEY UPDATE role = VALUES(role), last_updated = VALUES(last_updated)";

    List<DbWorkspaceUserCache> entryList =
        entries instanceof List
            ? (List<DbWorkspaceUserCache>) entries
            : StreamSupport.stream(entries.spliterator(), false).toList();

    if (entryList.isEmpty()) {
      return 0;
    }

    int[][] results =
        jdbcTemplate.batchUpdate(
            sql,
            entryList,
            entryList.size(),
            (PreparedStatement ps, DbWorkspaceUserCache entry) -> {
              ps.setLong(1, entry.getWorkspaceId());
              ps.setLong(2, entry.getUserId());
              ps.setString(3, entry.getRole());
              ps.setTimestamp(4, entry.getLastUpdated());
            });

    // Sum up all affected rows from the batch results
    int totalRows = 0;
    for (int[] batch : results) {
      for (int count : batch) {
        totalRows += count;
      }
    }
    return totalRows;
  }
}
