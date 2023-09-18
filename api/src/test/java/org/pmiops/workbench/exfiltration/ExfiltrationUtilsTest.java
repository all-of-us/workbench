package org.pmiops.workbench.exfiltration;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;
import org.pmiops.workbench.model.AppType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
public class ExfiltrationUtilsTest {

  @Test
  public void testExtractUserIdFromGkeServiceName() {
    assertThat(
            ExfiltrationUtils.gkeServiceNameToUserDatabaseId("all-of-us-123-sas-random-bla").get())
        .isEqualTo(123);
  }

  @Test
  public void testExtractUserIdFromGceVmName() {
    assertThat(ExfiltrationUtils.gceVmNameToUserDatabaseId("all-of-us-123").get()).isEqualTo(123);
  }

  @Test
  public void testExtractAppTypeFromGkeServiceName() {
    assertThat(ExfiltrationUtils.gkeServiceNameToAppType("all-of-us-123-sas-random-bla").get())
        .isEqualTo(AppType.SAS);
    assertThat(ExfiltrationUtils.gkeServiceNameToAppType("all-of-us-123-rstudio-random-sas").get())
        .isEqualTo(AppType.RSTUDIO);
  }
}
