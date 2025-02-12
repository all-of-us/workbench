package org.pmiops.workbench.tools.cdrconfig;

import static com.google.common.truth.Truth.assertThat;
import static org.pmiops.workbench.utils.TestMockFactory.createRegisteredTier;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.config.CommonConfig;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.model.ArchivalStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

@Import({CdrConfigMapperImpl.class, CommonConfig.class})
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class CdrConfigMapperTest {
  @Autowired CdrConfigMapper mapper;
  @Autowired AccessTierDao accessTierDao;

  private AccessTierConfig testTierJson;
  private CdrVersionVO testVersionJson;

  @BeforeEach
  public void setup() {
    boolean enableUserWorkflows = true;
    testTierJson =
        new AccessTierConfig(
            5,
            "tier5",
            "Tier Five",
            "tier/5/perim",
            "tier-5-users",
            "tier-5-users@firecloud.org",
            "ds-bucket",
            enableUserWorkflows,
            "vwb-tier-5-group");

    testVersionJson = new CdrVersionVO();
    testVersionJson.cdrVersionId = 20;
    testVersionJson.isDefault = true;
    testVersionJson.name = "CDR Version 20";
    testVersionJson.accessTier = testTierJson.shortName();
    testVersionJson.archivalStatus = DbStorageEnums.archivalStatusToStorage(ArchivalStatus.LIVE);
    testVersionJson.bigqueryProject = "a big one";
    testVersionJson.bigqueryDataset = "also a big one";
    testVersionJson.creationTime = Timestamp.from(Instant.now());
    testVersionJson.numParticipants = 100;
    testVersionJson.cdrDbName = "data";
    testVersionJson.wgsBigqueryDataset = "wgs1";
    testVersionJson.hasFitbitData = false;
    testVersionJson.hasFitbitSleepData = false;
    testVersionJson.hasSurveyConductData = false;
    testVersionJson.hasCopeSurveyData = true;
    testVersionJson.tanagraEnabled = true;
    testVersionJson.wgsFilterSetName = "my_filter";
    testVersionJson.storageBasePath = "20";
    testVersionJson.microarrayHailStoragePath = "hail/mt";
    testVersionJson.wgsVcfMergedStoragePath = "wgs/vcf/merged";
    testVersionJson.wgsHailStoragePath = "wgs/vcf/hail.mt";
    testVersionJson.wgsCramManifestPath = "wgs/cram/manifest.csv";
    testVersionJson.microarrayVcfManifestPath = "microarray/vcf/manifest.csv";
    testVersionJson.microarrayIdatManifestPath = "microarray/idat/manifest.csv";
  }

  private DbAccessTier getExpected() {
    return new DbAccessTier()
        .setAccessTierId(testTierJson.accessTierId())
        .setShortName(testTierJson.shortName())
        .setDisplayName(testTierJson.displayName())
        .setServicePerimeter(testTierJson.servicePerimeter())
        .setAuthDomainName(testTierJson.authDomainName())
        .setAuthDomainGroupEmail(testTierJson.authDomainGroupEmail())
        .setDatasetsBucket(testTierJson.datasetsBucket())
        .setEnableUserWorkflows(testTierJson.enableUserWorkflows())
        .setVwbTierGroupName(testTierJson.vwbTierGroupName());
  }

  @Test
  public void test_toDbTier() {
    assertThat(mapper.toDbTier(testTierJson)).isEqualTo(getExpected());
  }

  @Test
  public void test_toDbTiers_empty() {
    assertThat(mapper.toDbTiers(Collections.emptyList())).isEmpty();
  }

  @Test
  public void test_toDbTiers_no_unwanted() {
    // regression test: a bug was adding the existing tiers in the DB to this list

    // create an existing tier in DB that we are not updating
    accessTierDao.save(
        new DbAccessTier()
            .setAccessTierId(10)
            .setShortName("badTier")
            .setDisplayName("Don't want this")
            .setServicePerimeter("random/text")
            .setAuthDomainName("bad-dudes")
            .setAuthDomainGroupEmail("bad-dudes@arcade.net")
            .setEnableUserWorkflows(true));

    assertThat(mapper.toDbTiers(Collections.singletonList(testTierJson)))
        .containsExactly(getExpected());
  }

  @Test
  public void test_populateAccessTier() {
    DbAccessTier registeredTier = accessTierDao.save(createRegisteredTier());

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
    DbAccessTier registeredTier = accessTierDao.save(createRegisteredTier());

    CdrVersionVO cdrVersionVO = new CdrVersionVO();
    cdrVersionVO.accessTier = "a tier which doesn't exist";

    DbCdrVersion dbCdrVersion = new DbCdrVersion().setCdrVersionId(3);

    assertThat(dbCdrVersion.getAccessTier()).isNull();
    mapper.populateAccessTier(cdrVersionVO, dbCdrVersion, accessTierDao);
    assertThat(dbCdrVersion.getAccessTier()).isNull();
  }

  @Test
  public void test_toDbVersion() {
    DbAccessTier testTierInDb = accessTierDao.save(mapper.toDbTier(testTierJson));

    DbCdrVersion expected =
        new DbCdrVersion()
            .setAccessTier(testTierInDb)
            .setCdrVersionId(testVersionJson.cdrVersionId)
            .setIsDefault(testVersionJson.isDefault)
            .setName(testVersionJson.name)
            .setArchivalStatus(testVersionJson.archivalStatus)
            .setBigqueryProject(testVersionJson.bigqueryProject)
            .setBigqueryDataset(testVersionJson.bigqueryDataset)
            .setCreationTime(testVersionJson.creationTime)
            .setNumParticipants(testVersionJson.numParticipants)
            .setCdrDbName(testVersionJson.cdrDbName)
            .setWgsBigqueryDataset(testVersionJson.wgsBigqueryDataset)
            .setHasFitbitData(testVersionJson.hasFitbitData)
            .setHasFitbitSleepData(testVersionJson.hasFitbitSleepData)
            .setHasSurveyConductData(testVersionJson.hasSurveyConductData)
            .setTanagraEnabled(testVersionJson.tanagraEnabled)
            .setHasCopeSurveyData(testVersionJson.hasCopeSurveyData)
            .setWgsFilterSetName(testVersionJson.wgsFilterSetName)
            .setWgsVcfMergedStoragePath(testVersionJson.wgsVcfMergedStoragePath)
            .setWgsHailStoragePath(testVersionJson.wgsHailStoragePath)
            .setWgsCramManifestPath(testVersionJson.wgsCramManifestPath)
            .setStorageBasePath(testVersionJson.storageBasePath)
            .setMicroarrayHailStoragePath(testVersionJson.microarrayHailStoragePath)
            .setMicroarrayVcfManifestPath(testVersionJson.microarrayVcfManifestPath)
            .setMicroarrayIdatManifestPath(testVersionJson.microarrayIdatManifestPath);

    assertThat(mapper.toDbVersion(testVersionJson, accessTierDao)).isEqualTo(expected);
  }

  @Test
  public void test_empty() {
    CdrConfigRecord cdrConfig =
        new CdrConfigRecord(Collections.emptyList(), Collections.emptyList());
    assertThat(mapper.accessTiers(cdrConfig)).isEmpty();
    assertThat(mapper.cdrVersions(cdrConfig, accessTierDao)).isEmpty();
  }
}
