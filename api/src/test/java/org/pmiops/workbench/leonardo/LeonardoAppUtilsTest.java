package org.pmiops.workbench.leonardo;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;
import org.pmiops.workbench.model.AppType;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
public class LeonardoAppUtilsTest {

  @Test
  public void testExtractAppTypeFromGkeServiceName() {
    assertThat(LeonardoAppUtils.appServiceNameToAppType("all-of-us-123-sas-random-bla").get())
        .isEqualTo(AppType.SAS);
    assertThat(LeonardoAppUtils.appServiceNameToAppType("all-of-us-123-rstudio-random-sas").get())
        .isEqualTo(AppType.RSTUDIO);
  }
}
