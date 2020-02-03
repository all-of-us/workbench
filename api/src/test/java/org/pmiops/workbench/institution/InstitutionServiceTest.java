package org.pmiops.workbench.institution;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.model.Institution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
public class InstitutionServiceTest {
  @Autowired private InstitutionService service;

  @TestConfiguration
  @Import({InstitutionServiceImpl.class})
  static class Configuration {}

  @Test
  public void test_InstitutionCRUD() {
    assertThat(service.getInstitutions()).isEmpty();

    final Institution inst = new Institution().shortName("test").displayName("this is a test");
    assertThat(service.createInstitution(inst)).isEqualTo(inst);
    assertThat(service.getInstitution(inst.getShortName())).isEqualTo(Optional.of(inst));

    final List<Institution> institutions = service.getInstitutions();
    assertThat(institutions).hasSize(1);
    assertThat(institutions.get(0)).isEqualTo(inst);

    final Institution modifiedInst =
        new Institution().shortName(inst.getShortName()).displayName("I have altered the test");
    assertThat(service.updateInstitution(inst.getShortName(), modifiedInst))
        .isEqualTo(Optional.of(modifiedInst));
    assertThat(service.getInstitution(inst.getShortName())).isEqualTo(Optional.of(modifiedInst));

    service.deleteInstitution(inst.getShortName());
    assertThat(service.getInstitutions()).isEmpty();

    // we uniquify Email Addresses and Domains in the DB

    final Institution newInst =
        new Institution()
            .shortName("test2")
            .displayName("another test")
            .emailDomains(ImmutableList.of("broad.org", "broad.org", "google.com"))
            .emailAddresses(
                ImmutableList.of("joel@broad.org", "joel@broad.org", "joel@google.com"));

    final Set<String> uniquifiedEmailDomains = new HashSet<>(newInst.getEmailDomains());
    final Set<String> uniquifiedEmailAddresses = new HashSet<>(newInst.getEmailAddresses());

    final Institution dbInst = service.createInstitution(newInst);

    assertThat(dbInst).isNotEqualTo(newInst);
    assertThat(dbInst.getEmailDomains()).containsExactlyElementsIn(uniquifiedEmailDomains);
    assertThat(dbInst.getEmailAddresses()).containsExactlyElementsIn(uniquifiedEmailAddresses);

    // displayName and email patterns can match other institutions

    final Institution otherInst =
        new Institution()
            .shortName("otherInst")
            .displayName(newInst.getDisplayName())
            .emailDomains(newInst.getEmailDomains())
            .emailAddresses(newInst.getEmailAddresses());

    final Institution otherDbInst = service.createInstitution(otherInst);

    assertThat(otherDbInst.getShortName()).isNotEqualTo(dbInst.getShortName());
    assertThat(otherDbInst.getDisplayName()).isEqualTo(dbInst.getDisplayName());
    assertThat(otherDbInst.getEmailDomains()).containsExactlyElementsIn(dbInst.getEmailDomains());
    assertThat(otherDbInst.getEmailAddresses())
        .containsExactlyElementsIn(dbInst.getEmailAddresses());
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
