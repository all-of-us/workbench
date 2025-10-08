package org.pmiops.workbench.absorb;

public enum EnrollmentStatus {
  NOT_STARTED(0),
  IN_PROGRESS(1),
  PENDING_APPROVAL(2),
  COMPLETE(3),
  NOT_COMPLETE(4),
  FAILED(5),
  DECLINED(6),
  PENDING_EVALUATION_REQUIRED(7),
  ON_WAITLIST(8),
  ABSENT(9),
  NOT_APPLICABLE(10),
  PENDING_PROCTOR(11),
  READY_FOR_REVIEW(12);

  private final int value;

  EnrollmentStatus(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }

  public static EnrollmentStatus fromValue(int value) {
    for (EnrollmentStatus status : EnrollmentStatus.values()) {
      if (status.value == value) {
        return status;
      }
    }
    throw new IllegalArgumentException("Unknown EnrollmentStatus value: " + value);
  }
}
