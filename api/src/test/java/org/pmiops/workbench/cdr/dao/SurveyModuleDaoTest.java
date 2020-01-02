package org.pmiops.workbench.cdr.dao;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.model.DbSurveyModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class SurveyModuleDaoTest {

  @Autowired private SurveyModuleDao surveyModuleDao;
  private DbSurveyModule expectedDbSurveyModule;

  @Before
  public void setUp() {
    expectedDbSurveyModule =
        surveyModuleDao.save(
            new DbSurveyModule()
                .conceptId(1L)
                .description("descr1")
                .name("name1")
                .orderNumber(1)
                .participantCount(100)
                .questionCount(10));
    surveyModuleDao.save(
        new DbSurveyModule()
            .conceptId(2L)
            .description("descr2")
            .name("name2")
            .orderNumber(2)
            .participantCount(0)
            .questionCount(12));
  }

  @Test
  public void findByParticipantCountNotOrderByOrderNumberAsc() {
    List<DbSurveyModule> moduleList =
        surveyModuleDao.findByParticipantCountNotOrderByOrderNumberAsc(0L);
    assertThat(moduleList).hasSize(1);
    assertThat(moduleList.get(0)).isEqualTo(expectedDbSurveyModule);
  }
}
