package org.pmiops.workbench.institution;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.SpringTest;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.model.Institution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

@Import(InstitutionMapperImpl.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class InstitutionMapperTest extends SpringTest {
  @Autowired InstitutionMapper mapper;
  @MockBean InstitutionService service;

  private List<String> sortedModelDomains;
  private List<String> sortedModelAddresses;

  @BeforeEach
  public void setup() {
    sortedModelDomains = Lists.newArrayList("broad.org", "verily.com");
    sortedModelAddresses = Lists.newArrayList("alice@nih.gov", "joel@other-inst.org");
  }

  @Test
  public void test_modelToDb() {
    final Institution inst =
        new Institution().shortName("Test").displayName("Test State University");

    final DbInstitution dbInst = mapper.modelToDb(inst);

    assertThat(dbInst.getShortName()).isEqualTo(inst.getShortName());
    assertThat(dbInst.getDisplayName()).isEqualTo(inst.getDisplayName());

    final Institution roundTrip = mapper.dbToModel(dbInst, service);

    assertThat(roundTrip.getShortName()).isEqualTo(inst.getShortName());
    assertThat(roundTrip.getDisplayName()).isEqualTo(inst.getDisplayName());
  }

  @Test
  public void test_modelToDbWithEmailsAndUserInstructions() {
    final Institution inst =
        new Institution()
            .shortName("Test")
            .displayName("Test State University")
            .emailDomains(sortedModelDomains)
            .emailAddresses(sortedModelAddresses)
            .userInstructions("UserInstruction");

    final DbInstitution dbInst = mapper.modelToDb(inst);

    assertThat(dbInst.getShortName()).isEqualTo(inst.getShortName());
    assertThat(dbInst.getDisplayName()).isEqualTo(inst.getDisplayName());

    when(service.getEmailDomains(inst.getShortName())).thenReturn(inst.getEmailDomains());
    when(service.getEmailAddresses(inst.getShortName())).thenReturn(inst.getEmailAddresses());
    when(service.getInstitutionUserInstructions(inst.getShortName()))
        .thenReturn(Optional.of(inst.getUserInstructions()));

    final Institution roundTrip = mapper.dbToModel(dbInst, service);

    assertThat(roundTrip.getShortName()).isEqualTo(inst.getShortName());
    assertThat(roundTrip.getDisplayName()).isEqualTo(inst.getDisplayName());
    assertThat(roundTrip.getEmailDomains()).isEqualTo(sortedModelDomains);
    assertThat(roundTrip.getEmailAddresses()).isEqualTo(sortedModelAddresses);
    assertThat(roundTrip.getUserInstructions()).isEqualTo("UserInstruction");
  }

  @Test
  public void test_dbToModel() {
    final DbInstitution dbInst =
        new DbInstitution().setShortName("Test").setDisplayName("Test State University");

    final Institution inst = mapper.dbToModel(dbInst, service);

    assertThat(inst.getShortName()).isEqualTo(dbInst.getShortName());
    assertThat(inst.getDisplayName()).isEqualTo(dbInst.getDisplayName());

    final DbInstitution roundTrip = mapper.modelToDb(inst);

    assertThat(roundTrip.getShortName()).isEqualTo(dbInst.getShortName());
    assertThat(roundTrip.getDisplayName()).isEqualTo(dbInst.getDisplayName());
  }

  @Test
  public void test_populateFromAuxTables() {
    final Institution instToPopulate = new Institution().shortName("ShortName");

    final String instructions = "Bake a batch of brownies";

    when(service.getEmailDomains("ShortName")).thenReturn(sortedModelDomains);
    when(service.getEmailAddresses("ShortName")).thenReturn(sortedModelAddresses);
    when(service.getInstitutionUserInstructions("ShortName")).thenReturn(Optional.of(instructions));

    mapper.populateFromAuxTables(instToPopulate, service);

    assertThat(instToPopulate.getEmailDomains()).isEqualTo(sortedModelDomains);
    assertThat(instToPopulate.getEmailAddresses()).isEqualTo(sortedModelAddresses);
    assertThat(instToPopulate.getUserInstructions()).isEqualTo(instructions);
  }

  @Test
  public void test_populateFromAuxTables_empty() {
    final Institution instToPopulate = new Institution().shortName("ShortName");

    when(service.getEmailDomains("ShortName")).thenReturn(Collections.emptyList());
    when(service.getEmailAddresses("ShortName")).thenReturn(Collections.emptyList());
    when(service.getInstitutionUserInstructions("ShortName")).thenReturn(Optional.empty());

    mapper.populateFromAuxTables(instToPopulate, service);

    assertThat(instToPopulate.getEmailDomains()).isEmpty();
    assertThat(instToPopulate.getEmailAddresses()).isEmpty();
    assertThat(instToPopulate.getUserInstructions()).isNull();
  }
}
