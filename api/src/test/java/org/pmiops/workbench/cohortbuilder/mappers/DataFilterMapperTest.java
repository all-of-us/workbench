package org.pmiops.workbench.cohortbuilder.mappers;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.model.DbDataFilter;
import org.pmiops.workbench.model.DataFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class DataFilterMapperTest {

  @Autowired private DataFilterMapper dataFilterMapper;

  @TestConfiguration
  @Import({DataFilterMapperImpl.class})
  static class Configuration {}

  @Test
  public void dbModelToClient() {
    DataFilter expectedDataFilter =
        new DataFilter().dataFilterId(1L).displayName("displayName").name("name");
    assertThat(
            dataFilterMapper.dbModelToClient(
                DbDataFilter.builder()
                    .addDataFilterId(1L)
                    .addDisplayName("displayName")
                    .addName("name")
                    .build()))
        .isEqualTo(expectedDataFilter);
  }
}
