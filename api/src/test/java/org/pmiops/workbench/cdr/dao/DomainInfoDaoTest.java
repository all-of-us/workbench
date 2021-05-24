package org.pmiops.workbench.cdr.dao;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.SpringTest;
import org.pmiops.workbench.cdr.model.DbDomainInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class DomainInfoDaoTest extends SpringTest {

  @Autowired private DomainInfoDao domainInfoDao;
  private DbDomainInfo domainInfoObservation;
  private DbDomainInfo domainInfoCondition;
  private DbDomainInfo domainInfoPM;

  @BeforeEach
  public void setUp() {
    domainInfoObservation =
        domainInfoDao.save(
            new DbDomainInfo()
                .conceptId(27L)
                .domain((short) 5)
                .domainId("Observation")
                .name("Observations")
                .description("descr")
                .allConceptCount(0)
                .standardConceptCount(0)
                .participantCount(0));
    domainInfoCondition =
        domainInfoDao.save(
            new DbDomainInfo()
                .conceptId(19L)
                .domain((short) 0)
                .domainId("Condition")
                .name("Conditions")
                .description("descr")
                .allConceptCount(0)
                .standardConceptCount(0)
                .participantCount(0));
    domainInfoPM =
        domainInfoDao.save(
            new DbDomainInfo()
                .conceptId(0L)
                .domain((short) 10)
                .domainId("Physical Measurements")
                .name("Physical Measurements")
                .description("descr")
                .allConceptCount(33)
                .standardConceptCount(0)
                .participantCount(453542));
  }

  @Test
  public void findByOrderByDomainId() {
    List<DbDomainInfo> infos = domainInfoDao.findByOrderByDomainId();
    assertThat(infos.size()).isEqualTo(3);
    assertThat(domainInfoCondition).isEqualTo(infos.get(0));
    assertThat(domainInfoObservation).isEqualTo(infos.get(1));
    assertThat(domainInfoPM).isEqualTo(infos.get(2));
  }
}
