package org.pmiops.workbench.institution;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.model.Institution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import({InstitutionServiceImpl.class})
public class InstitutionServiceTest {
  @Autowired private InstitutionService service;

  private final Institution testInst =
      new Institution().shortName("test").displayName("this is a test");

  // the DB converts nulls to empty lists
  private final Institution testInstAfterRT =
      new Institution()
          .shortName(testInst.getShortName())
          .displayName(testInst.getDisplayName())
          .emailDomains(Collections.emptyList())
          .emailAddresses(Collections.emptyList());

  @Before
  public void setUp() {
    service.createInstitution(testInst);
  }

  @Test
  public void test_createInstitution() {
    assertThat(service.getInstitutions()).hasSize(1);

    service.createInstitution(
        new Institution().shortName("otherInst").displayName("The Institution of testing"));
    assertThat(service.getInstitutions()).hasSize(2);
  }

  @Test
  public void test_deleteInstitution() {
    assertThat(service.getInstitutions()).hasSize(1);

    service.deleteInstitution(testInst.getShortName());
    assertThat(service.getInstitutions()).hasSize(0);
  }

  @Test
  public void test_getInstitutions() {
    assertThat(service.getInstitutions()).containsExactly(testInstAfterRT);

    final Institution otherInst =
        new Institution()
            .shortName("otherInst")
            .displayName("The Institution of testing")
            .emailDomains(new ArrayList<>())
            .emailAddresses(new ArrayList<>());
    service.createInstitution(otherInst);
    assertThat(service.getInstitutions()).containsExactly(testInstAfterRT, otherInst);

    service.deleteInstitution(testInst.getShortName());
    assertThat(service.getInstitutions()).containsExactly(otherInst);
  }

  @Test
  public void test_getInstitution() {
    assertThat(service.getInstitution(testInst.getShortName())).hasValue(testInstAfterRT);
    assertThat(service.getInstitution("otherInst")).isEmpty();

    final Institution otherInst =
        new Institution()
            .shortName("otherInst")
            .displayName("The Institution of testing")
            .emailAddresses(new ArrayList<>())
            .emailDomains(new ArrayList<>());
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

    final Set<String> uniquifiedEmailDomains = new HashSet<>(instWithDupes.getEmailDomains());
    final Set<String> uniquifiedEmailAddresses = new HashSet<>(instWithDupes.getEmailAddresses());

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
    assertThat(service.deleteInstitution("missing")).isFalse();
  }

  @Test(expected = DataIntegrityViolationException.class)
  public void test_nonUniqueIds() {
    service.createInstitution(
        new Institution().shortName("test").displayName("We are all individuals"));
    service.createInstitution(new Institution().shortName("test").displayName("I'm not"));
  }
}
