package org.pmiops.workbench.reporting.insertion;

import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.bigquery.InsertAllRequest;
import com.google.cloud.bigquery.TableId;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.model.ReportingResearcher;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class InsertAllRequestBuilderTest {
  final InsertAllRequestBuilder<ReportingResearcher> researcherRequestBuilder =
      ResearcherParameter::values;

  final Map<String, Object> fixedValues =
      ImmutableMap.of("snapshot_timestamp", Instant.now().toEpochMilli());

  final List<ReportingResearcher> researchers =
      ImmutableList.of(
          new ReportingResearcher()
              .username("user1")
              .firstName("Onceler")
              .isDisabled(false)
              .researcherId(1L),
          new ReportingResearcher()
              .username(null)
              .firstName("Nullson")
              .isDisabled(false)
              .researcherId(111L),
          new ReportingResearcher()
              .username("america@usa.gov")
              .firstName(null)
              .isDisabled(false)
              .researcherId(212L),
          new ReportingResearcher()
              .username(null)
              .firstName(null)
              .isDisabled(true)
              .researcherId(313L));

  @Test
  public void build_tolerates_nulls() {
    InsertAllRequest request =
        researcherRequestBuilder.build(
            TableId.of("project ID", "dataset", "researcher"), researchers, fixedValues);

    assertThat(request.getRows().size()).isEqualTo(researchers.size());
  }
}
