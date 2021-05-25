package org.pmiops.workbench.institution;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.SpringTest;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionEmailAddress;
import org.pmiops.workbench.model.Institution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@ExtendWith(SpringExtension.class)
@Import(InstitutionEmailAddressMapperImpl.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class InstitutionEmailAddressMapperTest extends SpringTest {
  @Autowired InstitutionEmailAddressMapper mapper;

  @Test
  public void test_modelToDb() {
    // contains an out-of-order duplicate
    final List<String> rawAddresses =
        Lists.newArrayList("alice@nih.gov", "joel@other-inst.org", "alice@nih.gov");
    final SortedSet<String> sortedDistinctAddresses = new TreeSet<>(rawAddresses);

    final Institution modelInst =
        new Institution()
            .shortName("Broad")
            .displayName("The Broad Institute")
            .emailAddresses(rawAddresses);

    // does not need to match the modelInst; it is simply attached to the DbInstitutionEmailAddress
    final DbInstitution dbInst = new DbInstitution();

    final Set<DbInstitutionEmailAddress> dbAddresses = mapper.modelToDb(modelInst, dbInst);

    assertThat(dbAddresses).hasSize(sortedDistinctAddresses.size());

    for (final DbInstitutionEmailAddress dbAddress : dbAddresses) {
      assertThat(sortedDistinctAddresses).contains(dbAddress.getEmailAddress());
      assertThat(dbAddress.getInstitution()).isEqualTo(dbInst);
    }

    final SortedSet<String> roundTripModelAddresses = mapper.dbAddressesToStrings(dbAddresses);
    assertThat(roundTripModelAddresses).isEqualTo(sortedDistinctAddresses);
  }

  @Test
  public void test_modelToDb_null() {
    final Institution modelInst =
        new Institution()
            .shortName("Broad")
            .displayName("The Broad Institute")
            .emailAddresses(null);

    // does not need to match the modelInst; it is simply attached to the DbInstitutionEmailAddress
    final DbInstitution dbInst = new DbInstitution();

    final Set<DbInstitutionEmailAddress> dbAddresses = mapper.modelToDb(modelInst, dbInst);

    assertThat(dbAddresses).isNotNull();
    assertThat(dbAddresses).isEmpty();
  }

  @Test
  public void test_dbAddressesToStrings() {
    final DbInstitution dbInst =
        new DbInstitution().setShortName("Broad").setDisplayName("The Broad Institute");

    final Set<DbInstitutionEmailAddress> dbAddresses =
        Sets.newHashSet(
            new DbInstitutionEmailAddress()
                .setEmailAddress("joel@broad.org")
                .setInstitution(dbInst),
            new DbInstitutionEmailAddress()
                .setEmailAddress("eric@broad.org")
                .setInstitution(dbInst),
            new DbInstitutionEmailAddress()
                .setEmailAddress("joel@broad.org")
                .setInstitution(dbInst));

    // sorted and de-duplicated
    final SortedSet<String> expected =
        new TreeSet<>(Sets.newHashSet("eric@broad.org", "joel@broad.org"));

    final SortedSet<String> modelAddresses = mapper.dbAddressesToStrings(dbAddresses);
    assertThat(modelAddresses).isEqualTo(expected);

    // does not need to match dbInst: we only care about its emailAddresses
    final Institution modelInst =
        new Institution()
            .shortName("Whatever")
            .displayName("Whatever Tech")
            .emailAddresses(new ArrayList<>(modelAddresses));

    final Set<DbInstitutionEmailAddress> roundTripDbAddresses = mapper.modelToDb(modelInst, dbInst);
    assertThat(roundTripDbAddresses).isEqualTo(dbAddresses);
  }

  @Test
  public void test_dbAddressesToStrings_null() {
    final Set<String> modelAddresses = mapper.dbAddressesToStrings(null);
    assertThat(modelAddresses).isNotNull();
    assertThat(modelAddresses).isEmpty();
  }
}
