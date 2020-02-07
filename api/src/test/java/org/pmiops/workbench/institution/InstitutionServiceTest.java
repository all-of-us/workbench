package org.pmiops.workbench.institution;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.VerifiedUserInstitutionDao;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbVerifiedUserInstitution;
import org.pmiops.workbench.institution.InstitutionService.DeletionResult;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.model.InstitutionalRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import({InstitutionServiceImpl.class, InstitutionMapperImpl.class})
public class InstitutionServiceTest {
  @Autowired private InstitutionService service;
  @Autowired private UserDao userDao;
  @Autowired private VerifiedUserInstitutionDao verifiedUserInstitutionDao;

  private final Institution TEST_INST =
      new Institution().shortName("test").displayName("this is a test");

  // the DB converts nulls to empty lists
  private final Institution TEST_INST_AFTER_RT =
      new Institution()
          .shortName(TEST_INST.getShortName())
          .displayName(TEST_INST.getDisplayName())
          .emailDomains(Collections.emptyList())
          .emailAddresses(Collections.emptyList());

  @Before
  public void setUp() {
    // will be retrieved as TEST_INST_AFTER_RT
    service.createInstitution(TEST_INST);
  }

  @Test
  public void test_createInstitution() {
    assertThat(service.getInstitutions()).containsExactly(TEST_INST_AFTER_RT);

    final Institution otherInst =
        new Institution()
            .shortName("otherInst")
            .displayName("The Institution of testing")
            .emailDomains(Collections.emptyList())
            .emailAddresses(Collections.emptyList());
    assertThat(service.createInstitution(otherInst)).isEqualTo(otherInst);

    assertThat(service.getInstitutions()).containsExactly(TEST_INST_AFTER_RT, otherInst);
  }

  @Test
  public void test_deleteInstitution() {
    assertThat(service.getInstitutions()).containsExactly(TEST_INST_AFTER_RT);

    assertThat(service.deleteInstitution(TEST_INST.getShortName()))
        .isEqualTo(DeletionResult.SUCCESS);
    assertThat(service.getInstitutions()).isEmpty();

    service.createInstitution(TEST_INST);
    assertThat(service.getInstitutions()).containsExactly(TEST_INST_AFTER_RT);

    createAffiliation(TEST_INST.getShortName());
    assertThat(service.deleteInstitution(TEST_INST.getShortName()))
        .isEqualTo(DeletionResult.HAS_VERIFIED_AFFILIATIONS);
    assertThat(service.getInstitutions()).containsExactly(TEST_INST_AFTER_RT);
  }

  @Test
  public void test_getInstitutions() {
    assertThat(service.getInstitutions()).containsExactly(TEST_INST_AFTER_RT);

    final Institution otherInst =
        new Institution()
            .shortName("otherInst")
            .displayName("The Institution of testing")
            .emailDomains(Collections.emptyList())
            .emailAddresses(Collections.emptyList());
    service.createInstitution(otherInst);
    assertThat(service.getInstitutions()).containsExactly(TEST_INST_AFTER_RT, otherInst);

    service.deleteInstitution(TEST_INST.getShortName());
    assertThat(service.getInstitutions()).containsExactly(otherInst);
  }

  @Test
  public void test_getInstitution() {
    assertThat(service.getInstitution(TEST_INST.getShortName())).hasValue(TEST_INST_AFTER_RT);
    assertThat(service.getInstitution("otherInst")).isEmpty();

    final Institution otherInst =
        new Institution()
            .shortName("otherInst")
            .displayName("The Institution of testing")
            .emailAddresses(Collections.emptyList())
            .emailDomains(Collections.emptyList());
    service.createInstitution(otherInst);
    assertThat(service.getInstitution("otherInst")).hasValue(otherInst);
  }

  // we uniquify Email Addresses and Domains in the DB per-institution
  @Test
  public void test_uniqueEmailPatterns() {
    final Institution instWithDupes =
        new Institution()
            .shortName("test2")
            .displayName("another test")
            .emailDomains(ImmutableList.of("broad.org", "broad.org", "google.com"))
            .emailAddresses(
                ImmutableList.of("joel@broad.org", "joel@broad.org", "joel@google.com"));

    final Set<String> uniquifiedEmailDomains = Sets.newHashSet(instWithDupes.getEmailDomains());
    final Set<String> uniquifiedEmailAddresses = Sets.newHashSet(instWithDupes.getEmailAddresses());

    final Institution uniquifiedInst = service.createInstitution(instWithDupes);

    assertThat(uniquifiedInst).isNotEqualTo(instWithDupes);
    assertThat(uniquifiedInst.getEmailDomains()).containsExactlyElementsIn(uniquifiedEmailDomains);
    assertThat(uniquifiedInst.getEmailAddresses())
        .containsExactlyElementsIn(uniquifiedEmailAddresses);
  }

  // we do not uniquify Email Addresses and Domains in the DB across institutions
  @Test
  public void test_nonUniqueEmailPatterns() {
    final Institution instWithEmails =
        new Institution()
            .shortName("hasEmails")
            .displayName("another test")
            .emailDomains(ImmutableList.of("broad.org", "google.com"))
            .emailAddresses(ImmutableList.of("joel@broad.org", "joel@google.com"));

    final Institution similarInst =
        new Institution()
            .shortName("otherInst")
            .displayName("The University of Elsewhere")
            .emailDomains(instWithEmails.getEmailDomains())
            .emailAddresses(instWithEmails.getEmailAddresses());

    final Institution instWithEmailsViaDb = service.createInstitution(instWithEmails);
    final Institution similarInstViaDb = service.createInstitution(similarInst);

    assertThat(instWithEmailsViaDb.getShortName()).isNotEqualTo(similarInstViaDb.getShortName());
    assertThat(instWithEmailsViaDb.getDisplayName())
        .isNotEqualTo(similarInstViaDb.getDisplayName());
    assertThat(instWithEmailsViaDb.getEmailDomains())
        .containsExactlyElementsIn(similarInstViaDb.getEmailDomains());
    assertThat(instWithEmailsViaDb.getEmailAddresses())
        .containsExactlyElementsIn(similarInstViaDb.getEmailAddresses());
  }

  @Test
  public void test_InstitutionNotFound() {
    assertThat(service.getInstitution("missing")).isEmpty();
    assertThat(service.updateInstitution("missing", new Institution())).isEmpty();
    assertThat(service.deleteInstitution("missing")).isEqualTo(DeletionResult.NOT_FOUND);
  }

  @Test(expected = DataIntegrityViolationException.class)
  public void test_nonUniqueIds() {
    service.createInstitution(
        new Institution().shortName("test").displayName("We are all individuals"));
    service.createInstitution(new Institution().shortName("test").displayName("I'm not"));
  }

  private DbVerifiedUserInstitution createAffiliation(final String instName) {
    final DbUser user = userDao.save(new DbUser());
    final DbInstitution inst = service.getDbInstitution(instName).get();
    final DbVerifiedUserInstitution affiliation =
        new DbVerifiedUserInstitution()
            .setUser(user)
            .setInstitution(inst)
            .setInstitutionalRoleEnum(InstitutionalRole.FELLOW);
    return verifiedUserInstitutionDao.save(affiliation);
  }
}
