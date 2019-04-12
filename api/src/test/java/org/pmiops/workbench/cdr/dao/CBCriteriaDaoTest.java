package org.pmiops.workbench.cdr.dao;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.model.CBCriteria;
import org.pmiops.workbench.model.CriteriaType;
import org.pmiops.workbench.model.DomainType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@DataJpaTest
@Import(LiquibaseAutoConfiguration.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
public class CBCriteriaDaoTest {

  @Autowired
  private CBCriteriaDao cbCriteriaDao;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Before
  public void setUp() {
    jdbcTemplate.execute("delete from criteria");
  }

  @Test
  public void findCriteriaByDomainAndSearchTerm() throws Exception {
    //match on code
    CBCriteria criteria = new CBCriteria()
      .code("001")
      .count("10")
      .conceptId("123")
      .domainId(DomainType.MEASUREMENT.toString())
      .group(true)
      .selectable(true)
      .name("chol blah")
      .parentId(0)
      .type(CriteriaType.CPT4.toString())
      .attribute(Boolean.FALSE)
      .standard(true)
      .synonyms("001");
    cbCriteriaDao.save(criteria);
    List<CBCriteria> measurements =
      cbCriteriaDao.findCriteriaByDomainAndSearchTerm(DomainType.MEASUREMENT.toString(), true,"001", new PageRequest(0, 10));
    assertEquals(1, measurements.size());
    assertEquals(criteria, measurements.get(0));
  }

}
