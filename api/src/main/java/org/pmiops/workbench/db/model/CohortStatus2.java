package org.pmiops.workbench.db.model;

import org.pmiops.workbench.model.CohortStatus;

// The name is temporary.
// The plan is to migrate the client enum names into something like ApiCohortStatus,
// so this enum would just be CohortStatus.
public enum CohortStatus2 {
  EXCLUDED(CohortStatus.EXCLUDED, (short) 0),
  INCLUDED(CohortStatus.INCLUDED, (short)1),
  NEEDS_FURTHER_REVIEW(CohortStatus.NEEDS_FURTHER_REVIEW, (short) 2),
  NOT_REVIEWED(CohortStatus.NEEDS_FURTHER_REVIEW, (short) 3);

  private final CohortStatus clientCohortStatus;
  private final short dbOrdinal;

  CohortStatus2(CohortStatus clientCohortStatus, short dbOrdinal) {

    this.clientCohortStatus = clientCohortStatus;
    this.dbOrdinal = dbOrdinal;
  }

  public short toStorage() {
    return dbOrdinal;
  }

  public CohortStatus toClientCohortStatus() {
    return clientCohortStatus;
  }

  public static CohortStatus2 fromClientCohortStatus(CohortStatus cohortStatus) {
    switch (cohortStatus) {
      case EXCLUDED:
        return CohortStatus2.EXCLUDED;
      case INCLUDED:
        return CohortStatus2.INCLUDED;
      case NEEDS_FURTHER_REVIEW:
        return CohortStatus2.NEEDS_FURTHER_REVIEW;
      case NOT_REVIEWED:
        return CohortStatus2.NOT_REVIEWED;
      default:
        throw new IllegalArgumentException(String.format("Unrecognized CohortStatus %s",
            cohortStatus.toString());
    }
  }

  public static CohortStatus2 fromOrdinal(short ordinal) {
    // If this catches on, we'd move StorageEnums.cohortStatusFromStorage to this class
     CohortStatus clientStatus = StorageEnums.cohortStatusFromStorage(ordinal);
     return CohortStatus2.fromClientCohortStatus(clientStatus);
  }
}
