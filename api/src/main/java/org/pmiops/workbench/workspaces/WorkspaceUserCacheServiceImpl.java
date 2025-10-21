package org.pmiops.workbench.workspaces;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceUserCacheDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspaceUserCache;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceAccessEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkspaceUserCacheServiceImpl implements WorkspaceUserCacheService {
  private static final Logger log = Logger.getLogger(WorkspaceUserCacheServiceImpl.class.getName());
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
  public List<WorkspaceDao.WorkspaceUserCacheView> findAllActiveWorkspacesNeedingCacheUpdate() {
    return workspaceDao.findAllActiveWorkspaceNamespacesNeedingCacheUpdate();
  }

  @Override
  @Transactional
  public void updateWorkspaceUserCache(
      Map<Long, Map<String, RawlsWorkspaceAccessEntry>> newEntriesByWorkspaceId) {
    var usernameMap =
        userDao.getUsersMappedByUsernames(
            newEntriesByWorkspaceId.values().stream()
                .flatMap(entry -> entry.keySet().stream())
                .collect(Collectors.toSet()));

    var existingCacheEntries =
        workspaceUserCacheDao.findAllByWorkspaceIdIn(newEntriesByWorkspaceId.keySet());

    var newCacheEntries =
        newEntriesByWorkspaceId.entrySet().stream()
            .flatMap(
                workspaceWithAcl ->
                    getWorkspaceUserCacheEntries(
                        workspaceWithAcl.getKey(), workspaceWithAcl.getValue(), usernameMap))
            .toList();

    var entriesToDelete =
        existingCacheEntries.stream()
            .filter(
                existingEntry ->
                    newCacheEntries.stream()
                        .noneMatch(
                            newEntry ->
                                newEntry.getWorkspaceId() == existingEntry.getWorkspaceId()
                                    && newEntry.getUserId() == existingEntry.getUserId()))
            .toList();

    log.info(
        String.format(
            "Removing %d stale entries from workspace user cache", entriesToDelete.size()));
    workspaceUserCacheDao.deleteAllById(
        entriesToDelete.stream().map(DbWorkspaceUserCache::getId).toList());

    log.info(
        String.format("Upserting %d entries into workspace user cache", newCacheEntries.size()));
    workspaceUserCacheDao.upsertAll(newCacheEntries);
  }

  private Stream<DbWorkspaceUserCache> getWorkspaceUserCacheEntries(
      long workspaceId,
      Map<String, RawlsWorkspaceAccessEntry> acl,
      Map<String, DbUser> usernameMap) {
    return acl.entrySet().stream()
        .filter(aclItem -> usernameMap.containsKey(aclItem.getKey()))
        .map(
            aclItem ->
                new DbWorkspaceUserCache()
                    .setWorkspaceId(workspaceId)
                    .setUserId(usernameMap.get(aclItem.getKey()).getUserId())
                    .setRole(aclItem.getValue().getAccessLevel())
                    .setLastUpdated(Timestamp.from(Instant.now())));
  }

  @Override
  @Transactional
  public void removeInactiveWorkspaces() {
    workspaceUserCacheDao.deleteAllInactiveWorkspaces();
  }

  @Override
  public Set<String> getWorkspaceUsers(Long workspaceId) {
    return workspaceUserCacheDao.findAllUsersByWorkspaceId(workspaceId);
  }
}
