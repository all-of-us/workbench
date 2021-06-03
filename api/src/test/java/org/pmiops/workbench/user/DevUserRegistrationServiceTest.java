package org.pmiops.workbench.user;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.google.api.services.oauth2.model.Userinfoplus;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.pmiops.workbench.db.dao.UserService;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbVerifiedInstitutionalAffiliation;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.google.DirectoryService;
import org.pmiops.workbench.institution.InstitutionService;
import org.pmiops.workbench.institution.VerifiedInstitutionalAffiliationMapperImpl;
import org.pmiops.workbench.model.Institution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

public class DevUserRegistrationServiceTest {

  @MockBean private DirectoryService directoryService;
  @MockBean private InstitutionService institutionService;
  @MockBean private UserService userService;

  private Userinfoplus userInfo;
  private ArgumentCaptor<DbVerifiedInstitutionalAffiliation> dbAffiliationCaptor =
      ArgumentCaptor.forClass(DbVerifiedInstitutionalAffiliation.class);

  @Autowired private DevUserRegistrationService service;

  @TestConfiguration
  @Import({DevUserRegistrationServiceImpl.class, VerifiedInstitutionalAffiliationMapperImpl.class})
  static class Configuration {}

  @BeforeEach
  public void setUp() {
    userInfo = new Userinfoplus().setEmail("gjordan@fake-research-aou.org");
  }

  @Test
  public void testCreateUserFromUserInfo() {
    // Tests the happy path: a contact email and matching institution are both found, allowing us
    // to register the dev user.
    DbUser dbUser = new DbUser();
    dbUser.setContactEmail("gregory.jordan.123@gmail.com");
    dbUser.setUsername("gjordan@fake-research-aou.org");
    DbInstitution dbInstitution = new DbInstitution();
    dbInstitution.setShortName("Google");

    when(directoryService.getContactEmail(eq("gjordan@fake-research-aou.org")))
        .thenReturn(Optional.of("gregory.jordan.123@gmail.com"));
    when(institutionService.getFirstMatchingInstitution("gregory.jordan.123@gmail.com"))
        .thenReturn(Optional.of(new Institution().shortName("Google")));
    when(institutionService.getDbInstitutionOrThrow(eq("Google"))).thenReturn(dbInstitution);
    when(userService.createUser(
            eq(userInfo), eq("gregory.jordan.123@gmail.com"), dbAffiliationCaptor.capture()))
        .thenReturn(dbUser);

    service.createUser(userInfo);

    assertThat(dbAffiliationCaptor.getValue().getInstitution().getShortName()).isEqualTo("Google");
  }

  @Test(expected = BadRequestException.class)
  public void testCreateUserFromUserInfo_NoMatchingInstitution() {
    // If no matching institution could be found, an exception is thrown.
    when(directoryService.getContactEmail(eq("gjordan@fake-research-aou.org")))
        .thenReturn(Optional.of("gregory.jordan.123@gmail.com"));
    when(institutionService.getFirstMatchingInstitution("gregory.jordan.123@gmail.com"))
        .thenReturn(Optional.empty());

    service.createUser(userInfo);
  }
}
