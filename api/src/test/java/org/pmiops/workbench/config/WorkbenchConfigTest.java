package org.pmiops.workbench.config;

import static com.google.common.truth.Truth.assertThat;

import com.google.gson.Gson;
import java.io.FileNotFoundException;
import java.io.FileReader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class WorkbenchConfigTest {

  @ParameterizedTest(name = "config_{0}.json is valid")
  @ValueSource(strings = {"local", "test", "perf", "staging", "stable", "preprod", "prod"})
  public void testConfigFilesAreValid(String env) throws Exception {
    String path = String.format("../api/config/config_%s.json", env);
    assertThat(FileConfigs.loadConfig(path, WorkbenchConfig.class)).isNotNull();
  }

  @Test
  public void testUnsafeEndpointsDisabledInProd() throws Exception {
    WorkbenchConfig workbenchConfig = getConfigFromFile("../api/config/config_prod.json");
    assertThat(workbenchConfig.access.unsafeAllowSelfBypass).isFalse();
    assertThat(workbenchConfig.access.unsafeAllowUserCreationFromGSuiteData).isFalse();
    assertThat(workbenchConfig.featureFlags.unsafeAllowDeleteUser).isFalse();
  }

  @Test
  public void testUnsafeEndpointsDisabledInStable() throws Exception {
    WorkbenchConfig workbenchConfig = getConfigFromFile("../api/config/config_stable.json");
    assertThat(workbenchConfig.access.unsafeAllowSelfBypass).isFalse();
    assertThat(workbenchConfig.access.unsafeAllowUserCreationFromGSuiteData).isFalse();
    assertThat(workbenchConfig.featureFlags.unsafeAllowDeleteUser).isFalse();
  }

  @Test
  public void testUnsafeEndpointsDisabledInStaging() throws Exception {
    WorkbenchConfig workbenchConfig = getConfigFromFile("../api/config/config_staging.json");
    assertThat(workbenchConfig.access.unsafeAllowSelfBypass).isFalse();
    assertThat(workbenchConfig.access.unsafeAllowUserCreationFromGSuiteData).isFalse();
    assertThat(workbenchConfig.featureFlags.unsafeAllowDeleteUser).isFalse();
  }

  private WorkbenchConfig getConfigFromFile(String path) throws FileNotFoundException {
    return new Gson().fromJson(new FileReader(path), WorkbenchConfig.class);
  }
}
