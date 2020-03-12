package org.pmiops.workbench.elasticsearch;

import static com.google.common.truth.Truth.assertThat;
import static org.pmiops.workbench.elasticsearch.AggregationUtils.DATE;
import static org.pmiops.workbench.elasticsearch.AggregationUtils.GENDER_OR_SEX;
import static org.pmiops.workbench.elasticsearch.AggregationUtils.RACE;
import static org.pmiops.workbench.elasticsearch.AggregationUtils.RANGE_18_44;

import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.model.AgeType;
import org.pmiops.workbench.model.GenderOrSexType;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class AggregationUtilsTest {

  @Test
  public void buildDemoChartAggregationAgeGenderRace() {
    AggregationBuilder actualBuilder =
        AggregationUtils.buildDemoChartAggregation(
            GenderOrSexType.GENDER, AgeType.AGE, RANGE_18_44);
    assertThat(actualBuilder.getName()).isEqualTo(DATE + RANGE_18_44);
    assertThat(actualBuilder.getType()).isEqualTo("date_range");
    assertThat(actualBuilder.getSubAggregations().size()).isEqualTo(1);
    AggregationBuilder genderBuilder = actualBuilder.getSubAggregations().iterator().next();
    assertThat(genderBuilder.getName()).isEqualTo(GENDER_OR_SEX + RANGE_18_44);
    assertThat(genderBuilder.getType()).isEqualTo("terms");
    assertThat(genderBuilder.getSubAggregations().size()).isEqualTo(1);
    AggregationBuilder raceBuilder = genderBuilder.getSubAggregations().iterator().next();
    assertThat(raceBuilder.getName()).isEqualTo(RACE + RANGE_18_44);
    assertThat(raceBuilder.getType()).isEqualTo("terms");
    assertThat(raceBuilder.getSubAggregations().isEmpty()).isTrue();
  }

  @Test
  public void buildDemoChartAggregationAgeSexAtBirthRace() {
    AggregationBuilder actualBuilder =
        AggregationUtils.buildDemoChartAggregation(
            GenderOrSexType.SEX_AT_BIRTH, AgeType.AGE, RANGE_18_44);
    assertThat(actualBuilder.getName()).isEqualTo(DATE + RANGE_18_44);
    assertThat(actualBuilder.getType()).isEqualTo("date_range");
    assertThat(actualBuilder.getSubAggregations().size()).isEqualTo(1);
    AggregationBuilder genderBuilder = actualBuilder.getSubAggregations().iterator().next();
    assertThat(genderBuilder.getName()).isEqualTo(GENDER_OR_SEX + RANGE_18_44);
    assertThat(genderBuilder.getType()).isEqualTo("terms");
    assertThat(genderBuilder.getSubAggregations().size()).isEqualTo(1);
    AggregationBuilder raceBuilder = genderBuilder.getSubAggregations().iterator().next();
    assertThat(raceBuilder.getName()).isEqualTo(RACE + RANGE_18_44);
    assertThat(raceBuilder.getType()).isEqualTo("terms");
    assertThat(raceBuilder.getSubAggregations().isEmpty()).isTrue();
  }

  @Test
  public void buildDemoChartAggregationAgeAtConsentGenderRace() {
    AggregationBuilder actualBuilder =
        AggregationUtils.buildDemoChartAggregation(
            GenderOrSexType.GENDER, AgeType.AGE_AT_CONSENT, RANGE_18_44);
    assertThat(actualBuilder.getName()).isEqualTo(DATE + RANGE_18_44);
    assertThat(actualBuilder.getType()).isEqualTo("range");
    assertThat(actualBuilder.getSubAggregations().size()).isEqualTo(1);
    AggregationBuilder genderBuilder = actualBuilder.getSubAggregations().iterator().next();
    assertThat(genderBuilder.getName()).isEqualTo(GENDER_OR_SEX + RANGE_18_44);
    assertThat(genderBuilder.getType()).isEqualTo("terms");
    assertThat(genderBuilder.getSubAggregations().size()).isEqualTo(1);
    AggregationBuilder raceBuilder = genderBuilder.getSubAggregations().iterator().next();
    assertThat(raceBuilder.getName()).isEqualTo(RACE + RANGE_18_44);
    assertThat(raceBuilder.getType()).isEqualTo("terms");
    assertThat(raceBuilder.getSubAggregations().isEmpty()).isTrue();
  }
}
