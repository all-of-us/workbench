package org.pmiops.workbench.actionaudit;

import jakarta.annotation.Nullable;
import org.pmiops.workbench.db.model.DbUser;

public record Agent(AgentType agentType, @Nullable Long idMaybe, @Nullable String emailMaybe) {
  public static Agent asUser(DbUser u) {
    return new Agent(AgentType.USER, u.getUserId(), u.getUsername());
  }

  public static Agent asAdmin(DbUser u) {
    return new Agent(AgentType.ADMINISTRATOR, u.getUserId(), u.getUsername());
  }

  public static Agent asSystem() {
    return new Agent(AgentType.SYSTEM, null, null);
  }
}
