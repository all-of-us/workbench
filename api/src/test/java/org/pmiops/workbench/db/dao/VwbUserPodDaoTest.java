package org.pmiops.workbench.db.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbVwbUserPod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Import(FakeClockConfiguration.class)
@Transactional
public class VwbUserPodDaoTest {

  @Autowired private VwbUserPodDao vwbUserPodDao;

  @Autowired private UserDao userDao;

  private DbUser user1;
  private DbUser user2;

  @BeforeEach
  public void setUp() {
    // Create test users
    user1 = new DbUser();
    user1.setUsername("user1@test.com");
    user1.setContactEmail("user1@test.com");
    user1.setDisabled(false);
    user1 = userDao.save(user1);

    user2 = new DbUser();
    user2.setUsername("user2@test.com");
    user2.setContactEmail("user2@test.com");
    user2.setDisabled(false);
    user2 = userDao.save(user2);
  }

  @Test
  public void testFindByUserUserId_found() {
    // Create a VWB pod for user1
    DbVwbUserPod pod = new DbVwbUserPod();
    pod.setUser(user1);
    pod.setVwbPodId("pod-123");
    pod.setInitialCreditsActive(true);
    vwbUserPodDao.save(pod);

    // Find by user ID
    DbVwbUserPod foundPod = vwbUserPodDao.findByUserUserId(user1.getUserId());

    assertNotNull(foundPod);
    assertEquals("pod-123", foundPod.getVwbPodId());
    assertEquals(user1.getUserId(), foundPod.getUser().getUserId());
    assertEquals(true, foundPod.isInitialCreditsActive());
  }

  @Test
  public void testFindByUserUserId_notFound() {
    // Try to find pod for user with no pod
    DbVwbUserPod foundPod = vwbUserPodDao.findByUserUserId(user2.getUserId());

    assertNull(foundPod);
  }

  @Test
  public void testFindByUserUserId_withNullPodId() {
    // Create a VWB pod lock (null pod_id) for user1
    DbVwbUserPod pod = new DbVwbUserPod();
    pod.setUser(user1);
    pod.setVwbPodId(null); // Lock row
    pod.setInitialCreditsActive(false);
    vwbUserPodDao.save(pod);

    // Should be able to find it even with null pod_id
    DbVwbUserPod foundPod = vwbUserPodDao.findByUserUserId(user1.getUserId());

    assertNotNull(foundPod);
    assertNull(foundPod.getVwbPodId());
    assertEquals(user1.getUserId(), foundPod.getUser().getUserId());
    assertEquals(false, foundPod.isInitialCreditsActive());
  }

  @Test
  public void testFindByUserUserId_multipleUsers() {
    // Create pods for both users
    DbVwbUserPod pod1 = new DbVwbUserPod();
    pod1.setUser(user1);
    pod1.setVwbPodId("pod-123");
    pod1.setInitialCreditsActive(true);
    vwbUserPodDao.save(pod1);

    DbVwbUserPod pod2 = new DbVwbUserPod();
    pod2.setUser(user2);
    pod2.setVwbPodId("pod-456");
    pod2.setInitialCreditsActive(true);
    vwbUserPodDao.save(pod2);

    // Find each user's pod
    DbVwbUserPod foundPod1 = vwbUserPodDao.findByUserUserId(user1.getUserId());
    DbVwbUserPod foundPod2 = vwbUserPodDao.findByUserUserId(user2.getUserId());

    assertNotNull(foundPod1);
    assertEquals("pod-123", foundPod1.getVwbPodId());

    assertNotNull(foundPod2);
    assertEquals("pod-456", foundPod2.getVwbPodId());
  }
}
