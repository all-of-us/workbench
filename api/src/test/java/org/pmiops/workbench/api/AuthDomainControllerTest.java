package org.pmiops.workbench.api;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.pmiops.workbench.access.AccessTierService.REGISTERED_TIER_SHORT_NAME;

import java.time.Instant;
import java.util.Optional;
import javax.inject.Provider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.firecloud.model.FirecloudManagedGroupWithMembers;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.model.AuthDomainCreatedResponse;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.test.FakeClock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Import(FakeClockConfiguration.class)
public class AuthDomainControllerTest {

  private static final String GIVEN_NAME = "Bob";
  private static final String FAMILY_NAME = "Bobberson";
  private static final String CONTACT_EMAIL = "bob@example.com";
  private static final String PRIMARY_EMAIL = "bob@researchallofus.org";
  private static final String RESEARCH_PURPOSE = "To test things";

  @Autowired private UserDao userDao;

  @Mock private FireCloudService fireCloudService;
  @Mock private Provider<DbUser> userProvider;
  @Mock private InstitutionService mockInstitutionService;

  private AuthDomainController authDomainController;
  private Institution institution = new Institution();

  private final String testGroupEmail = "test-group@google.com";
  private final FirecloudManagedGroupWithMembers testGroup =
      new FirecloudManagedGroupWithMembers().groupEmail(testGroupEmail);

  @BeforeEach
  public void setUp() {
    DbUser adminUser = new DbUser();
    adminUser.setUserId(0L);
    when(fireCloudService.createGroup(any())).thenReturn(testGroup);
    when(userProvider.get()).thenReturn(adminUser);
    when(mockInstitutionService.getByUser(any(DbUser.class))).thenReturn(Optional.of(institution));
    when(mockInstitutionService.validateInstitutionalEmail(
            eq(institution), anyString(), eq(REGISTERED_TIER_SHORT_NAME)))
        .thenReturn(true);
    WorkbenchConfig config = WorkbenchConfig.createEmptyConfig();
    config.access.renewal.expiryDays = 365L;
    FakeClock clock = new FakeClock(Instant.now());

    this.authDomainController = new AuthDomainController(fireCloudService);
  }

  @Test
  public void testCreateAuthDomain() {
    final String testDomain = "my-auth-domain";
    final ResponseEntity<AuthDomainCreatedResponse> response =
        this.authDomainController.createAuthDomain(testDomain);
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody())
        .isEqualTo(
            new AuthDomainCreatedResponse().authDomainName(testDomain).groupEmail(testGroupEmail));
  }

  //  @Test
  //  public void testDisableUser() {
  //    final boolean oldDisabledValue = false;
  //    final DbUser createdUser = createUser(oldDisabledValue);
  //
  //    final boolean newDisabledValue = true;
  //    UpdateUserDisabledRequest request =
  //        new UpdateUserDisabledRequest().email(PRIMARY_EMAIL).disabled(newDisabledValue);
  //    ResponseEntity<Void> response = this.authDomainController.updateUserDisabledStatus(request);
  //    verify(mockAuthDomainAuditAdapter)
  //        .fireSetAccountDisabledStatus(createdUser.getUserId(), newDisabledValue,
  // oldDisabledValue);
  //    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
  //    DbUser updatedUser = userDao.findUserByUsername(PRIMARY_EMAIL);
  //    assertThat(updatedUser.getDisabled()).isTrue();
  //  }
  //
  //  @Test
  //  public void testEnableUser() {
  //    final boolean oldDisabledValue = true;
  //    final DbUser createdUser = createUser(oldDisabledValue);
  //
  //    final boolean newDisabledValue = false;
  //    UpdateUserDisabledRequest request =
  //        new UpdateUserDisabledRequest().email(PRIMARY_EMAIL).disabled(newDisabledValue);
  //
  //    ResponseEntity<Void> response = this.authDomainController.updateUserDisabledStatus(request);
  //    verify(mockAuthDomainAuditAdapter)
  //        .fireSetAccountDisabledStatus(createdUser.getUserId(), newDisabledValue,
  // oldDisabledValue);
  //    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
  //    DbUser updatedUser = userDao.findUserByUsername(PRIMARY_EMAIL);
  //    assertThat(updatedUser.getDisabled()).isFalse();
  //  }

  private DbUser createUser(boolean disabled) {
    DbUser user = new DbUser();
    user.setGivenName(GIVEN_NAME);
    user.setFamilyName(FAMILY_NAME);
    user.setUsername(PRIMARY_EMAIL);
    user.setContactEmail(CONTACT_EMAIL);
    user.setAreaOfResearch(RESEARCH_PURPOSE);
    user.setDisabled(disabled);
    return userDao.save(user);
  }
}
