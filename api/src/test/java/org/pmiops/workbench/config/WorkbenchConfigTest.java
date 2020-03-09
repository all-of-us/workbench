package org.pmiops.workbench.config;

import static com.google.common.truth.Truth.assertThat;

import com.google.gson.Gson;
import java.io.FileNotFoundException;
import java.io.FileReader;
import org.junit.Test;
import org.springframework.context.annotation.Bean;

public class WorkbenchConfigTest {

  @Test
  public void testUnsafeEndpointsDisabledInProd() throws FileNotFoundException {
    WorkbenchConfig workbenchConfig = getConfigFromFile("../api/config/config_prod.json");
    assertThat(workbenchConfig.access.unsafeAllowSelfBypass).isFalse();
    assertThat(workbenchConfig.featureFlags.unsafeAllowDeleteUser).isFalse();
  }

  @Test
  public void testUnsafeEndpointsDisabledInStable() throws FileNotFoundException {
    WorkbenchConfig workbenchConfig = getConfigFromFile("../api/config/config_stable.json");
    assertThat(workbenchConfig.access.unsafeAllowSelfBypass).isFalse();
    assertThat(workbenchConfig.featureFlags.unsafeAllowDeleteUser).isFalse();
  }

  @Test
  public void testUnsafeEndpointsDisabledInStaging() throws FileNotFoundException {
    WorkbenchConfig workbenchConfig = getConfigFromFile("../api/config/config_staging.json");
    assertThat(workbenchConfig.access.unsafeAllowSelfBypass).isFalse();
    assertThat(workbenchConfig.featureFlags.unsafeAllowDeleteUser).isFalse();
  }

  @Bean
  private WorkbenchConfig getConfigFromFile(String path) throws FileNotFoundException {
    return new Gson().fromJson(new FileReader(path), WorkbenchConfig.class);
  }
}
