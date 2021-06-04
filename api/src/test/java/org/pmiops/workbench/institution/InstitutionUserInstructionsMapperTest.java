package org.pmiops.workbench.institution;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.pmiops.workbench.SpringTest;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionUserInstructions;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.InstitutionUserInstructions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

@Import(InstitutionUserInstructionsMapperImpl.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class InstitutionUserInstructionsMapperTest extends SpringTest {
  @Autowired InstitutionUserInstructionsMapper mapper;

  @Test
  public void test_modelToDb() {
    final DbInstitution broad =
        new DbInstitution().setShortName("Broad").setDisplayName("The Broad Institute");

    final InstitutionUserInstructions userInstructions =
        new InstitutionUserInstructions()
            .instructions("Wash your hands")
            .institutionShortName(broad.getShortName());

    final DbInstitutionUserInstructions dbUserInstructions =
        mapper.modelToDb(userInstructions, broad);

    assertThat(dbUserInstructions.getUserInstructions()).isEqualTo("Wash your hands");
    assertThat(dbUserInstructions.getInstitution()).isEqualTo(broad);
  }

  @Test
  public void test_modelToDb_nullInstructions() {
    final DbInstitution broad =
        new DbInstitution().setShortName("Broad").setDisplayName("The Broad Institute");

    final InstitutionUserInstructions userInstructions =
        new InstitutionUserInstructions().institutionShortName(broad.getShortName());

    assertThrows(BadRequestException.class, () -> mapper.modelToDb(userInstructions, broad));
  }

  @Test
  public void test_modelToDb_emptyInstructions() {
    final DbInstitution broad =
        new DbInstitution().setShortName("Broad").setDisplayName("The Broad Institute");

    final InstitutionUserInstructions userInstructions =
        new InstitutionUserInstructions()
            .instructions("")
            .institutionShortName(broad.getShortName());

    assertThrows(BadRequestException.class, () -> mapper.modelToDb(userInstructions, broad));
  }
}
