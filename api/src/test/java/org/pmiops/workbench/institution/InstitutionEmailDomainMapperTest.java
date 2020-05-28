package org.pmiops.workbench.institution;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionEmailDomain;
import org.pmiops.workbench.model.Institution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@Import(InstitutionEmailDomainMapperImpl.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class InstitutionEmailDomainMapperTest {
  @Autowired InstitutionEmailDomainMapper mapper;

  @Test
  public void test_modelToDb() {
    // contains an out-of-order duplicate
    final List<String> rawDomains = Lists.newArrayList("nih.gov", "other-inst.org", "nih.gov");
    final List<String> sortedDistinctDomains =
        rawDomains.stream().sorted().distinct().collect(Collectors.toList());

    final Institution modelInst =
        new Institution()
            .shortName("Broad")
            .displayName("The Broad Institute")
            .emailDomains(rawDomains);

    // does not need to match the modelInst; it is simply attached to the DbInstitutionEmailDomain
    final DbInstitution dbInst = new DbInstitution();

    final Set<DbInstitutionEmailDomain> dbDomains = mapper.modelToDb(modelInst, dbInst);

    assertThat(dbDomains.size()).isEqualTo(sortedDistinctDomains.size());

    for (final DbInstitutionEmailDomain dbDomain : dbDomains) {
      assertThat(sortedDistinctDomains).contains(dbDomain.getEmailDomain());
      assertThat(dbDomain.getInstitution()).isEqualTo(dbInst);
    }

    final List<String> roundTripModelDomains = mapper.dbToModel(dbDomains);
    assertThat(roundTripModelDomains).isEqualTo(sortedDistinctDomains);
  }

  @Test
  public void test_modelToDb_null() {
    final Institution modelInst =
        new Institution().shortName("Broad").displayName("The Broad Institute").emailDomains(null);

    // does not need to match the modelInst; it is simply attached to the DbInstitutionEmailDomain
    final DbInstitution dbInst = new DbInstitution();

    final Set<DbInstitutionEmailDomain> dbDomains = mapper.modelToDb(modelInst, dbInst);

    assertThat(dbDomains).isNotNull();
    assertThat(dbDomains).isEmpty();
  }

  @Test
  public void test_dbToModel() {
    final DbInstitution dbInst =
        new DbInstitution().setShortName("Broad").setDisplayName("The Broad Institute");

    final Set<DbInstitutionEmailDomain> dbDomains =
        Sets.newHashSet(
            new DbInstitutionEmailDomain().setEmailDomain("vumc.org").setInstitution(dbInst),
            new DbInstitutionEmailDomain().setEmailDomain("broad.org").setInstitution(dbInst),
            new DbInstitutionEmailDomain().setEmailDomain("vumc.org").setInstitution(dbInst));

    // sorted and de-duplicated (the Set should not store the duplicate anyway)
    final List<String> expected = Lists.newArrayList("broad.org", "vumc.org");

    final List<String> modelDomains = mapper.dbToModel(dbDomains);
    assertThat(modelDomains).isEqualTo(expected);

    // does not need to match dbInst: we only care about its emailDomains
    final Institution modelInst =
        new Institution()
            .shortName("Whatever")
            .displayName("Whatever Tech")
            .emailDomains(modelDomains);

    final Set<DbInstitutionEmailDomain> roundTripDbDomains = mapper.modelToDb(modelInst, dbInst);
    assertThat(roundTripDbDomains).isEqualTo(dbDomains);
  }

  @Test
  public void test_dbToModel_null() {
    final List<String> modelDomains = mapper.dbToModel(null);
    assertThat(modelDomains).isNotNull();
    assertThat(modelDomains).isEmpty();
  }
}
