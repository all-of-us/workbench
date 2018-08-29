package org.pmiops.workbench.api;

import java.util.Comparator;
import java.util.List;

import com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.model.UserResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static com.google.common.truth.Truth.assertThat;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class UserControllerTest {

  @Autowired
  private UserDao userDao;

  private UserController userController;

  @Before
  public void setUp() {
    this.userController = new UserController(userDao);
    saveFamily();
  }

  @After
  public void tearDown() {
    userDao.deleteAll();
  }

  @Test
  public void testUserSearch() {
    User john = userDao.findUserByEmail("john@lis.org");

    List<UserResponse> users = userController.user("John", null, null, null).getBody();
    assertThat(users).hasSize(1);
    assertThat(users.get(0).getEmail()).isSameAs(john.getEmail());
  }

  @Test
  public void testUserPartialStringSearch() {
    List<User> allUsers = Lists.newArrayList(userDao.findAll());

    List<UserResponse> users = userController.user("obin", null, null, null).getBody();
    assertThat(users).hasSize(allUsers.size());
  }

  @Test
  public void testUserEmptyResponse() {
    List<UserResponse> emptyString = userController.user("", null, null, null).getBody();
    assertThat(emptyString).hasSize(0);
  }

  @Test
  public void testUserNoUsersResponse() {
    List<UserResponse> noSmiths = userController.user("Smith", null, null, null).getBody();
    assertThat(noSmiths).hasSize(0);
  }

  @Test
  public void testUserPageSize() {
    int size = 1;
    List<UserResponse> robinsons_0 = userController.user("Robinson", 0, size, null).getBody();
    List<UserResponse> robinsons_1 = userController.user("Robinson", 1, size, null).getBody();
    List<UserResponse> robinsons_2 = userController.user("Robinson", 2, size, null).getBody();
    List<UserResponse> robinsons_3 = userController.user("Robinson", 3, size, null).getBody();
    List<UserResponse> robinsons_4 = userController.user("Robinson", 4, size, null).getBody();

    assertThat(robinsons_0).hasSize(size);
    assertThat(robinsons_1).hasSize(size);
    assertThat(robinsons_2).hasSize(size);
    assertThat(robinsons_3).hasSize(size);
    assertThat(robinsons_4).hasSize(size);
  }

  @Test
  public void testUserPagedResponses() {
    List<UserResponse> robinsons_0_1 = userController.user("Robinson", 0, 2, null).getBody();
    List<UserResponse> robinsons_2_3 = userController.user("Robinson", 1, 2, null).getBody();
    List<UserResponse> robinsons_4 = userController.user("Robinson", 3, 1, null).getBody();

    // Assert the expected size for each page
    assertThat(robinsons_0_1).hasSize(2);
    assertThat(robinsons_2_3).hasSize(2);
    assertThat(robinsons_4).hasSize(1);

    // Assert uniqueness across pages
    assertThat(robinsons_0_1).containsNoneOf(robinsons_2_3, robinsons_4);
    assertThat(robinsons_2_3).containsNoneOf(robinsons_0_1, robinsons_4);
    assertThat(robinsons_4).containsNoneOf(robinsons_0_1, robinsons_2_3);

  }

  @Test
  public void testUserSort() {
    List<UserResponse> robinsonsAsc = userController.user("Robinson", null, null, "asc").getBody();
    List<UserResponse> robinsonsDesc = userController.user("Robinson", null, null, "desc").getBody();

    // Assert we have the same elements in both responses
    assertThat(robinsonsAsc).containsAllIn(robinsonsDesc);

    // Now reverse one and assert both in the same order
    List<UserResponse> descendingReversed = Lists.reverse(robinsonsDesc);
    assertThat(robinsonsAsc).containsAllIn(descendingReversed).inOrder();

    // Test that JPA sorting is really what we expected it to be by re-sorting one into a new list
    List<UserResponse> newAscending = Lists.newArrayList(robinsonsAsc);
    newAscending.sort(Comparator.comparing(UserResponse::getEmail));
    assertThat(robinsonsAsc).containsAllIn(newAscending).inOrder();

  }

  /*
   * Testing helpers
   */

  private void saveFamily() {
    saveUser("john@lis.org", "John", "Robinson");
    saveUser("judy@lis.org", "Judy", "Robinson");
    saveUser("maureen@lis.org", "Mauren", "Robinson");
    saveUser("penny@lis.org", "Penny", "Robinson");
    saveUser("will@lis.org", "Will", "Robinson");
  }

  private Long incrementedUserId = 1L;

  @SuppressWarnings("SameParameterValue")
  private void saveUser(String email, String givenName, String familyName) {
    User user = new User();
    user.setEmail(email);
    user.setUserId(incrementedUserId);
    user.setGivenName(givenName);
    user.setFamilyName(familyName);
    incrementedUserId++;
    userDao.save(user);
  }

}
