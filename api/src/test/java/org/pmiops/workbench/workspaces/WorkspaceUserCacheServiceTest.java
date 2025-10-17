package org.pmiops.workbench.workspaces;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceUserCacheDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspaceUserCache;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceAccessEntry;

@ExtendWith(MockitoExtension.class)
public class WorkspaceUserCacheServiceTest {

  @Mock private UserDao mockUserDao;
  @Mock private WorkspaceDao mockWorkspaceDao;
  @Mock private WorkspaceUserCacheDao mockWorkspaceUserCacheDao;

  @Captor private ArgumentCaptor<Iterable<DbWorkspaceUserCache>> cacheEntriesIterableCaptor;

  private WorkspaceUserCacheService workspaceUserCacheService;

  @Mock private WorkspaceDao.WorkspaceUserCacheView testWorkspace1;
  @Mock private WorkspaceDao.WorkspaceUserCacheView testWorkspace2;
  private DbUser testUser1;
  private DbUser testUser2;

  @BeforeEach
  public void setUp() {
    workspaceUserCacheService =
        new WorkspaceUserCacheServiceImpl(mockUserDao, mockWorkspaceDao, mockWorkspaceUserCacheDao);

    testUser1 = new DbUser().setUserId(101L).setUsername("user1@example.com");

    testUser2 = new DbUser().setUserId(102L).setUsername("user2@example.com");
  }

  @Test
  public void testFindAllActiveWorkspacesNeedingCacheUpdate() {
    List<WorkspaceDao.WorkspaceUserCacheView> expectedWorkspaces =
        List.of(testWorkspace1, testWorkspace2);
    when(mockWorkspaceDao.findAllActiveWorkspaceNamespacesNeedingCacheUpdate())
        .thenReturn(expectedWorkspaces);

    List<WorkspaceDao.WorkspaceUserCacheView> result =
        workspaceUserCacheService.findAllActiveWorkspacesNeedingCacheUpdate();

    assertThat(result).containsExactlyElementsIn(expectedWorkspaces);
    verify(mockWorkspaceDao).findAllActiveWorkspaceNamespacesNeedingCacheUpdate();
  }

  @Test
  public void testUpdateWorkspaceUserCache_singleWorkspace() {
    RawlsWorkspaceAccessEntry ownerEntry = new RawlsWorkspaceAccessEntry().accessLevel("OWNER");
    RawlsWorkspaceAccessEntry readerEntry = new RawlsWorkspaceAccessEntry().accessLevel("READER");

    Map<String, RawlsWorkspaceAccessEntry> acl =
        Map.of(
            "user1@example.com", ownerEntry,
            "user2@example.com", readerEntry);

    Map<Long, Map<String, RawlsWorkspaceAccessEntry>> newEntriesByWorkspaceId = Map.of(1L, acl);

    when(mockUserDao.getUsersMappedByUsernames(Set.of("user1@example.com", "user2@example.com")))
        .thenReturn(Map.of(testUser1.getUsername(), testUser1, testUser2.getUsername(), testUser2));
    when(mockWorkspaceUserCacheDao.findAllByWorkspaceIdIn(Set.of(1L))).thenReturn(List.of());

    workspaceUserCacheService.updateWorkspaceUserCache(newEntriesByWorkspaceId);

    // With no existing entries, nothing should be deleted
    verify(mockWorkspaceUserCacheDao).deleteAllById(List.of());
    verify(mockWorkspaceUserCacheDao).upsertAll(cacheEntriesIterableCaptor.capture());

    List<DbWorkspaceUserCache> upsertedEntries =
        (List<DbWorkspaceUserCache>) cacheEntriesIterableCaptor.getValue();
    assertThat(upsertedEntries).hasSize(2);

    // Verify the cache entries were created correctly
    DbWorkspaceUserCache user1Entry =
        upsertedEntries.stream()
            .filter(entry -> entry.getUserId() == 101L)
            .findFirst()
            .orElseThrow();
    assertThat(user1Entry.getWorkspaceId()).isEqualTo(1L);
    assertThat(user1Entry.getRole()).isEqualTo("OWNER");
    assertThat(user1Entry.getLastUpdated()).isNotNull();

    DbWorkspaceUserCache user2Entry =
        upsertedEntries.stream()
            .filter(entry -> entry.getUserId() == 102L)
            .findFirst()
            .orElseThrow();
    assertThat(user2Entry.getWorkspaceId()).isEqualTo(1L);
    assertThat(user2Entry.getRole()).isEqualTo("READER");
    assertThat(user2Entry.getLastUpdated()).isNotNull();
  }

  @Test
  public void testUpdateWorkspaceUserCache_multipleWorkspaces() {
    RawlsWorkspaceAccessEntry ownerEntry = new RawlsWorkspaceAccessEntry().accessLevel("OWNER");
    RawlsWorkspaceAccessEntry writerEntry = new RawlsWorkspaceAccessEntry().accessLevel("WRITER");

    Map<Long, Map<String, RawlsWorkspaceAccessEntry>> newEntriesByWorkspaceId =
        Map.of(
            1L, Map.of("user1@example.com", ownerEntry),
            2L, Map.of("user2@example.com", writerEntry));

    when(mockUserDao.getUsersMappedByUsernames(Set.of("user1@example.com", "user2@example.com")))
        .thenReturn(Map.of(testUser1.getUsername(), testUser1, testUser2.getUsername(), testUser2));
    when(mockWorkspaceUserCacheDao.findAllByWorkspaceIdIn(Set.of(1L, 2L))).thenReturn(List.of());

    workspaceUserCacheService.updateWorkspaceUserCache(newEntriesByWorkspaceId);

    verify(mockWorkspaceUserCacheDao).deleteAllById(List.of());
    verify(mockWorkspaceUserCacheDao).upsertAll(cacheEntriesIterableCaptor.capture());

    List<DbWorkspaceUserCache> upsertedEntries =
        (List<DbWorkspaceUserCache>) cacheEntriesIterableCaptor.getValue();
    assertThat(upsertedEntries).hasSize(2);

    // Verify workspace 1 entry
    DbWorkspaceUserCache ws1Entry =
        upsertedEntries.stream()
            .filter(entry -> entry.getWorkspaceId() == 1L)
            .findFirst()
            .orElseThrow();
    assertThat(ws1Entry.getUserId()).isEqualTo(101L);
    assertThat(ws1Entry.getRole()).isEqualTo("OWNER");

    // Verify workspace 2 entry
    DbWorkspaceUserCache ws2Entry =
        upsertedEntries.stream()
            .filter(entry -> entry.getWorkspaceId() == 2L)
            .findFirst()
            .orElseThrow();
    assertThat(ws2Entry.getUserId()).isEqualTo(102L);
    assertThat(ws2Entry.getRole()).isEqualTo("WRITER");
  }

  @Test
  public void testUpdateWorkspaceUserCache_emptyAcl() {
    Map<Long, Map<String, RawlsWorkspaceAccessEntry>> newEntriesByWorkspaceId =
        Map.of(1L, Map.of());

    when(mockUserDao.getUsersMappedByUsernames(Set.of())).thenReturn(Map.of());

    // Mock existing entry that should be deleted
    DbWorkspaceUserCache existingEntry =
        new DbWorkspaceUserCache()
            .setId(1001L)
            .setWorkspaceId(1L)
            .setUserId(101L)
            .setRole("OWNER")
            .setLastUpdated(Timestamp.from(Instant.now()));
    when(mockWorkspaceUserCacheDao.findAllByWorkspaceIdIn(Set.of(1L)))
        .thenReturn(List.of(existingEntry));

    workspaceUserCacheService.updateWorkspaceUserCache(newEntriesByWorkspaceId);

    // Should delete the existing entry since the new ACL is empty
    verify(mockWorkspaceUserCacheDao).deleteAllById(List.of(1001L));
    verify(mockWorkspaceUserCacheDao).upsertAll(cacheEntriesIterableCaptor.capture());

    List<DbWorkspaceUserCache> upsertedEntries =
        (List<DbWorkspaceUserCache>) cacheEntriesIterableCaptor.getValue();
    assertThat(upsertedEntries).isEmpty();
  }

  @Test
  public void testUpdateWorkspaceUserCache_noWorkspaces() {
    Map<Long, Map<String, RawlsWorkspaceAccessEntry>> emptyMap = Map.of();

    when(mockUserDao.getUsersMappedByUsernames(Set.of())).thenReturn(Map.of());
    when(mockWorkspaceUserCacheDao.findAllByWorkspaceIdIn(Set.of())).thenReturn(List.of());

    workspaceUserCacheService.updateWorkspaceUserCache(emptyMap);

    verify(mockWorkspaceUserCacheDao).deleteAllById(List.of());
    verify(mockWorkspaceUserCacheDao).upsertAll(cacheEntriesIterableCaptor.capture());

    List<DbWorkspaceUserCache> upsertedEntries =
        (List<DbWorkspaceUserCache>) cacheEntriesIterableCaptor.getValue();
    assertThat(upsertedEntries).isEmpty();
  }

  @Test
  public void testUpdateWorkspaceUserCache_duplicateUsers() {
    RawlsWorkspaceAccessEntry ownerEntry = new RawlsWorkspaceAccessEntry().accessLevel("OWNER");
    RawlsWorkspaceAccessEntry readerEntry = new RawlsWorkspaceAccessEntry().accessLevel("READER");

    Map<Long, Map<String, RawlsWorkspaceAccessEntry>> newEntriesByWorkspaceId =
        Map.of(
            1L, Map.of("user1@example.com", ownerEntry),
            2L, Map.of("user1@example.com", readerEntry));

    when(mockUserDao.getUsersMappedByUsernames(Set.of("user1@example.com")))
        .thenReturn(Map.of(testUser1.getUsername(), testUser1));
    when(mockWorkspaceUserCacheDao.findAllByWorkspaceIdIn(Set.of(1L, 2L))).thenReturn(List.of());

    workspaceUserCacheService.updateWorkspaceUserCache(newEntriesByWorkspaceId);

    verify(mockWorkspaceUserCacheDao).deleteAllById(List.of());
    verify(mockWorkspaceUserCacheDao).upsertAll(cacheEntriesIterableCaptor.capture());

    List<DbWorkspaceUserCache> upsertedEntries =
        (List<DbWorkspaceUserCache>) cacheEntriesIterableCaptor.getValue();
    assertThat(upsertedEntries).hasSize(2);

    // Verify both entries are for the same user but different workspaces and roles
    assertThat(upsertedEntries.stream().allMatch(entry -> entry.getUserId() == 101L)).isTrue();
    assertThat(upsertedEntries.stream().map(DbWorkspaceUserCache::getWorkspaceId).toList())
        .containsExactly(1L, 2L);
    assertThat(upsertedEntries.stream().map(DbWorkspaceUserCache::getRole).toList())
        .containsExactly("OWNER", "READER");
  }

  @Test
  public void testUpdateWorkspaceUserCache_timestampsAreRecent() {
    Instant beforeCall = Instant.now();

    RawlsWorkspaceAccessEntry ownerEntry = new RawlsWorkspaceAccessEntry().accessLevel("OWNER");
    Map<Long, Map<String, RawlsWorkspaceAccessEntry>> newEntriesByWorkspaceId =
        Map.of(1L, Map.of("user1@example.com", ownerEntry));

    when(mockUserDao.getUsersMappedByUsernames(Set.of("user1@example.com")))
        .thenReturn(Map.of(testUser1.getUsername(), testUser1));
    when(mockWorkspaceUserCacheDao.findAllByWorkspaceIdIn(Set.of(1L))).thenReturn(List.of());

    workspaceUserCacheService.updateWorkspaceUserCache(newEntriesByWorkspaceId);

    Instant afterCall = Instant.now();

    verify(mockWorkspaceUserCacheDao).upsertAll(cacheEntriesIterableCaptor.capture());

    List<DbWorkspaceUserCache> upsertedEntries =
        (List<DbWorkspaceUserCache>) cacheEntriesIterableCaptor.getValue();
    assertThat(upsertedEntries).hasSize(1);

    Timestamp lastUpdated = upsertedEntries.get(0).getLastUpdated();
    assertThat(lastUpdated.toInstant()).isAtLeast(beforeCall);
    assertThat(lastUpdated.toInstant()).isAtMost(afterCall);
  }

  @Test
  public void testUpdateWorkspaceUserCache_userNotFound() {
    RawlsWorkspaceAccessEntry ownerEntry = new RawlsWorkspaceAccessEntry().accessLevel("OWNER");
    Map<Long, Map<String, RawlsWorkspaceAccessEntry>> newEntriesByWorkspaceId =
        Map.of(1L, Map.of("nonexistent@example.com", ownerEntry));

    when(mockUserDao.getUsersMappedByUsernames(Set.of("nonexistent@example.com")))
        .thenReturn(Map.of()); // User not found
    when(mockWorkspaceUserCacheDao.findAllByWorkspaceIdIn(Set.of(1L))).thenReturn(List.of());

    // Method should complete without throwing exception
    workspaceUserCacheService.updateWorkspaceUserCache(newEntriesByWorkspaceId);

    // Verify workspace cache entries deletion is called (even if empty set)
    verify(mockWorkspaceUserCacheDao).deleteAllById(List.of());

    // Verify no new entries are saved since user doesn't exist
    verify(mockWorkspaceUserCacheDao).upsertAll(cacheEntriesIterableCaptor.capture());
    List<DbWorkspaceUserCache> upsertedEntries =
        (List<DbWorkspaceUserCache>) cacheEntriesIterableCaptor.getValue();
    assertThat(upsertedEntries).isEmpty();
  }

  @Test
  public void testUpdateWorkspaceUserCache_removesStaleEntries() {
    // Test that entries that exist in the cache but not in the new ACL are deleted
    RawlsWorkspaceAccessEntry ownerEntry = new RawlsWorkspaceAccessEntry().accessLevel("OWNER");
    Map<Long, Map<String, RawlsWorkspaceAccessEntry>> newEntriesByWorkspaceId =
        Map.of(1L, Map.of("user1@example.com", ownerEntry));

    when(mockUserDao.getUsersMappedByUsernames(Set.of("user1@example.com")))
        .thenReturn(Map.of(testUser1.getUsername(), testUser1));

    // Mock existing entries - user2 should be removed as they're not in the new ACL
    DbWorkspaceUserCache existingUser1Entry =
        new DbWorkspaceUserCache()
            .setId(1001L)
            .setWorkspaceId(1L)
            .setUserId(101L)
            .setRole("READER")
            .setLastUpdated(Timestamp.from(Instant.now()));
    DbWorkspaceUserCache existingUser2Entry =
        new DbWorkspaceUserCache()
            .setId(1002L)
            .setWorkspaceId(1L)
            .setUserId(102L)
            .setRole("OWNER")
            .setLastUpdated(Timestamp.from(Instant.now()));
    when(mockWorkspaceUserCacheDao.findAllByWorkspaceIdIn(Set.of(1L)))
        .thenReturn(List.of(existingUser1Entry, existingUser2Entry));

    workspaceUserCacheService.updateWorkspaceUserCache(newEntriesByWorkspaceId);

    // Should delete the stale entry for user2
    verify(mockWorkspaceUserCacheDao).deleteAllById(List.of(1002L));

    verify(mockWorkspaceUserCacheDao).upsertAll(cacheEntriesIterableCaptor.capture());
    List<DbWorkspaceUserCache> upsertedEntries =
        (List<DbWorkspaceUserCache>) cacheEntriesIterableCaptor.getValue();
    assertThat(upsertedEntries).hasSize(1);
    assertThat(upsertedEntries.get(0).getUserId()).isEqualTo(101L);
    assertThat(upsertedEntries.get(0).getRole()).isEqualTo("OWNER");
  }

  @Test
  public void testRemoveInactiveWorkspaces() {
    workspaceUserCacheService.removeInactiveWorkspaces();

    verify(mockWorkspaceUserCacheDao).deleteAllInactiveWorkspaces();
  }
}
