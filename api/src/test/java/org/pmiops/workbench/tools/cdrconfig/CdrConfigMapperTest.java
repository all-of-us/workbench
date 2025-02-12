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
  private CdrVersionConfig testVersionJson;

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

    boolean isDefault = true;
    boolean hasFitbitData = false;
    boolean hasFitbitSleepData = false;
    boolean hasSurveyConductData = false;
    boolean hasCopeSurveyData = true;
    boolean tanagraEnabled = true;

    testVersionJson =
        new CdrVersionConfig(
            20,
            isDefault,
            "CDR Version 20",
            testTierJson.shortName(),
            DbStorageEnums.archivalStatusToStorage(ArchivalStatus.LIVE),
            "a big one",
            "also a big one",
            Timestamp.from(Instant.now()),
            100,
            "data",
            "wgs1",
            "my_filter",
            hasFitbitData,
            hasCopeSurveyData,
            hasFitbitSleepData,
            null,
            hasSurveyConductData,
            tanagraEnabled,
            "20",
            "wgs/vcf/merged",
            "wgs/vcf/hail.mt",
            "wgs/cram/manifest.csv",
            "hail/mt",
            null,
            "microarray/vcf/manifest.csv",
            "microarray/idat/manifest.csv",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            20);
  }

  private DbAccessTier getExpectedTier() {
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
    assertThat(mapper.toDbTier(testTierJson)).isEqualTo(getExpectedTier());
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
        .containsExactly(getExpectedTier());
  }

  @Test
  public void test_toDbTierByShortName() {
    DbAccessTier registeredTier = accessTierDao.save(createRegisteredTier());
    assertThat(mapper.toDbTierByShortName(registeredTier.getShortName(), accessTierDao))
        .isEqualTo(registeredTier);
  }

  @Test
  public void test_toDbTierByShortName_missing() {
    accessTierDao.save(createRegisteredTier());
    assertThat(mapper.toDbTierByShortName("a tier which doesn't exist", accessTierDao)).isNull();
  }

  private DbCdrVersion getExpectedVersion(DbAccessTier tier, Timestamp creationTime) {
    return new DbCdrVersion()
        .setAccessTier(tier)
        .setCreationTime(creationTime)
        .setCdrVersionId(testVersionJson.cdrVersionId())
        .setIsDefault(testVersionJson.isDefault())
        .setName(testVersionJson.name())
        .setArchivalStatus(testVersionJson.archivalStatus())
        .setBigqueryProject(testVersionJson.bigqueryProject())
        .setBigqueryDataset(testVersionJson.bigqueryDataset())
        .setNumParticipants(testVersionJson.numParticipants())
        .setCdrDbName(testVersionJson.cdrDbName())
        .setWgsBigqueryDataset(testVersionJson.wgsBigqueryDataset())
        .setHasFitbitData(testVersionJson.hasFitbitData())
        .setHasFitbitSleepData(testVersionJson.hasFitbitSleepData())
        .setHasSurveyConductData(testVersionJson.hasSurveyConductData())
        .setTanagraEnabled(testVersionJson.tanagraEnabled())
        .setHasCopeSurveyData(testVersionJson.hasCopeSurveyData())
        .setWgsFilterSetName(testVersionJson.wgsFilterSetName())
        .setWgsVcfMergedStoragePath(testVersionJson.wgsVcfMergedStoragePath())
        .setWgsHailStoragePath(testVersionJson.wgsHailStoragePath())
        .setWgsCramManifestPath(testVersionJson.wgsCramManifestPath())
        .setStorageBasePath(testVersionJson.storageBasePath())
        .setMicroarrayHailStoragePath(testVersionJson.microarrayHailStoragePath())
        .setMicroarrayVcfManifestPath(testVersionJson.microarrayVcfManifestPath())
        .setMicroarrayIdatManifestPath(testVersionJson.microarrayIdatManifestPath())
        .setPublicReleaseNumber(testVersionJson.publicReleaseNumber());
  }

  @Test
  public void test_toDbVersion() {
    Timestamp commonCreationTime = Timestamp.from(Instant.now());
    DbAccessTier testTierInDb = accessTierDao.save(mapper.toDbTier(testTierJson));
    DbCdrVersion mapped =
        mapper.toDbVersion(testVersionJson, accessTierDao).setCreationTime(commonCreationTime);
    DbCdrVersion expected = getExpectedVersion(testTierInDb, commonCreationTime);
    assertThat(mapped).isEqualTo(expected);
  }

  @Test
  public void test_empty() {
    CdrConfigRecord cdrConfig =
        new CdrConfigRecord(Collections.emptyList(), Collections.emptyList());
    assertThat(mapper.accessTiers(cdrConfig)).isEmpty();
    assertThat(mapper.cdrVersions(cdrConfig, accessTierDao)).isEmpty();
  }
}
