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
import org.pmiops.workbench.model.InstitutionMembershipRequirement;
import org.pmiops.workbench.model.InstitutionTierConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

@Import(InstitutionMapperImpl.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class InstitutionMapperTest extends SpringTest {
  private static final String TIER_NAME = "REGISTERED";
  @Autowired InstitutionMapper mapper;
  @MockBean InstitutionService service;

  private List<String> sortedModelDomains;
  private List<String> sortedModelAddresses;
  private List<InstitutionTierConfig> tierConfigs;

  @BeforeEach
  public void setup() {
    sortedModelDomains = Lists.newArrayList("broad.org", "verily.com");
    sortedModelAddresses = Lists.newArrayList("alice@nih.gov", "joel@other-inst.org");
    tierConfigs =
        Lists.newArrayList(
            new InstitutionTierConfig()
                .accessTierShortName(TIER_NAME)
                .membershipRequirement(InstitutionMembershipRequirement.DOMAINS)
                .eraRequired(true)
                .emailDomains(sortedModelDomains)
                .emailAddresses(sortedModelAddresses));
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
            .tierConfigs(tierConfigs)
            .userInstructions("UserInstruction");

    final DbInstitution dbInst = mapper.modelToDb(inst);

    assertThat(dbInst.getShortName()).isEqualTo(inst.getShortName());
    assertThat(dbInst.getDisplayName()).isEqualTo(inst.getDisplayName());

    when(service.getTierConfigs(inst.getShortName())).thenReturn(inst.getTierConfigs());
    when(service.getInstitutionUserInstructions(inst.getShortName()))
        .thenReturn(Optional.of(inst.getUserInstructions()));

    final Institution roundTrip = mapper.dbToModel(dbInst, service);

    assertThat(roundTrip.getShortName()).isEqualTo(inst.getShortName());
    assertThat(roundTrip.getDisplayName()).isEqualTo(inst.getDisplayName());
    assertThat(roundTrip.getTierConfigs()).isEqualTo(tierConfigs);
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
  public void test_populateFromAuxTables_empty() {
    final Institution instToPopulate = new Institution().shortName("ShortName");

    when(service.getTierConfigs("ShortName")).thenReturn(Collections.emptyList());
    when(service.getInstitutionUserInstructions("ShortName")).thenReturn(Optional.empty());

    mapper.populateFromAuxTables(instToPopulate, service);

    assertThat(instToPopulate.getTierConfigs()).isEmpty();
    assertThat(instToPopulate.getUserInstructions()).isNull();
  }
}
