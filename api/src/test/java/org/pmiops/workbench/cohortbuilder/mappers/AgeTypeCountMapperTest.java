package org.pmiops.workbench.cohortbuilder.mappers;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.cdr.model.DbAgeTypeCount;
import org.pmiops.workbench.model.AgeTypeCount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class AgeTypeCountMapperTest {

  @Autowired private AgeTypeCountMapper ageTypeCountMapper;

  @TestConfiguration
  @Import({AgeTypeCountMapperImpl.class})
  static class Configuration {}

  @Test
  public void dbModelToClient() {
    AgeTypeCount expectedAgeTypeCount = new AgeTypeCount().ageType("Age").age(22).count(2344L);
    assertThat(ageTypeCountMapper.dbModelToClient(new DbAgeTypeCountImpl("Age", 22, 2344L)))
        .isEqualTo(expectedAgeTypeCount);
  }

  class DbAgeTypeCountImpl implements DbAgeTypeCount {
    private String ageType;
    private int age;
    private long count;

    public DbAgeTypeCountImpl(String ageType, int age, long count) {
      this.ageType = ageType;
      this.age = age;
      this.count = count;
    }

    @Override
    public String getAgeType() {
      return ageType;
    }

    @Override
    public int getAge() {
      return age;
    }

    @Override
    public long getCount() {
      return count;
    }
  }
}
