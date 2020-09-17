package org.pmiops.workbench.cohortbuilder.util;

import static com.google.common.truth.Truth.assertThat;
import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.buildParameter;
import static org.pmiops.workbench.cohortbuilder.util.QueryParameterValues.decorateParameterName;

import com.google.cloud.bigquery.QueryParameterValue;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class QueryParameterValuesTest {

  private static final Map<String, QueryParameterValue> PARAM_MAP = new HashMap<>();

  @Before
  public void setup() {
    PARAM_MAP.put("foo", QueryParameterValue.int64(99));
    PARAM_MAP.put("bar", QueryParameterValue.string("hooray"));
  }

  @After
  public void tearDown() {
    PARAM_MAP.clear();
  }

  @Test
  public void testDecorateParameterName() {
    assertThat(decorateParameterName("foo")).isEqualTo("@foo");
  }

  @Test
  public void testBuildParameter() {
    final QueryParameterValue newParameter = QueryParameterValue.int64(42);

    final String parameter = buildParameter(PARAM_MAP, newParameter);
    assertThat(parameter).isEqualTo("@p2");

    assertThat(PARAM_MAP.get("p2")).isEqualTo(newParameter);
  }


}
