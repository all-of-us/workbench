package org.pmiops.workbench.config;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.gson.Gson;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.pmiops.workbench.config.WorkbenchConfig.EgressAlertRemediationPolicy.Escalation;

public class WorkbenchConfigTest {

  private static Stream<String> allEnvs() {
    return Stream.of("local", "test", "staging", "stable", "preprod", "prod");
  }

  @ParameterizedTest(name = "config_{0}.json is valid")
  @MethodSource("allEnvs")
  public void testConfigFilesAreValid(String env) throws Exception {
    assertThat(FileConfigs.loadConfig(envToConfigPath(env), WorkbenchConfig.class)).isNotNull();
  }

  @ParameterizedTest(name = "config_{0}.json has a valid egress policy")
  @MethodSource("allEnvs")
  public void testEgressPoliciesAreValid(String env) throws Exception {
    WorkbenchConfig config = getConfigForEnv(env);

    if (config.egressAlertRemediationPolicy != null) {
      List<Escalation> escalations = config.egressAlertRemediationPolicy.escalations;
      assertThat(escalations).isNotEmpty();

      int prevIncidentCount = -1;
      for (int i = 0; i < escalations.size(); i++) {
        Escalation e = escalations.get(i);
        assertThat(e.afterIncidentCount).isNotNull();

        assertWithMessage("missing an egress policy action on escalation " + i)
            .that(e.disableUser == null && e.suspendCompute == null)
            .isFalse();
        assertWithMessage("must specify exactly one action on escalation " + i)
            .that(e.disableUser != null && e.suspendCompute != null)
            .isFalse();
        if (e.suspendCompute != null) {
          assertThat(e.suspendCompute.durationMinutes).isNotNull();
        }
        assertWithMessage("escalation incident count should be increasing on escalation " + i)
            .that(e.afterIncidentCount)
            .isGreaterThan(prevIncidentCount);
        prevIncidentCount = e.afterIncidentCount;
      }
    }
  }

  @Test
  public void testUnsafeEndpointsDisabledInProd() throws Exception {
    WorkbenchConfig workbenchConfig = getConfigForEnv("prod");
    assertThat(workbenchConfig.access.unsafeAllowSelfBypass).isFalse();
    assertThat(workbenchConfig.access.unsafeAllowUserCreationFromGSuiteData).isFalse();
    assertThat(workbenchConfig.featureFlags.unsafeAllowDeleteUser).isFalse();
  }

  @Test
  public void testUnsafeEndpointsDisabledInPreprod() throws FileNotFoundException {
    WorkbenchConfig workbenchConfig = getConfigForEnv("preprod");
    assertThat(workbenchConfig.access.unsafeAllowSelfBypass).isFalse();
    assertThat(workbenchConfig.access.unsafeAllowUserCreationFromGSuiteData).isFalse();
    assertThat(workbenchConfig.featureFlags.unsafeAllowDeleteUser).isFalse();
  }

  @Test
  public void testUnsafeEndpointsDisabledInStable() throws Exception {
    WorkbenchConfig workbenchConfig = getConfigForEnv("stable");
    assertThat(workbenchConfig.access.unsafeAllowSelfBypass).isFalse();
    assertThat(workbenchConfig.access.unsafeAllowUserCreationFromGSuiteData).isFalse();
    assertThat(workbenchConfig.featureFlags.unsafeAllowDeleteUser).isFalse();
  }

  @Test
  public void testUnsafeEndpointsDisabledInStaging() throws Exception {
    WorkbenchConfig workbenchConfig = getConfigForEnv("staging");
    assertThat(workbenchConfig.access.unsafeAllowSelfBypass).isFalse();
    assertThat(workbenchConfig.access.unsafeAllowUserCreationFromGSuiteData).isFalse();
    assertThat(workbenchConfig.featureFlags.unsafeAllowDeleteUser).isFalse();
  }

  private String envToConfigPath(String env) {
    return String.format("../api/config/config_%s.json", env);
  }

  private WorkbenchConfig getConfigForEnv(String env) throws FileNotFoundException {
    return new Gson().fromJson(new FileReader(envToConfigPath(env)), WorkbenchConfig.class);
  }
}
