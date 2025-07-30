package org.pmiops.workbench.workspaces;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceUserCacheDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.DbWorkspaceUserCache;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceAccessEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkspaceUserCacheServiceImpl implements WorkspaceUserCacheService {

  private final UserDao userDao;
  private final WorkspaceDao workspaceDao;
  private final WorkspaceUserCacheDao workspaceUserCacheDao;

  @Autowired
  public WorkspaceUserCacheServiceImpl(
      UserDao userDao, WorkspaceDao workspaceDao, WorkspaceUserCacheDao workspaceUserCacheDao) {
    this.userDao = userDao;
    this.workspaceDao = workspaceDao;
    this.workspaceUserCacheDao = workspaceUserCacheDao;
  }

  @Override
  public List<DbWorkspace> findAllActiveWorkspaceNamespacesNeedingCacheUpdate() {
    return workspaceDao.findAllActiveWorkspaceNamespacesNeedingCacheUpdate();
  }

  @Override
  @Transactional
  public void updateWorkspaceUserCache(
      Map<Long, Map<String, RawlsWorkspaceAccessEntry>> newEntriesByWorkspaceId) {
    var userEmailMap =
        userDao.findUsersByEmail(
            newEntriesByWorkspaceId.values().stream()
                .flatMap(entry -> entry.keySet().stream())
                .toList());

    workspaceUserCacheDao.deleteAllForWorkspaces(newEntriesByWorkspaceId.keySet());

    var newRecords =
        newEntriesByWorkspaceId.entrySet().stream()
            .flatMap(
                workspaceWithAcl ->
                    getWorkspaceUserCacheEntries(
                        workspaceWithAcl.getKey(), workspaceWithAcl.getValue(), userEmailMap))
            .toList();
    workspaceUserCacheDao.saveAll(newRecords);
  }

  private Stream<DbWorkspaceUserCache> getWorkspaceUserCacheEntries(
      long workspaceId,
      Map<String, RawlsWorkspaceAccessEntry> acl,
      Map<String, DbUser> userEmailMap) {
    return acl.entrySet().stream()
        .map(
            aclItem ->
                new DbWorkspaceUserCache()
                    .setWorkspaceId(workspaceId)
                    .setUserId(userEmailMap.get(aclItem.getKey()).getUserId())
                    .setRole(aclItem.getValue().getAccessLevel())
                    .setLastUpdated(Timestamp.from(Instant.now())));
  }

  @Override
  public void removeInactiveWorkspaces() {
    workspaceUserCacheDao.deleteAllInactiveWorkspaces();
  }
}
