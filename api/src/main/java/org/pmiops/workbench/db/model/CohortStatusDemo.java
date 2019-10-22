package org.pmiops.workbench.db.model;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import org.pmiops.workbench.model.CohortStatus;

// The name is temporary.
// The plan is to migrate the client enum names into something like ApiCohortStatus,
// so this enum would just be CohortStatus.
public enum CohortStatusDemo {
  EXCLUDED("Excluded or whatever", false),
  INCLUDED("Yay! I'm in, boys", true),
  NEEDS_FURTHER_REVIEW("One of these days", false),
  NOT_REVIEWED("Didn't even get the chance", false);

  private final String description;
  private final boolean isHappy;

  CohortStatusDemo(String description, boolean isHappy) {
    this.description = description;
    this.isHappy = isHappy;
  }

  public short toStorage() {
    return CLIENT_TO_STORAGE.get(toClientCohortStatus());
  }

  public CohortStatus toClientCohortStatus() {
    return CLIENT_TO_COHORT_STATUS_2.inverse().get(this);
  }

  public static CohortStatusDemo fromClientCohortStatus(CohortStatus cohortStatus) {
    return CLIENT_TO_COHORT_STATUS_2.get(cohortStatus);
  }

  public static CohortStatusDemo fromStorage(short ordinal) {
    // this one is two hops, so we only need 2 maps. Though we could make a third bimap....
    final CohortStatus clientStatus = CLIENT_TO_STORAGE.inverse().get(ordinal);
    return CLIENT_TO_COHORT_STATUS_2.get(clientStatus);
  }

  private static final BiMap<CohortStatus, CohortStatusDemo> CLIENT_TO_COHORT_STATUS_2 =
      ImmutableBiMap.<CohortStatus, CohortStatusDemo>builder()
          .put(CohortStatus.EXCLUDED, EXCLUDED)
          .put(CohortStatus.INCLUDED, INCLUDED)
          .put(CohortStatus.NEEDS_FURTHER_REVIEW, NEEDS_FURTHER_REVIEW)
          .put(CohortStatus.NOT_REVIEWED, NOT_REVIEWED)
          .build();

  private static final BiMap<CohortStatus, Short> CLIENT_TO_STORAGE =
      ImmutableBiMap.<CohortStatus, Short>builder()
          .put(CohortStatus.EXCLUDED, (short) 0)
          .put(CohortStatus.INCLUDED, (short) 1)
          .put(CohortStatus.NEEDS_FURTHER_REVIEW, (short) 2)
          .put(CohortStatus.NOT_REVIEWED, (short) 3)
          .build();

  public String getDescription() {
    return description;
  }

  public boolean isHappy() {
    return isHappy;
  }
}
