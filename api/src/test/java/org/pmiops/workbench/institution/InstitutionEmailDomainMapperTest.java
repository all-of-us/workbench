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
import org.pmiops.workbench.db.model.DbInstitutionEmailDomain;
import org.pmiops.workbench.model.Institution;
import org.pmiops.workbench.model.TierEmailDomains;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

@Import(InstitutionEmailDomainMapperImpl.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class InstitutionEmailDomainMapperTest extends SpringTest {
  private static final String RT_SHORT_NAME = "REGISTERED";
  private static final String CT_SHORT_NAME = "CONTROLLED";
  private static final DbAccessTier REGISTERED_ACCESS_TIER =
      new DbAccessTier().setShortName(RT_SHORT_NAME);
  private static final DbAccessTier CONTROLLED_ACCESS_TIER =
      new DbAccessTier().setShortName(CT_SHORT_NAME);

  @Autowired InstitutionEmailDomainMapper mapper;

  @Test
  public void test_modelToDb_thenModelToDb() {
    // contains an out-of-order duplicate
    final List<String> rtRawDomains = Lists.newArrayList("nih.gov", "other-inst.org", "nih.gov");
    final List<String> ctRawDomains = Lists.newArrayList("ct.gov");
    final SortedSet<String> sortedRtDistinctDomains = new TreeSet<>(rtRawDomains);
    final SortedSet<String> sortedCtDistinctDomains = new TreeSet<>(ctRawDomains);

    TierEmailDomains rtEmailDomains =
        new TierEmailDomains()
            .emailDomains(new ArrayList<>(sortedRtDistinctDomains))
            .accessTierShortName(RT_SHORT_NAME);
    TierEmailDomains ctEmailDomains =
        new TierEmailDomains()
            .emailDomains(new ArrayList<>(sortedCtDistinctDomains))
            .accessTierShortName(CT_SHORT_NAME);

    final Institution modelInst =
        new Institution()
            .shortName("Broad")
            .displayName("The Broad Institute")
            .tierEmailDomains(ImmutableList.of(rtEmailDomains, ctEmailDomains));

    // does not need to match the modelInst; it is simply attached to the DbInstitutionEmailDomain
    final DbInstitution dbInst = new DbInstitution();

    final Set<DbInstitutionEmailDomain> rtDbDomains =
        mapper.modelToDb(modelInst, dbInst, REGISTERED_ACCESS_TIER);
    final Set<DbInstitutionEmailDomain> ctDbDomains =
        mapper.modelToDb(modelInst, dbInst, CONTROLLED_ACCESS_TIER);

    assertThat(rtDbDomains).hasSize(sortedRtDistinctDomains.size());
    assertThat(ctDbDomains).hasSize(sortedCtDistinctDomains.size());

    for (final DbInstitutionEmailDomain dbDomain : rtDbDomains) {
      assertThat(sortedRtDistinctDomains).contains(dbDomain.getEmailDomain());
      assertThat(dbDomain.getInstitution()).isEqualTo(dbInst);
    }
    for (final DbInstitutionEmailDomain dbDomain : ctDbDomains) {
      assertThat(sortedCtDistinctDomains).contains(dbDomain.getEmailDomain());
      assertThat(dbDomain.getInstitution()).isEqualTo(dbInst);
    }

    final List<TierEmailDomains> roundTripModelDomains =
        mapper.dbDomainsToTierEmailDomains(Sets.union(rtDbDomains, ctDbDomains));
    assertThat(roundTripModelDomains).containsExactly(rtEmailDomains, ctEmailDomains);
  }

  @Test
  public void test_modelToDb_tierNotFound() {
    // The target domains to convert is CT's tier, while the model only contain RT email domains.
    // Expect to get empty CT domains list.
    TierEmailDomains rtEmailDomains =
        new TierEmailDomains().emailDomains(ImmutableList.of()).accessTierShortName(RT_SHORT_NAME);
    final Institution modelInst =
        new Institution()
            .shortName("Broad")
            .displayName("The Broad Institute")
            .tierEmailDomains(ImmutableList.of(rtEmailDomains));

    // does not need to match the modelInst; it is simply attached to the DbInstitutionEmailDomain
    final DbInstitution dbInst = new DbInstitution();
    final Set<DbInstitutionEmailDomain> dbDomains =
        mapper.modelToDb(modelInst, dbInst, CONTROLLED_ACCESS_TIER);

    assertThat(dbDomains).isNotNull();
    assertThat(dbDomains).isEmpty();
  }

  @Test
  public void test_modelToDb_null() {
    final Institution modelInst =
        new Institution()
            .shortName("Broad")
            .displayName("The Broad Institute")
            .tierEmailDomains(null);

    // does not need to match the modelInst; it is simply attached to the DbInstitutionEmailDomain
    final DbInstitution dbInst = new DbInstitution();
    final Set<DbInstitutionEmailDomain> dbDomains =
        mapper.modelToDb(modelInst, dbInst, REGISTERED_ACCESS_TIER);

    assertThat(dbDomains).isNotNull();
    assertThat(dbDomains).isEmpty();
  }

  @Test
  public void test_modelToDb_empty() {
    final Institution modelInst =
        new Institution()
            .shortName("Broad")
            .displayName("The Broad Institute")
            .tierEmailDomains(new ArrayList<>());

    // does not need to match the modelInst; it is simply attached to the DbInstitutionEmailDomain
    final DbInstitution dbInst = new DbInstitution();
    final Set<DbInstitutionEmailDomain> dbDomains =
        mapper.modelToDb(modelInst, dbInst, REGISTERED_ACCESS_TIER);

    assertThat(dbDomains).isNotNull();
    assertThat(dbDomains).isEmpty();
  }

  @Test
  public void test_dbDomainsToModel_empty() {
    final List<TierEmailDomains> modelDomains = mapper.dbDomainsToTierEmailDomains(new HashSet<>());
    assertThat(modelDomains).isNotNull();
    assertThat(modelDomains).isEmpty();
  }
}
