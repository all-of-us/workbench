package org.pmiops.workbench.institution;

import static com.google.common.truth.Truth.assertThat;

import com.google.appengine.repackaged.com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.model.DbInstitution;
import org.pmiops.workbench.db.model.DbInstitutionEmailAddress;
import org.pmiops.workbench.db.model.DbInstitutionEmailDomain;
import org.pmiops.workbench.model.Institution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@Import(InstitutionMapperImpl.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class InstitutionMapperTest {
  @Autowired InstitutionMapper mapper;

  private List<String> modelDomains;
  private List<String> modelAddresses;
  private Set<DbInstitutionEmailDomain> dbDomains;
  private Set<DbInstitutionEmailAddress> dbAddresses;

  @Before
  public void setup() {
    modelDomains = Lists.newArrayList("broad.org", "broadinst.org");
    modelAddresses = Lists.newArrayList("joel@other-univ.org");

    // note: these do NOT have their institutions set
    // so they will match the withoutInstitution tests

    dbDomains =
        Sets.newHashSet(
            new DbInstitutionEmailDomain().setEmailDomain("broad.org"),
            new DbInstitutionEmailDomain().setEmailDomain("broadinst.org"));

    dbAddresses =
        Sets.newHashSet(new DbInstitutionEmailAddress().setEmailAddress("joel@other-univ.org"));
  }

  @Test
  public void test_toModelDomains() {
    final List<String> testModelDomains = mapper.toModelDomains(dbDomains);
    assertThat(testModelDomains).containsExactlyElementsIn(modelDomains);

    // round trip
    assertThat(mapper.toDbDomainsWithoutInstitution(testModelDomains)).isEqualTo(dbDomains);
  }

  @Test
  public void test_toDbDomainsWithoutInstitution() {
    final Set<DbInstitutionEmailDomain> testDbDomains =
        mapper.toDbDomainsWithoutInstitution(modelDomains);
    assertThat(testDbDomains).containsExactlyElementsIn(dbDomains);

    // round trip
    assertThat(mapper.toModelDomains(testDbDomains)).isEqualTo(modelDomains);
  }

  @Test
  public void test_toDbDomainsWithoutInstitution_null() {
    assertThat(mapper.toDbDomainsWithoutInstitution(null)).isEmpty();
  }

  @Test
  public void test_toModelAddresses() {
    final List<String> testModelAddresses = mapper.toModelAddresses(dbAddresses);
    assertThat(testModelAddresses).containsExactlyElementsIn(modelAddresses);

    // round trip
    assertThat(mapper.toDbAddressesWithoutInstitution(testModelAddresses)).isEqualTo(dbAddresses);
  }

  @Test
  public void test_toDbAddressesWithoutInstitution() {
    final Set<DbInstitutionEmailAddress> testDbAddresses =
        mapper.toDbAddressesWithoutInstitution(modelAddresses);
    assertThat(testDbAddresses).containsExactlyElementsIn(dbAddresses);

    // round trip
    assertThat(mapper.toModelAddresses(testDbAddresses)).isEqualTo(modelAddresses);
  }

  @Test
  public void test_toDbAddressesWithoutInstitution_null() {
    assertThat(mapper.toDbAddressesWithoutInstitution(null)).isEmpty();
  }

  @Test
  public void test_modelToDb() {
    final Institution inst =
        new Institution()
            .shortName("Test")
            .displayName("Test State University")
            .emailDomains(modelDomains)
            .emailAddresses(modelAddresses);

    final DbInstitution dbInst = mapper.modelToDb(inst);

    assertThat(dbInst.getShortName()).isEqualTo(inst.getShortName());
    assertThat(dbInst.getDisplayName()).isEqualTo(inst.getDisplayName());

    // cannot assert that dbInst.getEmailDomains() matches dbDomains
    // because the dbInst DbInstitutionEmailDomains are now associated with dbInst
    // and the original dbDomains are not

    // likewise for addresses

    final Institution roundTrip = mapper.dbToModel(dbInst);

    assertThat(roundTrip.getShortName()).isEqualTo(inst.getShortName());
    assertThat(roundTrip.getDisplayName()).isEqualTo(inst.getDisplayName());
    assertThat(roundTrip.getEmailDomains()).containsExactlyElementsIn(inst.getEmailDomains());
    assertThat(roundTrip.getEmailAddresses()).containsExactlyElementsIn(inst.getEmailAddresses());
  }

  @Test
  public void test_dbToModel() {
    final DbInstitution dbInst =
        new DbInstitution()
            .setShortName("Test")
            .setDisplayName("Test State University")
            .setEmailDomains(dbDomains)
            .setEmailAddresses(dbAddresses);

    final Institution inst = mapper.dbToModel(dbInst);

    assertThat(inst.getShortName()).isEqualTo(dbInst.getShortName());
    assertThat(inst.getDisplayName()).isEqualTo(dbInst.getDisplayName());
    assertThat(inst.getEmailDomains()).containsExactlyElementsIn(modelDomains);
    assertThat(inst.getEmailAddresses()).containsExactlyElementsIn(modelAddresses);

    final DbInstitution roundTrip = mapper.modelToDb(inst);

    assertThat(roundTrip.getShortName()).isEqualTo(dbInst.getShortName());
    assertThat(roundTrip.getDisplayName()).isEqualTo(dbInst.getDisplayName());

    // cannot assert that roundTrip.getEmailDomains() matches dbInst.getEmailDomains()
    // because the dbInst DbInstitutionEmailDomains are associated with dbInst
    // and the roundTrip DbInstitutionEmailDomains are associated with roundTrip

    // likewise for addresses
  }
}
