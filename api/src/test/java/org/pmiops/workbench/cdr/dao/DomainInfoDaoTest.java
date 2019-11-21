package org.pmiops.workbench.cdr.dao;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.model.DbConcept;
import org.pmiops.workbench.cdr.model.DbDomainInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
public class DomainInfoDaoTest {

  @Autowired private DomainInfoDao domainInfoDao;
  @Autowired private ConceptDao conceptDao;
  private DbDomainInfo domainInfoObservation;
  private DbDomainInfo domainInfoCondition;

  @Before
  public void setUp() {
    conceptDao.save(
        new DbConcept()
            .conceptId(1L)
            .domainId("Observation")
            .count(10L)
            .sourceCountValue(22L)
            .standardConcept("S")
            .conceptName("name"));
    conceptDao.save(
        new DbConcept()
            .conceptId(2L)
            .domainId("Condition")
            .count(100L)
            .sourceCountValue(122L)
            .standardConcept("S")
            .conceptName("name"));
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
  }

  @Test
  public void findByOrderByDomainId() {
    List<DbDomainInfo> infos = domainInfoDao.findByOrderByDomainId();
    assertEquals(2, infos.size());
    assertEquals(domainInfoCondition, infos.get(0));
    assertEquals(domainInfoObservation, infos.get(1));
  }

  @Test
  public void findStandardConceptCounts() {
    List<DbDomainInfo> infos = domainInfoDao.findStandardConceptCounts("name");
    assertEquals(2, infos.size());
    assertEquals(domainInfoCondition.standardConceptCount(1), infos.get(0));
    assertEquals(domainInfoObservation.standardConceptCount(1), infos.get(1));
  }

  @Test
  public void findAllMatchConceptCounts() {
    List<DbDomainInfo> infos = domainInfoDao.findAllMatchConceptCounts("name");
    assertEquals(2, infos.size());
    assertEquals(domainInfoCondition.allConceptCount(1), infos.get(0));
    assertEquals(domainInfoObservation.allConceptCount(1), infos.get(1));
  }
}
