package org.pmiops.workbench.db.dao;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.DbWorkspaceUserCache;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

@DataJpaTest
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@Import(FakeClockConfiguration.class)
public class WorkspaceUserCacheDaoTest {

  @Autowired WorkspaceUserCacheDao workspaceUserCacheDao;
  @Autowired WorkspaceDao workspaceDao;
  @Autowired UserDao userDao;

  private DbWorkspace activeWorkspace;
  private DbWorkspace inactiveWorkspace;
  private DbUser user1;
  private DbWorkspaceUserCache cache1;
  private DbWorkspaceUserCache cache2;
  private DbWorkspaceUserCache cache3;

  @BeforeEach
  public void setUp() {
    Timestamp now = Timestamp.from(Instant.now());

    user1 = userDao.save(new DbUser().setUsername("user1@example.com"));
    DbUser user2 = userDao.save(new DbUser().setUsername("user2@example.com"));

    activeWorkspace =
        new DbWorkspace()
            .setName("Active Workspace")
            .setWorkspaceNamespace("active-ws")
            .setFirecloudName("active-ws")
            .setFirecloudUuid("active-uuid")
            .setCreationTime(now)
            .setLastModifiedTime(now)
            .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);
    activeWorkspace = workspaceDao.save(activeWorkspace);

    inactiveWorkspace =
        new DbWorkspace()
            .setName("Inactive Workspace")
            .setWorkspaceNamespace("inactive-ws")
            .setFirecloudName("inactive-ws")
            .setFirecloudUuid("inactive-uuid")
            .setCreationTime(now)
            .setLastModifiedTime(now)
            .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.DELETED);
    inactiveWorkspace = workspaceDao.save(inactiveWorkspace);

    cache1 =
        new DbWorkspaceUserCache(activeWorkspace.getWorkspaceId(), user1.getUserId(), "OWNER", now);
    cache1 = workspaceUserCacheDao.save(cache1);

    cache2 =
        new DbWorkspaceUserCache(
            activeWorkspace.getWorkspaceId(), user2.getUserId(), "READER", now);
    cache2 = workspaceUserCacheDao.save(cache2);

    cache3 =
        new DbWorkspaceUserCache(
            inactiveWorkspace.getWorkspaceId(), user1.getUserId(), "WRITER", now);
    cache3 = workspaceUserCacheDao.save(cache3);
  }

  @Test
  public void testDeleteAllByWorkspaceIdsIn_singleWorkspace() {
    Set<Long> workspaceIds = Set.of(activeWorkspace.getWorkspaceId());
    workspaceUserCacheDao.deleteAllByWorkspaceIdIn(workspaceIds);

    List<DbWorkspaceUserCache> remaining =
        (List<DbWorkspaceUserCache>) workspaceUserCacheDao.findAll();
    assertThat(remaining).hasSize(1);
    assertThat(remaining).containsExactly(cache3);
  }

  @Test
  public void testDeleteAllByWorkspaceIdIn_multipleWorkspaces() {
    Set<Long> workspaceIds =
        Set.of(activeWorkspace.getWorkspaceId(), inactiveWorkspace.getWorkspaceId());
    workspaceUserCacheDao.deleteAllByWorkspaceIdIn(workspaceIds);

    List<DbWorkspaceUserCache> remaining =
        (List<DbWorkspaceUserCache>) workspaceUserCacheDao.findAll();
    assertThat(remaining).isEmpty();
  }

  @Test
  public void testDeleteAllByWorkspaceIdsIn_nonExistentWorkspace() {
    Set<Long> workspaceIds = Set.of(99999L);
    workspaceUserCacheDao.deleteAllByWorkspaceIdIn(workspaceIds);

    // All cache entries should still exist
    List<DbWorkspaceUserCache> remaining =
        (List<DbWorkspaceUserCache>) workspaceUserCacheDao.findAll();
    assertThat(remaining).hasSize(3);
    assertThat(remaining).containsExactly(cache1, cache2, cache3);
  }

  @Test
  public void testDeleteAllByWorkspaceIdIn_noWorkspaces() {
    Set<Long> workspaceIds = Set.of();
    workspaceUserCacheDao.deleteAllByWorkspaceIdIn(workspaceIds);

    // All cache entries should still exist
    List<DbWorkspaceUserCache> remaining =
        (List<DbWorkspaceUserCache>) workspaceUserCacheDao.findAll();
    assertThat(remaining).hasSize(3);
    assertThat(remaining).containsExactly(cache1, cache2, cache3);
  }

  @Test
  public void testDeleteAllInactiveWorkspaces() {
    workspaceUserCacheDao.deleteAllInactiveWorkspaces();

    List<DbWorkspaceUserCache> remaining =
        (List<DbWorkspaceUserCache>) workspaceUserCacheDao.findAll();
    // Should only have cache entries for active workspaces
    assertThat(remaining).hasSize(2);
    assertThat(remaining).containsExactly(cache1, cache2);
  }

  @Test
  public void testDeleteAllInactiveWorkspaces_noInactiveWorkspaces() {
    // Update inactive workspace to be active
    inactiveWorkspace.setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);
    workspaceDao.save(inactiveWorkspace);

    workspaceUserCacheDao.deleteAllInactiveWorkspaces();

    // All cache entries should still exist since all workspaces are active
    List<DbWorkspaceUserCache> remaining =
        (List<DbWorkspaceUserCache>) workspaceUserCacheDao.findAll();
    assertThat(remaining).hasSize(3);
    assertThat(remaining).containsExactly(cache1, cache2, cache3);
  }

  @Test
  public void testUniqueConstraint() {
    Timestamp now = Timestamp.from(Instant.now());

    DbWorkspaceUserCache duplicateCache =
        new DbWorkspaceUserCache(
            activeWorkspace.getWorkspaceId(), user1.getUserId(), "WRITER", now);

    assertThrows(
        org.springframework.dao.DataIntegrityViolationException.class,
        () -> workspaceUserCacheDao.save(duplicateCache));
  }
}
