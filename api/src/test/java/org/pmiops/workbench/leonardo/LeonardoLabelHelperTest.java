package org.pmiops.workbench.leonardo;

import static com.google.common.truth.Truth.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.model.AppType;

public class LeonardoLabelHelperTest {

  private Map<String, String> labelMap = new HashMap<>();

  @Test
  public void testInsertEntry() throws Exception {
    LeonardoLabelHelper.upsertLeonardoLabel(labelMap, "key", "value");
    assertThat(labelMap).containsExactly("key", "value");
  }

  @Test
  public void testUpdateEntry() throws Exception {
    labelMap.put("key", "value1");
    LeonardoLabelHelper.upsertLeonardoLabel(labelMap, "key", "value2");
    assertThat(labelMap).containsExactly("key", "value2");
  }

  @Test
  public void testAppTypeToLabelValue() throws Exception {
    assertThat(LeonardoLabelHelper.appTypeToLabelValue(AppType.RSTUDIO)).isEqualTo("rstudio");
    assertThat(LeonardoLabelHelper.appTypeToLabelValue(AppType.CROMWELL)).isEqualTo("cromwell");
  }
}
