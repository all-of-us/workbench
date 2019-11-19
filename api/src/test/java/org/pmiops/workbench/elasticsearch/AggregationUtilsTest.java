package org.pmiops.workbench.elasticsearch;

import static com.google.common.truth.Truth.assertThat;
import static org.pmiops.workbench.elasticsearch.AggregationUtils.DATE;
import static org.pmiops.workbench.elasticsearch.AggregationUtils.GENDER;
import static org.pmiops.workbench.elasticsearch.AggregationUtils.RACE;
import static org.pmiops.workbench.elasticsearch.AggregationUtils.RANGE_18_44;

import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class AggregationUtilsTest {

  @Test
  public void buildDemoChartAggregation() throws Exception {
    AggregationBuilder actualBuilder = AggregationUtils.buildDemoChartAggregation(RANGE_18_44);
    assertThat(actualBuilder.getName()).isEqualTo(DATE + RANGE_18_44);
    assertThat(actualBuilder.getType()).isEqualTo("date_range");
    assertThat(actualBuilder.getSubAggregations().size()).isEqualTo(1);
    AggregationBuilder genderBuilder = actualBuilder.getSubAggregations().iterator().next();
    assertThat(genderBuilder.getName()).isEqualTo(GENDER + RANGE_18_44);
    assertThat(genderBuilder.getType()).isEqualTo("terms");
    assertThat(genderBuilder.getSubAggregations().size()).isEqualTo(1);
    AggregationBuilder raceBuilder = genderBuilder.getSubAggregations().iterator().next();
    assertThat(raceBuilder.getName()).isEqualTo(RACE + RANGE_18_44);
    assertThat(raceBuilder.getType()).isEqualTo("terms");
    assertThat(raceBuilder.getSubAggregations().isEmpty()).isTrue();
  }
}
