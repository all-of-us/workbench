package org.pmiops.workbench.institution;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.SpringTest;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionEmailAddress;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.model.TierEmailAddresses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

@Import(InstitutionEmailAddressMapperImpl.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class InstitutionEmailAddressMapperTest extends SpringTest {
  private static final String RT_SHORT_NAME = "REGISTERED";
  private static final String CT_SHORT_NAME = "CONTROLLED";
  private static final DbAccessTier REGISTERED_ACCESS_TIER =
      new DbAccessTier().setShortName(RT_SHORT_NAME);
  private static final DbAccessTier CONTROLLED_ACCESS_TIER =
      new DbAccessTier().setShortName(CT_SHORT_NAME);

  @Autowired InstitutionEmailAddressMapper mapper;

  @Test
  public void test_modelToDb() {
    // contains an out-of-order duplicate
    final List<String> rtRawAddresses =
        Lists.newArrayList("alice@nih.gov", "joel@other-inst.org", "alice@nih.gov");
    final List<String> ctRawAddresses = Lists.newArrayList("foo@nih.gov");
    final SortedSet<String> sortedRtDistinctAddresses = new TreeSet<>(rtRawAddresses);
    final SortedSet<String> sortedCtDistinctAddresses = new TreeSet<>(ctRawAddresses);
    TierEmailAddresses rtEmailAddresses =
        new TierEmailAddresses()
            .emailAddresses(new ArrayList<>(sortedRtDistinctAddresses))
            .accessTierShortName(RT_SHORT_NAME);
    TierEmailAddresses ctEmailAddresses =
        new TierEmailAddresses()
            .emailAddresses(new ArrayList<>(sortedCtDistinctAddresses))
            .accessTierShortName(CT_SHORT_NAME);

    final Institution modelInst =
        new Institution()
            .shortName("Broad")
            .displayName("The Broad Institute")
            .tierEmailAddresses(ImmutableList.of(rtEmailAddresses, ctEmailAddresses));

    // does not need to match the modelInst; it is simply attached to the DbInstitutionEmailAddress
    final DbInstitution dbInst = new DbInstitution();

    final Set<DbInstitutionEmailAddress> rtDbAddresses =
        mapper.modelToDb(modelInst, dbInst, REGISTERED_ACCESS_TIER);
    final Set<DbInstitutionEmailAddress> ctDbAddresses =
        mapper.modelToDb(modelInst, dbInst, CONTROLLED_ACCESS_TIER);

    assertThat(rtDbAddresses).hasSize(sortedRtDistinctAddresses.size());
    assertThat(ctDbAddresses).hasSize(sortedCtDistinctAddresses.size());

    for (final DbInstitutionEmailAddress dbAddress : rtDbAddresses) {
      assertThat(sortedRtDistinctAddresses).contains(dbAddress.getEmailAddress());
      assertThat(dbAddress.getInstitution()).isEqualTo(dbInst);
      assertThat(dbAddress.getAccessTier()).isEqualTo(REGISTERED_ACCESS_TIER);
    }
    for (final DbInstitutionEmailAddress dbAddress : ctDbAddresses) {
      assertThat(sortedCtDistinctAddresses).contains(dbAddress.getEmailAddress());
      assertThat(dbAddress.getInstitution()).isEqualTo(dbInst);
      assertThat(dbAddress.getAccessTier()).isEqualTo(CONTROLLED_ACCESS_TIER);
    }

    final List<TierEmailAddresses> roundTripModelAddresses =
        mapper.dbAddressesToTierEmailAddresses(Sets.union(rtDbAddresses, ctDbAddresses));
    assertThat(roundTripModelAddresses).containsExactly(rtEmailAddresses, ctEmailAddresses);
  }

  @Test
  public void test_modelToDb_tierNotFound() {
    TierEmailAddresses rtEmailAddresses =
        new TierEmailAddresses()
            .emailAddresses(ImmutableList.of())
            .accessTierShortName(RT_SHORT_NAME);
    final Institution modelInst =
        new Institution()
            .shortName("Broad")
            .displayName("The Broad Institute")
            .tierEmailAddresses(ImmutableList.of(rtEmailAddresses));

    // does not need to match the modelInst; it is simply attached to the DbInstitutionEmailAddress
    final DbInstitution dbInst = new DbInstitution();
    final Set<DbInstitutionEmailAddress> dbAddresses =
        mapper.modelToDb(modelInst, dbInst, REGISTERED_ACCESS_TIER);

    assertThat(dbAddresses).isNotNull();
    assertThat(dbAddresses).isEmpty();
  }

  @Test
  public void test_modelToDb_null() {
    final Institution modelInst =
        new Institution()
            .shortName("Broad")
            .displayName("The Broad Institute")
            .tierEmailAddresses(null);

    // does not need to match the modelInst; it is simply attached to the DbInstitutionEmailAddress
    final DbInstitution dbInst = new DbInstitution();
    final Set<DbInstitutionEmailAddress> dbAddresses =
        mapper.modelToDb(modelInst, dbInst, REGISTERED_ACCESS_TIER);

    assertThat(dbAddresses).isNotNull();
    assertThat(dbAddresses).isEmpty();
  }

  @Test
  public void test_modelToDb_empty() {
    final Institution modelInst =
        new Institution()
            .shortName("Broad")
            .displayName("The Broad Institute")
            .tierEmailAddresses(new ArrayList<>());

    // does not need to match the modelInst; it is simply attached to the DbInstitutionEmailAddress
    final DbInstitution dbInst = new DbInstitution();

    final Set<DbInstitutionEmailAddress> dbAddresses =
        mapper.modelToDb(modelInst, dbInst, REGISTERED_ACCESS_TIER);

    assertThat(dbAddresses).isNotNull();
    assertThat(dbAddresses).isEmpty();
  }

  @Test
  public void test_dbAddressesToModel() {
    final DbInstitution dbInst =
        new DbInstitution().setShortName("Broad").setDisplayName("The Broad Institute");

    final Set<DbInstitutionEmailAddress> dbAddresses =
        Sets.newHashSet(
            new DbInstitutionEmailAddress()
                .setEmailAddress("joel@broad.org")
                .setInstitution(dbInst)
                .setAccessTier(REGISTERED_ACCESS_TIER),
            new DbInstitutionEmailAddress()
                .setEmailAddress("eric@broad.org")
                .setInstitution(dbInst)
                .setAccessTier(REGISTERED_ACCESS_TIER),
            new DbInstitutionEmailAddress()
                .setEmailAddress("joel@broad.org")
                .setInstitution(dbInst)
                .setAccessTier(REGISTERED_ACCESS_TIER),
            new DbInstitutionEmailAddress()
                .setEmailAddress("foo@broad.org")
                .setInstitution(dbInst)
                .setAccessTier(CONTROLLED_ACCESS_TIER),
            new DbInstitutionEmailAddress()
                .setEmailAddress("foo@broad.org")
                .setInstitution(dbInst)
                .setAccessTier(CONTROLLED_ACCESS_TIER));

    final List<String> expectedRt = ImmutableList.of("eric@broad.org", "joel@broad.org");
    final List<String> expectedCt = ImmutableList.of("foo@broad.org");

    final List<TierEmailAddresses> modelTierAddresses =
        mapper.dbAddressesToTierEmailAddresses(dbAddresses);
    TierEmailAddresses expectedRtTierEmailAddress =
        new TierEmailAddresses().accessTierShortName(RT_SHORT_NAME).emailAddresses(expectedRt);
    TierEmailAddresses expectedCtTierEmailAddress =
        new TierEmailAddresses().accessTierShortName(CT_SHORT_NAME).emailAddresses(expectedCt);
    assertThat(modelTierAddresses)
        .containsExactly(expectedRtTierEmailAddress, expectedCtTierEmailAddress);

    // does not need to match dbInst: we only care about its emailAddresses
    final Institution modelInst =
        new Institution()
            .shortName("Whatever")
            .displayName("Whatever Tech")
            .tierEmailAddresses(
                ImmutableList.of(expectedRtTierEmailAddress, expectedCtTierEmailAddress));

    final Set<DbInstitutionEmailAddress> roundTripRtDbAddresses =
        mapper.modelToDb(modelInst, dbInst, REGISTERED_ACCESS_TIER);
    final Set<DbInstitutionEmailAddress> roundTripCtDbAddresses =
        mapper.modelToDb(modelInst, dbInst, CONTROLLED_ACCESS_TIER);
    assertThat(Sets.union(roundTripRtDbAddresses, roundTripCtDbAddresses)).isEqualTo(dbAddresses);
  }

  @Test
  public void test_dbAddressesToStrings_empty() {
    final List<TierEmailAddresses> modelAddresses =
        mapper.dbAddressesToTierEmailAddresses(new HashSet<>());
    assertThat(modelAddresses).isNotNull();
    assertThat(modelAddresses).isEmpty();
  }
}
