package org.pmiops.workbench.cohortbuilder;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cohortbuilder.querybuilder.FactoryKey;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@Import({QueryBuilderFactory.class})
@ComponentScan(basePackages = "org.pmiops.workbench.cohortbuilder.*")
public class QueryBuilderFactoryTest {

  @Test
  public void getQueryBuilder() throws Exception {
    for (FactoryKey key : FactoryKey.values()) {
      assertEquals(key, QueryBuilderFactory.getQueryBuilder(key).getType());
    }
  }
}
