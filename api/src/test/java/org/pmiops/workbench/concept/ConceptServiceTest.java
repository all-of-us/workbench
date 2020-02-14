package org.pmiops.workbench.concept;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.dao.CBCriteriaDao;
import org.pmiops.workbench.cdr.dao.ConceptDao;
import org.pmiops.workbench.cdr.dao.DomainInfoDao;
import org.pmiops.workbench.cdr.dao.SurveyModuleDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@Import({ConceptService.class})
@MockBean({ConceptDao.class, DomainInfoDao.class, SurveyModuleDao.class, CBCriteriaDao.class})
public class ConceptServiceTest {

  @Autowired private ConceptService conceptService;

  @Before
  public void setUp() {}

  @Test
  public void modifyMultipleMatchKeyword() {
    assertThat(conceptService.modifyMultipleMatchKeyword("brian")).isEqualTo("+brian");
    assertThat(conceptService.modifyMultipleMatchKeyword("brian free")).isEqualTo("+brian+free");
    assertThat(conceptService.modifyMultipleMatchKeyword("001")).isEqualTo("+001");
    assertThat(conceptService.modifyMultipleMatchKeyword("001.1")).isEqualTo("+\"001.1\"");
    assertThat(conceptService.modifyMultipleMatchKeyword("001*")).isEqualTo("+001*");
    assertThat(conceptService.modifyMultipleMatchKeyword("lun* can*")).isEqualTo("+lun*+can*");
    assertThat(conceptService.modifyMultipleMatchKeyword("lun* -can")).isEqualTo("+lun*-can");
    assertThat(conceptService.modifyMultipleMatchKeyword("at base ball hill"))
        .isEqualTo("at+base+ball+hill");
  }
}
