package org.pmiops.workbench.actionaudit.targetproperties;

import java.util.Optional;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.pmiops.workbench.config.WorkbenchConfig.EgressAlertRemediationPolicy.Escalation;

/**
 * Action properties describing the specific policy escalation automatically applied in response to
 * a high-egress event within the workbench.
 */
public enum EgressEscalationTargetProperty implements ModelBackedTargetProperty<Escalation> {
  REMEDIATION(
      "remediation",
      escalation -> escalation.disableUser == null ? "suspend_compute" : "disable_user"),

  SUSPEND_COMPUTE_DURATION_MIN(
      "suspend_duration",
      escalation ->
          Optional.ofNullable(escalation.suspendCompute)
              .map(s -> s.durationMinutes)
              .map(Object::toString)
              .orElse(null));

  private final String propertyName;

  private final Function<Escalation, String> extractor;

  EgressEscalationTargetProperty(String propertyName, Function<Escalation, String> extractor) {
    this.propertyName = propertyName;
    this.extractor = extractor;
  }

  @NotNull
  @Override
  public String getPropertyName() {
    return propertyName;
  }

  @NotNull
  @Override
  public Function<Escalation, String> getExtractor() {
    return extractor;
  }
}
