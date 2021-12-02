package org.pmiops.workbench.exfiltration;

import java.util.Objects;

/** Pattern configuration details for pulling logs from the Terra runtime log sink. */
public class EgressTerraRuntimeLogPattern {
  private final String name;
  private final String logMessagePattern;

  /**
   * @param name a display name for this configuration, to be shown in an admin view
   * @param logMessagePattern used in a LIKE expression on jsonPayload.message in the runtime logs
   */
  public EgressTerraRuntimeLogPattern(String name, String logMessagePattern) {
    this.name = name;
    this.logMessagePattern = logMessagePattern;
  }

  public String getName() {
    return name;
  }

  public String getLogMessagePattern() {
    return logMessagePattern;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EgressTerraRuntimeLogPattern that = (EgressTerraRuntimeLogPattern) o;
    return name.equals(that.name) && logMessagePattern.equals(that.logMessagePattern);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, logMessagePattern);
  }
}
