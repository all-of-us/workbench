package org.pmiops.workbench.api;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.ManagedGroupWithMembers;
import org.pmiops.workbench.model.AuthDomainDisableUserRequest;
import org.pmiops.workbench.model.EmptyResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
public class AuthDomainControllerTest {

  private static final String GIVEN_NAME = "Bob";
  private static final String FAMILY_NAME = "Bobberson";
  private static final String CONTACT_EMAIL = "bob@example.com";
  private static final String PRIMARY_EMAIL = "bob@researchallofus.org";
  private static final String ORGANIZATION = "Test";
  private static final String CURRENT_POSITION = "Tester";
  private static final String RESEARCH_PURPOSE = "To test things";

  @Mock
  private FireCloudService fireCloudService;
  @Mock
  private UserDao userDao;
  @Mock
  private UserService userService;

  private AuthDomainController authDomainController;

  @Before
  public void setUp() {
    User user = createUser();
    doNothing().when(fireCloudService).addUserToBillingProject(any(), any());
    doNothing().when(fireCloudService).removeUserFromBillingProject(any(), any());
    when(fireCloudService.createGroup(any())).thenReturn(new ManagedGroupWithMembers());
    when(userDao.findUserByEmail(any())).thenReturn(user);
    when(userService.setDisabledStatus(any(), anyBoolean())).thenReturn(user);
    this.authDomainController = new AuthDomainController(fireCloudService, userService, userDao);
  }

  @Test
  public void testCreateAuthDomain() {
    ResponseEntity<EmptyResponse> response = this.authDomainController.createAuthDomain("");
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  public void testDisableUser() {
    AuthDomainDisableUserRequest request = new AuthDomainDisableUserRequest().
        email(PRIMARY_EMAIL).
        disabled(true);
    ResponseEntity<Void> response = this.authDomainController.disableUser("", request);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
  }

  private User createUser() {
    User user = new User();
    user.setUserId(1L);
    user.setGivenName(GIVEN_NAME);
    user.setFamilyName(FAMILY_NAME);
    user.setEmail(PRIMARY_EMAIL);
    user.setContactEmail(CONTACT_EMAIL);
    user.setOrganization(ORGANIZATION);
    user.setCurrentPosition(CURRENT_POSITION);
    user.setAreaOfResearch(RESEARCH_PURPOSE);
    user.setDisabled(false);
    return user;
  }

}
