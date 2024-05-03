package org.pmiops.workbench.actionaudit.targetproperties;

import jakarta.validation.constraints.NotNull;
import java.util.function.Function;
import org.pmiops.workbench.model.SumologicEgressEvent;

/**
 * Action properties relating to high-egress events received by the Workbench. These properties
 * directly relate to values from an EgressEvent object instance.
 */
public enum EgressEventTargetProperty implements ModelBackedTargetProperty<SumologicEgressEvent> {
  EGRESS_MIB("egress_mib", PropertyUtils.stringOrNull(SumologicEgressEvent::getEgressMib)),
  TIME_WINDOW_START(
      "time_window_start",
      event ->
          event.getTimeWindowStart() != null ? event.getTimeWindowStart().toString() : "[no time]"),
  TIME_WINDOW_DURATION(
      "time_window_duration",
      PropertyUtils.stringOrNull(SumologicEgressEvent::getTimeWindowDuration)),
  VM_NAME("vm_prefix", SumologicEgressEvent::getVmPrefix);

  private final String propertyName;
  private final Function<SumologicEgressEvent, String> extractor;

  EgressEventTargetProperty(String propertyName, Function<SumologicEgressEvent, String> extractor) {
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
  public Function<SumologicEgressEvent, String> getExtractor() {
    return extractor;
  }
}
