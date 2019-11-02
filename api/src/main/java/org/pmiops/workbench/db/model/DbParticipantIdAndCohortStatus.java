package org.pmiops.workbench.db.model;

import org.pmiops.workbench.model.CohortStatus;

/**
 * Projection from {@link DbParticipantCohortStatus} that contains just the participant ID and
 * cohort status.
 */
public interface DbParticipantIdAndCohortStatus {

  Key getParticipantKey();

  CohortStatus getStatus();

  public interface Key {
    public Long getParticipantId();
  }
}
