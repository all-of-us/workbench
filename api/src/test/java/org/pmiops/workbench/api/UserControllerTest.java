package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.compliance.ComplianceService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.AdminActionHistoryDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.CommonStorageEnums;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.UserResponse;
import org.pmiops.workbench.moodle.ApiException;
import org.pmiops.workbench.moodle.model.BadgeDetails;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.test.Providers;
import org.pmiops.workbench.utils.PaginationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
public class UserControllerTest {

  @Autowired
  private UserDao userDao;
  @Mock
  private AdminActionHistoryDao adminActionHistoryDao;
  @Mock
  private Provider<WorkbenchConfig> configProvider;
  @Mock
  private Provider<User> userProvider;
  @Mock
  private FireCloudService fireCloudService;
  @Mock
  private ComplianceService complianceService;
  @Mock
  private DirectoryService directoryService;
  private UserService userService;

  private UserController userController;
  private Long incrementedUserId = 1L;
  private static final FakeClock CLOCK = new FakeClock(Instant.now(), ZoneId.systemDefault());
  public Clock clock() {
    return CLOCK;
  }


  @Before
  public void setUp() {
    WorkbenchConfig config =  new WorkbenchConfig();
    config.firecloud = new WorkbenchConfig.FireCloudConfig();
    config.firecloud.enforceRegistered = false;
    configProvider = Providers.of(config);
    this.userService = new UserService(userProvider, userDao, adminActionHistoryDao, clock(),
        new Random(), fireCloudService, configProvider, complianceService, directoryService);
    this.userController = new UserController(userService);
    saveFamily();
  }

  @After
  public void tearDown() {
    userDao.deleteAll();
  }

  @Test
  public void testEnforceRegistered() {
    configProvider.get().firecloud.enforceRegistered = true;
    this.userService = new UserService(userProvider, userDao, adminActionHistoryDao, clock(),
        new Random(), fireCloudService, configProvider, complianceService);
    this.userController = new UserController(userService);
    User john = userDao.findUserByEmail("john@lis.org");

    UserResponse response = userController.user("Robinson", null, null, null).getBody();
    // Test data contains a single registered user, John Robinson.
    assertThat(response.getUsers()).hasSize(1);
    assertThat(response.getUsers().get(0).getEmail()).isSameAs(john.getEmail());
  }

  @Test
  public void testUserSearch() {
    User john = userDao.findUserByEmail("john@lis.org");

    UserResponse response = userController.user("John", null, null, null).getBody();
    assertThat(response.getUsers()).hasSize(1);
    assertThat(response.getUsers().get(0).getEmail()).isSameAs(john.getEmail());
  }

  @Test
  public void testUserPartialStringSearch() {
    List<User> allUsers = Lists.newArrayList(userDao.findAll());

    UserResponse response = userController.user("obin", null, null, null).getBody();

    // We only want to include users that have active billing projects to avoid users not initialized in FC.
    assertThat(response.getUsers()).hasSize(
        allUsers
            .stream()
            .filter(user -> user.getFreeTierBillingProjectName() != null)
            .collect(Collectors.toList())
            .size());
  }

  @Test
  public void testUserEmptyResponse() {
    UserResponse response = userController.user("", null, null, null).getBody();
    assertThat(response.getUsers()).hasSize(0);
  }

  @Test
  public void testUserNoUsersResponse() {
    UserResponse response = userController.user("Smith", null, null, null).getBody();
    assertThat(response.getUsers()).hasSize(0);
  }

  @Test
  public void testInvalidPageTokenCharacters() {
    ResponseEntity<UserResponse> response = userController.user("Robinson", "Inv@l!dT0k3n#", null, null);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().getUsers()).hasSize(0);
  }

  @Test
  public void testInvalidPageToken() {
    ResponseEntity<UserResponse> response = userController.user("Robinson", "eyJvZmZzZXQBhcmFtZF9", null, null);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().getUsers()).hasSize(0);
  }

  @Test
  public void testNegativePageOffset() {
    ResponseEntity<UserResponse> response = userController.user("Robinson", PaginationToken.of(-1).toBase64(), null, null);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody().getUsers()).hasSize(0);
  }

  @Test
  public void testUserPageSize() {
    int size = 1;
    UserResponse robinsons_0 = userController.user("Robinson", PaginationToken.of(0).toBase64(), size, null).getBody();
    UserResponse robinsons_1 = userController.user("Robinson", PaginationToken.of(1).toBase64(), size, null).getBody();
    UserResponse robinsons_2 = userController.user("Robinson", PaginationToken.of(2).toBase64(), size, null).getBody();
    UserResponse robinsons_3 = userController.user("Robinson", PaginationToken.of(3).toBase64(), size, null).getBody();
    UserResponse robinsons_4 = userController.user("Robinson", PaginationToken.of(4).toBase64(), size, null).getBody();

    assertThat(robinsons_0.getUsers()).hasSize(size);
    assertThat(robinsons_0.getNextPageToken()).isEqualTo(PaginationToken.of(1).toBase64());
    assertThat(robinsons_1.getUsers()).hasSize(size);
    assertThat(robinsons_1.getNextPageToken()).isEqualTo(PaginationToken.of(2).toBase64());
    assertThat(robinsons_2.getUsers()).hasSize(size);
    assertThat(robinsons_2.getNextPageToken()).isEqualTo(PaginationToken.of(3).toBase64());
    assertThat(robinsons_3.getUsers()).hasSize(size);
    assertThat(robinsons_3.getNextPageToken()).isEqualTo(PaginationToken.of(4).toBase64());
    assertThat(robinsons_4.getUsers()).hasSize(size);
    assertThat(robinsons_4.getNextPageToken()).isEqualTo("");
  }

  @Test
  public void testUserPagedResponses() {
    UserResponse robinsons_0_1 = userController.user("Robinson", PaginationToken.of(0).toBase64(), 2, null).getBody();
    UserResponse robinsons_2_3 = userController.user("Robinson", PaginationToken.of(1).toBase64(), 2, null).getBody();
    UserResponse robinsons_4 = userController.user("Robinson", PaginationToken.of(3).toBase64(), 1, null).getBody();

    // Assert the expected size for each page
    assertThat(robinsons_0_1.getUsers()).hasSize(2);
    assertThat(robinsons_2_3.getUsers()).hasSize(2);
    assertThat(robinsons_4.getUsers()).hasSize(1);

    // Assert uniqueness across pages
    assertThat(robinsons_0_1.getUsers()).containsNoneOf(robinsons_2_3, robinsons_4);
    assertThat(robinsons_2_3.getUsers()).containsNoneOf(robinsons_0_1, robinsons_4);
    assertThat(robinsons_4.getUsers()).containsNoneOf(robinsons_0_1, robinsons_2_3);
  }

  @Test
  public void testUserSort() {
    UserResponse robinsonsAsc = userController.user("Robinson", null, null, "asc").getBody();
    UserResponse robinsonsDesc = userController.user("Robinson", null, null, "desc").getBody();

    // Assert we have the same elements in both responses
    assertThat(robinsonsAsc.getUsers()).containsAllIn(robinsonsDesc.getUsers());

    // Now reverse one and assert both in the same order
    List<org.pmiops.workbench.model.User> descendingReversed = Lists.reverse(robinsonsDesc.getUsers());
    assertThat(robinsonsAsc.getUsers()).containsAllIn(descendingReversed).inOrder();

    // Test that JPA sorting is really what we expected it to be by re-sorting one into a new list
    List<org.pmiops.workbench.model.User> newAscending = Lists.newArrayList(robinsonsAsc.getUsers());
    newAscending.sort(Comparator.comparing(org.pmiops.workbench.model.User::getEmail));
    assertThat(robinsonsAsc.getUsers()).containsAllIn(newAscending).inOrder();
  }

  /*
   * Testing helpers
   */

  private void saveFamily() {
    saveUser("john@lis.org", "John", "Robinson", true);
    saveUser("judy@lis.org", "Judy", "Robinson", false);
    saveUser("maureen@lis.org", "Mauren", "Robinson", false);
    saveUser("penny@lis.org", "Penny", "Robinson", false);
    saveUser("will@lis.org", "Will", "Robinson", false);
    saveUserNotInFirecloud("bob@lis.org", "Bob", "Robinson", false);
  }

  @SuppressWarnings("SameParameterValue")
  private void saveUser(String email, String givenName, String familyName, boolean registered) {
    User user = new User();
    user.setEmail(email);
    user.setUserId(incrementedUserId);
    user.setGivenName(givenName);
    user.setFamilyName(familyName);
    user.setFreeTierBillingProjectName(givenName + familyName);
    if (registered) {
      user.setDataAccessLevel(CommonStorageEnums.dataAccessLevelToStorage(DataAccessLevel.REGISTERED));
    } else {
      user.setDataAccessLevel(CommonStorageEnums.dataAccessLevelToStorage(DataAccessLevel.UNREGISTERED));
    }
    incrementedUserId++;
    userDao.save(user);
  }

  private void saveUserNotInFirecloud(String email, String givenName, String familyName, boolean registered) {
    User user = new User();
    user.setEmail(email);
    user.setUserId(incrementedUserId);
    user.setGivenName(givenName);
    user.setFamilyName(familyName);
    if (registered) {
      user.setDataAccessLevel(CommonStorageEnums.dataAccessLevelToStorage(DataAccessLevel.REGISTERED));
    } else {
      user.setDataAccessLevel(CommonStorageEnums.dataAccessLevelToStorage(DataAccessLevel.UNREGISTERED));
    }
    incrementedUserId++;
    userDao.save(user);
  }

}
