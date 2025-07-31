package org.pmiops.workbench.workspaces;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.never;
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
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.DbWorkspaceUserCache;
import org.pmiops.workbench.model.WorkspaceActiveStatus;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceAccessEntry;

@ExtendWith(MockitoExtension.class)
public class WorkspaceUserCacheServiceTest {

  @Mock private UserDao mockUserDao;
  @Mock private WorkspaceDao mockWorkspaceDao;
  @Mock private WorkspaceUserCacheDao mockWorkspaceUserCacheDao;

  @Captor private ArgumentCaptor<List<DbWorkspaceUserCache>> cacheEntriesCaptor;
  @Captor private ArgumentCaptor<Set<Long>> workspaceIdsCaptor;

  private WorkspaceUserCacheService workspaceUserCacheService;

  private DbWorkspace testWorkspace1;
  private DbWorkspace testWorkspace2;
  private DbUser testUser1;
  private DbUser testUser2;

  @BeforeEach
  public void setUp() {
    workspaceUserCacheService = new WorkspaceUserCacheServiceImpl(
        mockUserDao, mockWorkspaceDao, mockWorkspaceUserCacheDao);

    // Set up test data
    testWorkspace1 = new DbWorkspace()
        .setWorkspaceId(1L)
        .setName("Test Workspace 1")
        .setWorkspaceNamespace("test-ws-1")
        .setFirecloudName("test-ws-1-fc")
        .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);

    testWorkspace2 = new DbWorkspace()
        .setWorkspaceId(2L)
        .setName("Test Workspace 2")
        .setWorkspaceNamespace("test-ws-2")
        .setFirecloudName("test-ws-2-fc")
        .setWorkspaceActiveStatusEnum(WorkspaceActiveStatus.ACTIVE);

    testUser1 = new DbUser()
        .setUserId(101L)
        .setUsername("user1@example.com");

    testUser2 = new DbUser()
        .setUserId(102L)
        .setUsername("user2@example.com");
  }

  @Test
  public void testFindAllActiveWorkspaceNamespacesNeedingCacheUpdate() {
    List<DbWorkspace> expectedWorkspaces = List.of(testWorkspace1, testWorkspace2);
    when(mockWorkspaceDao.findAllActiveWorkspaceNamespacesNeedingCacheUpdate())
        .thenReturn(expectedWorkspaces);

    List<DbWorkspace> result = workspaceUserCacheService.findAllActiveWorkspaceNamespacesNeedingCacheUpdate();

    assertThat(result).containsExactlyElementsIn(expectedWorkspaces);
    verify(mockWorkspaceDao).findAllActiveWorkspaceNamespacesNeedingCacheUpdate();
  }

  @Test
  public void testUpdateWorkspaceUserCache_singleWorkspace() {
    RawlsWorkspaceAccessEntry ownerEntry = new RawlsWorkspaceAccessEntry().accessLevel("OWNER");
    RawlsWorkspaceAccessEntry readerEntry = new RawlsWorkspaceAccessEntry().accessLevel("READER");

    Map<String, RawlsWorkspaceAccessEntry> acl = Map.of(
        "user1@example.com", ownerEntry,
        "user2@example.com", readerEntry
    );

    Map<Long, Map<String, RawlsWorkspaceAccessEntry>> newEntriesByWorkspaceId = Map.of(
        1L, acl
    );

    when(mockUserDao.findUsersByUsernameIn(Set.of("user1@example.com", "user2@example.com")))
        .thenReturn(List.of(testUser1, testUser2));

    workspaceUserCacheService.updateWorkspaceUserCache(newEntriesByWorkspaceId);

    verify(mockWorkspaceUserCacheDao).deleteAllByWorkspaceIdIn(Set.of(1L));
    verify(mockWorkspaceUserCacheDao).saveAll(cacheEntriesCaptor.capture());

    List<DbWorkspaceUserCache> savedEntries = cacheEntriesCaptor.getValue();
    assertThat(savedEntries).hasSize(2);

    // Verify the cache entries were created correctly
    DbWorkspaceUserCache user1Entry = savedEntries.stream()
        .filter(entry -> entry.getUserId() == 101L)
        .findFirst()
        .orElseThrow();
    assertThat(user1Entry.getWorkspaceId()).isEqualTo(1L);
    assertThat(user1Entry.getRole()).isEqualTo("OWNER");
    assertThat(user1Entry.getLastUpdated()).isNotNull();

    DbWorkspaceUserCache user2Entry = savedEntries.stream()
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

    Map<Long, Map<String, RawlsWorkspaceAccessEntry>> newEntriesByWorkspaceId = Map.of(
        1L, Map.of("user1@example.com", ownerEntry),
        2L, Map.of("user2@example.com", writerEntry)
    );

    when(mockUserDao.findUsersByUsernameIn(Set.of("user1@example.com", "user2@example.com")))
        .thenReturn(List.of(testUser1, testUser2));

    workspaceUserCacheService.updateWorkspaceUserCache(newEntriesByWorkspaceId);

    verify(mockWorkspaceUserCacheDao).deleteAllByWorkspaceIdIn(Set.of(1L, 2L));
    verify(mockWorkspaceUserCacheDao).saveAll(cacheEntriesCaptor.capture());

    List<DbWorkspaceUserCache> savedEntries = cacheEntriesCaptor.getValue();
    assertThat(savedEntries).hasSize(2);

    // Verify workspace 1 entry
    DbWorkspaceUserCache ws1Entry = savedEntries.stream()
        .filter(entry -> entry.getWorkspaceId() == 1L)
        .findFirst()
        .orElseThrow();
    assertThat(ws1Entry.getUserId()).isEqualTo(101L);
    assertThat(ws1Entry.getRole()).isEqualTo("OWNER");

    // Verify workspace 2 entry
    DbWorkspaceUserCache ws2Entry = savedEntries.stream()
        .filter(entry -> entry.getWorkspaceId() == 2L)
        .findFirst()
        .orElseThrow();
    assertThat(ws2Entry.getUserId()).isEqualTo(102L);
    assertThat(ws2Entry.getRole()).isEqualTo("WRITER");
  }

  @Test
  public void testUpdateWorkspaceUserCache_emptyAcl() {
    Map<Long, Map<String, RawlsWorkspaceAccessEntry>> newEntriesByWorkspaceId = Map.of(
        1L, Map.of()
    );

    when(mockUserDao.findUsersByUsernameIn(Set.of()))
        .thenReturn(List.of());

    workspaceUserCacheService.updateWorkspaceUserCache(newEntriesByWorkspaceId);

    verify(mockWorkspaceUserCacheDao).deleteAllByWorkspaceIdIn(Set.of(1L));
    verify(mockWorkspaceUserCacheDao).saveAll(cacheEntriesCaptor.capture());

    List<DbWorkspaceUserCache> savedEntries = cacheEntriesCaptor.getValue();
    assertThat(savedEntries).isEmpty();
  }

  @Test
  public void testUpdateWorkspaceUserCache_noWorkspaces() {
    Map<Long, Map<String, RawlsWorkspaceAccessEntry>> emptyMap = Map.of();

    when(mockUserDao.findUsersByUsernameIn(Set.of()))
        .thenReturn(List.of());

    workspaceUserCacheService.updateWorkspaceUserCache(emptyMap);

    verify(mockWorkspaceUserCacheDao).deleteAllByWorkspaceIdIn(Set.of());
    verify(mockWorkspaceUserCacheDao).saveAll(cacheEntriesCaptor.capture());

    List<DbWorkspaceUserCache> savedEntries = cacheEntriesCaptor.getValue();
    assertThat(savedEntries).isEmpty();
  }

  @Test
  public void testUpdateWorkspaceUserCache_duplicateUsers() {
    RawlsWorkspaceAccessEntry ownerEntry = new RawlsWorkspaceAccessEntry().accessLevel("OWNER");
    RawlsWorkspaceAccessEntry readerEntry = new RawlsWorkspaceAccessEntry().accessLevel("READER");

    Map<Long, Map<String, RawlsWorkspaceAccessEntry>> newEntriesByWorkspaceId = Map.of(
        1L, Map.of("user1@example.com", ownerEntry),
        2L, Map.of("user1@example.com", readerEntry)
    );

    when(mockUserDao.findUsersByUsernameIn(Set.of("user1@example.com")))
        .thenReturn(List.of(testUser1));

    workspaceUserCacheService.updateWorkspaceUserCache(newEntriesByWorkspaceId);

    verify(mockWorkspaceUserCacheDao).deleteAllByWorkspaceIdIn(Set.of(1L, 2L));
    verify(mockWorkspaceUserCacheDao).saveAll(cacheEntriesCaptor.capture());

    List<DbWorkspaceUserCache> savedEntries = cacheEntriesCaptor.getValue();
    assertThat(savedEntries).hasSize(2);

    // Verify both entries are for the same user but different workspaces and roles
    assertThat(savedEntries.stream().allMatch(entry -> entry.getUserId() == 101L)).isTrue();
    assertThat(savedEntries.stream().map(DbWorkspaceUserCache::getWorkspaceId).toList())
        .containsExactly(1L, 2L);
    assertThat(savedEntries.stream().map(DbWorkspaceUserCache::getRole).toList())
        .containsExactly("OWNER", "READER");
  }

  @Test
  public void testUpdateWorkspaceUserCache_timestampsAreRecent() {
    Instant beforeCall = Instant.now();

    RawlsWorkspaceAccessEntry ownerEntry = new RawlsWorkspaceAccessEntry().accessLevel("OWNER");
    Map<Long, Map<String, RawlsWorkspaceAccessEntry>> newEntriesByWorkspaceId = Map.of(
        1L, Map.of("user1@example.com", ownerEntry)
    );

    when(mockUserDao.findUsersByUsernameIn(Set.of("user1@example.com")))
        .thenReturn(List.of(testUser1));

    workspaceUserCacheService.updateWorkspaceUserCache(newEntriesByWorkspaceId);

    Instant afterCall = Instant.now();

    verify(mockWorkspaceUserCacheDao).saveAll(cacheEntriesCaptor.capture());

    List<DbWorkspaceUserCache> savedEntries = cacheEntriesCaptor.getValue();
    assertThat(savedEntries).hasSize(1);

    Timestamp lastUpdated = savedEntries.get(0).getLastUpdated();
    assertThat(lastUpdated.toInstant()).isAtLeast(beforeCall);
    assertThat(lastUpdated.toInstant()).isAtMost(afterCall);
  }

  @Test
  public void testUpdateWorkspaceUserCache_userNotFound() {
    RawlsWorkspaceAccessEntry ownerEntry = new RawlsWorkspaceAccessEntry().accessLevel("OWNER");
    Map<Long, Map<String, RawlsWorkspaceAccessEntry>> newEntriesByWorkspaceId = Map.of(
        1L, Map.of("nonexistent@example.com", ownerEntry)
    );

    when(mockUserDao.findUsersByUsernameIn(Set.of("nonexistent@example.com")))
        .thenReturn(List.of()); // User not found

    // Method should complete without throwing exception
    workspaceUserCacheService.updateWorkspaceUserCache(newEntriesByWorkspaceId);

    // Verify workspace cache entries are deleted
    verify(mockWorkspaceUserCacheDao).deleteAllByWorkspaceIdIn(Set.of(1L));

    // Verify no new entries are saved since user doesn't exist
    verify(mockWorkspaceUserCacheDao).saveAll(cacheEntriesCaptor.capture());
    List<DbWorkspaceUserCache> savedEntries = cacheEntriesCaptor.getValue();
    assertThat(savedEntries).isEmpty();
  }

  @Test
  public void testRemoveInactiveWorkspaces() {
    workspaceUserCacheService.removeInactiveWorkspaces();

    verify(mockWorkspaceUserCacheDao).deleteAllInactiveWorkspaces();
  }
}
