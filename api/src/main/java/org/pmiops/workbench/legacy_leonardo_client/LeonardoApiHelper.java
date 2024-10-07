package org.pmiops.workbench.legacy_leonardo_client;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.FailedPreconditionException;
import org.pmiops.workbench.model.ErrorCode;
import org.pmiops.workbench.model.ErrorResponse;
import org.pmiops.workbench.model.SecuritySuspendedErrorParameters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Helper class to hold the common functions between Leonardo Runtime and Leonardo APPs. */
@Component
public class LeonardoApiHelper {
  private final Clock clock;

  @Autowired
  public LeonardoApiHelper(Clock clock) {
    this.clock = clock;
  }

  /** {@throws FailedPreconditionException} if user compute is suspended. */
  public void enforceComputeSecuritySuspension(DbUser user) throws FailedPreconditionException {
    Optional<Instant> suspendedUntil =
        Optional.ofNullable(user.getComputeSecuritySuspendedUntil()).map(Timestamp::toInstant);
    if (suspendedUntil.isPresent() && clock.instant().isBefore(suspendedUntil.get())) {
      throw new FailedPreconditionException(
          new ErrorResponse()
              .errorCode(ErrorCode.COMPUTE_SECURITY_SUSPENDED)
              .message("user is suspended from compute for security reasons")
              .parameters(
                  new SecuritySuspendedErrorParameters()
                      // Instant.toString() yields ISO-8601
                      .suspendedUntil(suspendedUntil.get().toString())));
    }
  }
}
