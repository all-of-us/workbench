package org.pmiops.workbench.cdr.dao;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.cdr.model.DbDSLinking;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Import(FakeClockConfiguration.class)
public class DSLinkingDaoTest {

  @Autowired private DSLinkingDao dsLinkingDao;
  private DbDSLinking dbDSLinking1;
  private DbDSLinking dbDSLinking2;

  @BeforeEach
  public void setUp() {
    dbDSLinking1 =
        dsLinkingDao.save(
            DbDSLinking.builder()
                .addDenormalizedName("CONDITION_CONCEPT_ID")
                .addOmopSql("c_occurrence.CONDITION_CONCEPT_ID")
                .addJoinValue("from `${projectId}.${dataSetId}.condition_occurrence` c_occurrence")
                .addDomain("Condition")
                .build());
    dbDSLinking2 =
        dsLinkingDao.save(
            DbDSLinking.builder()
                .addDenormalizedName("CONDITION_STATUS_CONCEPT_NAME")
                .addOmopSql("c_status.concept_name as CONDITION_STATUS_CONCEPT_NAME")
                .addJoinValue(
                    "left join `${projectId}.${dataSetId}.concept` c_status on c_occurrence.CONDITION_STATUS_CONCEPT_ID = c_status.CONCEPT_ID")
                .addDomain("Condition")
                .build());
  }

  @Test
  public void findByDomainAndDenormalizedNameInOrderById() {
    List<DbDSLinking> sqlParts =
        dsLinkingDao.findByDomainAndDenormalizedNameInOrderById(
            "Condition", ImmutableList.of("CONDITION_CONCEPT_ID", "CONDITION_STATUS_CONCEPT_NAME"));
    assertThat(sqlParts).hasSize(2);
    assertThat(sqlParts).containsAtLeast(dbDSLinking1, dbDSLinking2);
  }
}
