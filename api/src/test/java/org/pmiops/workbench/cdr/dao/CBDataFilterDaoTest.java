package org.pmiops.workbench.cdr.dao;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.model.DbDataFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class CBDataFilterDaoTest {

  private DbDataFilter dataFilter;

  @Autowired private CBDataFilterDao cbDataFilterDao;

  @Before
  public void setUp() {
    dataFilter =
        cbDataFilterDao.save(
            DbDataFilter.builder().addDisplayName("displayName").addName("name").build());
  }

  @Test
  public void findAll() {
    List<DbDataFilter> filters =
        StreamSupport.stream(cbDataFilterDao.findAll().spliterator(), false)
            .collect(Collectors.toList());
    assertThat(filters).hasSize(1);
    assertThat(filters.get(0)).isEqualTo(dataFilter);
  }
}
