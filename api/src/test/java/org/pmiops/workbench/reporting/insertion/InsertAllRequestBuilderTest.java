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
import org.pmiops.workbench.model.BqDtoUser;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class InsertAllRequestBuilderTest {
  final InsertAllRequestBuilder<BqDtoUser> researcherRequestBuilder =
      UserParameter::values;

  final Instant princePartyTime = Instant.parse("1999-12-31T23:59:59.99Z");
  final Map<String, Object> fixedValues =
      ImmutableMap.of("snapshot_timestamp", princePartyTime.toEpochMilli());

  final List<BqDtoUser> researchers =
      ImmutableList.of(
          new BqDtoUser()
              .username("user1")
              .givenName("Onceler")
              .disabled(false)
              .userId(1L),
          new BqDtoUser()
              .username(null)
              .givenName("Nullson")
              .disabled(false)
              .userId(111L),
          new BqDtoUser()
              .username("america@usa.gov")
              .givenName(null)
              .disabled(false)
              .userId(212L),
          new BqDtoUser()
              .username(null)
              .givenName(null)
              .disabled(true)
              .userId(313L));

  // regression test for RW-5437
  @Test
  public void build_tolerates_nulls() {
    InsertAllRequest request =
        researcherRequestBuilder.build(
            TableId.of("project ID", "dataset", "researcher"), researchers, fixedValues);

    assertThat(request.getRows().size()).isEqualTo(researchers.size());
  }
}
