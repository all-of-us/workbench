package org.pmiops.workbench.cohortbuilder;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.pmiops.workbench.cdr.dao.PersonDao;
import org.pmiops.workbench.model.AgeType;

@RunWith(MockitoJUnitRunner.class)
public class CohortBuilderServiceTest {

  @Mock private PersonDao personDao;
  private CohortBuilderService cohortBuilderService;

  @Before
  public void setUp() {
    cohortBuilderService = new CohortBuilderServiceImpl(personDao);
  }

  @Test
  public void countAgesByType() {
    final long count = 1;
    final AgeType ageType = AgeType.AGE;
    final int startAge = 20;
    final int endAge = 40;
    when(personDao.countAgesByType(ageType, startAge, endAge)).thenReturn(count);
    assertThat(cohortBuilderService.countAgesByType(ageType, startAge, endAge)).isEqualTo(count);
    verify(personDao, times(1)).countAgesByType(ageType, startAge, endAge);
  }
}
