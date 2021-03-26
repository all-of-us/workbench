package org.pmiops.workbench.tools.cdrconfig;

import static com.google.common.truth.Truth.assertThat;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.SpringTest;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.model.ArchivalStatus;
import org.pmiops.workbench.utils.TestMockFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@Import(CdrConfigVOMapperImpl.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class CdrConfigVOMapperTest extends SpringTest {
  @Autowired CdrConfigVOMapper mapper;
  @Autowired AccessTierDao accessTierDao;

  private AccessTierVO testTierJson;
  private CdrVersionVO testVersionJson;

  @Before
  public void setup() {
    testTierJson = new AccessTierVO();
    testTierJson.accessTierId = 5;
    testTierJson.shortName = "tier5";
    testTierJson.displayName = "Tier Five";
    testTierJson.servicePerimeter = "tier/5/perim";
    testTierJson.authDomainName = "tier-5-users";
    testTierJson.authDomainGroupEmail = "tier-5-users@firecloud.org";

    testVersionJson = new CdrVersionVO();
    testVersionJson.cdrVersionId = 20;
    testVersionJson.isDefault = true;
    testVersionJson.name = "CDR Version 20";
    testVersionJson.accessTier = testTierJson.shortName;
    testVersionJson.releaseNumber = 2;
    testVersionJson.archivalStatus = DbStorageEnums.archivalStatusToStorage(ArchivalStatus.LIVE);
    testVersionJson.bigqueryProject = "a big one";
    testVersionJson.bigqueryDataset = "also a big one";
    testVersionJson.creationTime = Timestamp.from(Instant.now());
    testVersionJson.numParticipants = 100;
    testVersionJson.cdrDbName = "data";
    testVersionJson.elasticIndexBaseName = "elastic";
    testVersionJson.microarrayBigqueryDataset = "micro big";
    testVersionJson.wgsBigqueryDataset = "wgs1";
    testVersionJson.hasFitbitData = false;
    testVersionJson.hasCopeSurveyData = true;
  }

  @Test
  public void test_toDbTier() {
    DbAccessTier expected =
        new DbAccessTier()
            .setAccessTierId(testTierJson.accessTierId)
            .setShortName(testTierJson.shortName)
            .setDisplayName(testTierJson.displayName)
            .setServicePerimeter(testTierJson.servicePerimeter)
            .setAuthDomainName(testTierJson.authDomainName)
            .setAuthDomainGroupEmail(testTierJson.authDomainGroupEmail);

    assertThat(mapper.toDbTier(testTierJson)).isEqualTo(expected);
  }

  @Test
  public void test_toDbTiers_empty() {
    assertThat(mapper.toDbTiers(Collections.emptyList())).isEmpty();
  }

  @Test
  public void test_toDbTiers_no_unwanted() {
    // regression test: a bug was adding the existing tiers in the DB to this list

    DbAccessTier unwantedTier =
        accessTierDao.save(
            new DbAccessTier()
                .setAccessTierId(10)
                .setShortName("badTier")
                .setDisplayName("Don't want this")
                .setServicePerimeter("random/text")
                .setAuthDomainName("bad-dudes")
                .setAuthDomainGroupEmail("bad-dudes@arcade.net"));

    DbAccessTier expected =
        new DbAccessTier()
            .setAccessTierId(testTierJson.accessTierId)
            .setShortName(testTierJson.shortName)
            .setDisplayName(testTierJson.displayName)
            .setServicePerimeter(testTierJson.servicePerimeter)
            .setAuthDomainName(testTierJson.authDomainName)
            .setAuthDomainGroupEmail(testTierJson.authDomainGroupEmail);

    assertThat(mapper.toDbTiers(Collections.singletonList(testTierJson))).containsExactly(expected);
  }

  @Test
  public void test_populateAccessTier() {
    DbAccessTier registeredTier = TestMockFactory.createRegisteredTierForTests(accessTierDao);

    CdrVersionVO cdrVersionVO = new CdrVersionVO();
    cdrVersionVO.accessTier = registeredTier.getShortName();

    DbCdrVersion dbCdrVersion = new DbCdrVersion();
    dbCdrVersion.setCdrVersionId(3);

    assertThat(dbCdrVersion.getAccessTier()).isNull();
    mapper.populateAccessTier(cdrVersionVO, dbCdrVersion, accessTierDao);
    assertThat(dbCdrVersion.getAccessTier()).isEqualTo(registeredTier);
  }

  @Test
  public void test_populateAccessTier_missing() {
    DbAccessTier registeredTier = TestMockFactory.createRegisteredTierForTests(accessTierDao);

    CdrVersionVO cdrVersionVO = new CdrVersionVO();
    cdrVersionVO.accessTier = "a tier which doesn't exist";

    DbCdrVersion dbCdrVersion = new DbCdrVersion();
    dbCdrVersion.setCdrVersionId(3);

    assertThat(dbCdrVersion.getAccessTier()).isNull();
    mapper.populateAccessTier(cdrVersionVO, dbCdrVersion, accessTierDao);
    assertThat(dbCdrVersion.getAccessTier()).isNull();
  }

  @Test
  public void test_toDbVersion() {
    DbAccessTier testTierInDb = accessTierDao.save(mapper.toDbTier(testTierJson));

    DbCdrVersion expected = new DbCdrVersion();

    expected.setAccessTier(testTierInDb);

    expected.setCdrVersionId(testVersionJson.cdrVersionId);
    expected.setIsDefault(testVersionJson.isDefault);
    expected.setName(testVersionJson.name);
    expected.setReleaseNumber(testVersionJson.releaseNumber);
    expected.setArchivalStatus(testVersionJson.archivalStatus);
    expected.setBigqueryProject(testVersionJson.bigqueryProject);
    expected.setBigqueryDataset(testVersionJson.bigqueryDataset);
    expected.setCreationTime(testVersionJson.creationTime);
    expected.setNumParticipants(testVersionJson.numParticipants);
    expected.setCdrDbName(testVersionJson.cdrDbName);
    expected.setElasticIndexBaseName(testVersionJson.elasticIndexBaseName);
    expected.setMicroarrayBigqueryDataset(testVersionJson.microarrayBigqueryDataset);
    expected.setWgsBigqueryDataset(testVersionJson.wgsBigqueryDataset);
    expected.setHasFitbitData(testVersionJson.hasFitbitData);
    expected.setHasCopeSurveyData(testVersionJson.hasCopeSurveyData);

    assertThat(mapper.toDbVersion(testVersionJson, accessTierDao)).isEqualTo(expected);
  }

  @Test
  public void test_empty_VO() {
    CdrConfigVO cdrConfig = new CdrConfigVO();
    cdrConfig.accessTiers = new ArrayList<>();
    cdrConfig.cdrVersions = new ArrayList<>();

    assertThat(mapper.accessTiers(cdrConfig)).isEmpty();
    assertThat(mapper.cdrVersions(cdrConfig, accessTierDao)).isEmpty();
  }
}
