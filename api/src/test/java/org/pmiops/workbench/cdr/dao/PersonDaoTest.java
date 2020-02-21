package org.pmiops.workbench.cdr.dao;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.model.DbPerson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class PersonDaoTest {

  @Autowired private PersonDao personDao;

  @Before
  public void setUp() {
    personDao.save(DbPerson.builder().addAgeAtConsent(55).addAgeAtCdr(56).build());
    personDao.save(DbPerson.builder().addAgeAtConsent(22).addAgeAtCdr(22).build());
    personDao.save(DbPerson.builder().addAgeAtConsent(34).addAgeAtCdr(35).build());
  }

  @Test
  public void countByAgeAtConsentBetween() {
    assertThat(personDao.countByAgeAtConsentBetween(22, 22)).isEqualTo(1);
    assertThat(personDao.countByAgeAtConsentBetween(22, 34)).isEqualTo(2);
    assertThat(personDao.countByAgeAtConsentBetween(22, 55)).isEqualTo(3);
    assertThat(personDao.countByAgeAtConsentBetween(60, 77)).isEqualTo(0);
  }

  @Test
  public void countByAgeAtCdrBetween() {
    assertThat(personDao.countByAgeAtCdrBetween(22, 22)).isEqualTo(1);
    assertThat(personDao.countByAgeAtCdrBetween(22, 35)).isEqualTo(2);
    assertThat(personDao.countByAgeAtCdrBetween(22, 56)).isEqualTo(3);
    assertThat(personDao.countByAgeAtCdrBetween(60, 77)).isEqualTo(0);
  }
}
