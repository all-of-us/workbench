package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import javax.inject.Provider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.actionaudit.auditors.AuthDomainAuditor;
import org.pmiops.workbench.actionaudit.auditors.UserServiceAuditor;
import org.pmiops.workbench.compliance.ComplianceService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.AdminActionHistoryDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.UserDataUseAgreementDao;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.dao.UserServiceImpl;
import org.pmiops.workbench.db.dao.UserTermsOfServiceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudManagedGroupWithMembers;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.UpdateUserDisabledRequest;
import org.pmiops.workbench.test.FakeClock;
import org.pmiops.workbench.test.FakeLongRandom;
import org.pmiops.workbench.test.Providers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class AuthDomainControllerTest {

  private static final String GIVEN_NAME = "Bob";
  private static final String FAMILY_NAME = "Bobberson";
  private static final String CONTACT_EMAIL = "bob@example.com";
  private static final String PRIMARY_EMAIL = "bob@researchallofus.org";
  private static final String ORGANIZATION = "Test";
  private static final String CURRENT_POSITION = "Tester";
  private static final String RESEARCH_PURPOSE = "To test things";

  @Mock private AdminActionHistoryDao adminActionHistoryDao;
  @Mock private FireCloudService fireCloudService;
  @Mock private Provider<DbUser> userProvider;
  @Mock private ComplianceService complianceService;
  @Mock private DirectoryService directoryService;
  @Mock private UserServiceAuditor mockUserServiceAuditAdapter;
  @Mock private AuthDomainAuditor mockAuthDomainAuditAdapter;
  @Autowired private UserDao userDao;
  @Mock private UserDataUseAgreementDao userDataUseAgreementDao;
  @Mock private UserTermsOfServiceDao userTermsOfServiceDao;

  private AuthDomainController authDomainController;

  @Before
  public void setUp() {
    DbUser adminUser = new DbUser();
    adminUser.setUserId(0L);
    when(fireCloudService.createGroup(any())).thenReturn(new FirecloudManagedGroupWithMembers());
    when(userProvider.get()).thenReturn(adminUser);
    WorkbenchConfig config = new WorkbenchConfig();
    config.firecloud = new WorkbenchConfig.FireCloudConfig();
    config.firecloud.registeredDomainName = "";
    config.access = new WorkbenchConfig.AccessConfig();
    config.access.enableDataUseAgreement = true;
    FakeClock clock = new FakeClock(Instant.now());
    UserService userService =
        new UserServiceImpl(
            userProvider,
            userDao,
            adminActionHistoryDao,
            userTermsOfServiceDao,
            userDataUseAgreementDao,
            clock,
            new FakeLongRandom(12345),
            fireCloudService,
            Providers.of(config),
            complianceService,
            directoryService,
            mockUserServiceAuditAdapter);
    this.authDomainController =
        new AuthDomainController(
            fireCloudService, userService, userDao, mockAuthDomainAuditAdapter);
  }

  @Test
  public void testCreateAuthDomain() {
    ResponseEntity<EmptyResponse> response = this.authDomainController.createAuthDomain("");
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
  }

  @Test
  public void testDisableUser() {
    final boolean oldDisabledValue = false;
    final DbUser createdUser = createUser(oldDisabledValue);

    final boolean newDisabledValue = true;
    UpdateUserDisabledRequest request =
        new UpdateUserDisabledRequest().email(PRIMARY_EMAIL).disabled(newDisabledValue);
    ResponseEntity<Void> response = this.authDomainController.updateUserDisabledStatus(request);
    verify(mockAuthDomainAuditAdapter)
        .fireSetAccountDisabledStatus(createdUser.getUserId(), newDisabledValue, oldDisabledValue);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    DbUser updatedUser = userDao.findUserByUsername(PRIMARY_EMAIL);
    assertThat(updatedUser.getDisabled()).isTrue();
  }

  @Test
  public void testEnableUser() {
    final boolean oldDisabledValue = true;
    final DbUser createdUser = createUser(oldDisabledValue);

    final boolean newDisabledValue = false;
    UpdateUserDisabledRequest request =
        new UpdateUserDisabledRequest().email(PRIMARY_EMAIL).disabled(newDisabledValue);

    ResponseEntity<Void> response = this.authDomainController.updateUserDisabledStatus(request);
    verify(mockAuthDomainAuditAdapter)
        .fireSetAccountDisabledStatus(createdUser.getUserId(), newDisabledValue, oldDisabledValue);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    DbUser updatedUser = userDao.findUserByUsername(PRIMARY_EMAIL);
    assertThat(updatedUser.getDisabled()).isFalse();
  }

  private DbUser createUser(boolean disabled) {
    DbUser user = new DbUser();
    user.setGivenName(GIVEN_NAME);
    user.setFamilyName(FAMILY_NAME);
    user.setUsername(PRIMARY_EMAIL);
    user.setContactEmail(CONTACT_EMAIL);
    user.setOrganization(ORGANIZATION);
    user.setCurrentPosition(CURRENT_POSITION);
    user.setAreaOfResearch(RESEARCH_PURPOSE);
    user.setDisabled(disabled);
    return userDao.save(user);
  }
}
