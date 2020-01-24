package org.pmiops.workbench.institution;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.exceptions.NotFoundException;
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

    final Institution inst = new Institution().id("test").longName("this is a test");
    assertThat(service.createInstitution(inst)).isEqualTo(inst);
    assertThat(service.getInstitution("test")).isEqualTo(inst);

    List<Institution> institutions = service.getInstitutions();
    assertThat(institutions).hasSize(1);
    assertThat(institutions.get(0)).isEqualTo(inst);

    final Institution newInst = new Institution().id("test").longName("I have altered the test");
    assertThat(service.updateInstitution("test", newInst)).isEqualTo(newInst);
    assertThat(service.getInstitution("test")).isEqualTo(newInst);

    service.deleteInstitution("test");
    assertThat(service.getInstitutions()).isEmpty();
  }

  @Test(expected = NotFoundException.class)
  public void test_InstitutionNotFoundGet() {
    service.getInstitution("missing");
  }

  @Test(expected = NotFoundException.class)
  public void test_InstitutionNotFoundUpdate() {
    service.updateInstitution("missing", new Institution());
  }

  @Test(expected = NotFoundException.class)
  public void test_InstitutionNotFoundDelete() {
    service.deleteInstitution("missing");
  }

  @Test(expected = DataIntegrityViolationException.class)
  public void test_nonUniqueIds() {
    service.createInstitution(new Institution().id("test").longName("We are all individuals"));
    service.createInstitution(new Institution().id("test").longName("I'm not"));
  }
}
