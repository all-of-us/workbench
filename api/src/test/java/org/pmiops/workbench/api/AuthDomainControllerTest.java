package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.actionaudit.adapters.AuthDomainAuditAdapter;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.ManagedGroupWithMembers;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.UpdateUserDisabledRequest;
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
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class AuthDomainControllerTest {

  private static final String GIVEN_NAME = "Bob";
  private static final String FAMILY_NAME = "Bobberson";
  private static final String CONTACT_EMAIL = "bob@example.com";
  private static final String PRIMARY_EMAIL = "bob@researchallofus.org";
  private static final String ORGANIZATION = "Test";
  private static final String CURRENT_POSITION = "Tester";
  private static final String RESEARCH_PURPOSE = "To test things";

  @Mock private FireCloudService mockFireCloudService;
  @Mock private AuthDomainAuditAdapter mockAuthDomainAuditAdapter;
  @Mock private UserService mockUserService;

  @Autowired private UserDao userDao;

  private AuthDomainController authDomainController;

  @Before
  public void setUp() {
    doNothing().when(mockFireCloudService).addUserToBillingProject(any(), any());
    doNothing().when(mockFireCloudService).removeUserFromBillingProject(any(), any());
    when(mockFireCloudService.createGroup(any())).thenReturn(new ManagedGroupWithMembers());

    this.authDomainController =
        new AuthDomainController(
            mockFireCloudService, mockUserService, userDao, mockAuthDomainAuditAdapter);
  }

  @Test
  public void testCreateAuthDomain() {
    ResponseEntity<EmptyResponse> response = this.authDomainController.createAuthDomain("");
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  public void testDisableUser() {
    DbUser createdUser = createUser(false);
    doReturn(createdUser).when(mockUserService).setDisabledStatus(createdUser.getUserId(), true);

    UpdateUserDisabledRequest request =
        new UpdateUserDisabledRequest().email(PRIMARY_EMAIL).disabled(true);
    ResponseEntity<Void> response = this.authDomainController.updateUserDisabledStatus(request);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    DbUser updatedUser = userDao.findUserByEmail(PRIMARY_EMAIL);
    assertThat(updatedUser.getDisabled());
  }

  @Test
  public void testEnableUser() {
    DbUser createdUser = createUser(true);
    doReturn(createdUser).when(mockUserService).setDisabledStatus(createdUser.getUserId(), false);

    final UpdateUserDisabledRequest request =
        new UpdateUserDisabledRequest().email(PRIMARY_EMAIL).disabled(false);
    ResponseEntity<Void> response = this.authDomainController.updateUserDisabledStatus(request);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    DbUser updatedUser = userDao.findUserByEmail(PRIMARY_EMAIL);
    assertThat(!updatedUser.getDisabled());
  }

  private DbUser createUser(boolean disabled) {
    DbUser user = new DbUser();
    user.setGivenName(GIVEN_NAME);
    user.setFamilyName(FAMILY_NAME);
    user.setEmail(PRIMARY_EMAIL);
    user.setContactEmail(CONTACT_EMAIL);
    user.setOrganization(ORGANIZATION);
    user.setCurrentPosition(CURRENT_POSITION);
    user.setAreaOfResearch(RESEARCH_PURPOSE);
    user.setDisabled(disabled);
    return userDao.save(user);
  }
}
