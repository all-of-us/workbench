package org.pmiops.workbench.cdr;

import java.util.Optional;
import javax.annotation.Nonnull;
import org.pmiops.workbench.db.model.DbCdrVersion;

/** Maintains state of what CDR version is being used in the context of the current request. */
public class CdrVersionContext {

  private static ThreadLocal<DbCdrVersion> cdrVersion = new ThreadLocal<>();

  /**
   * Call this method from source only if you've already fetched the workspace for the user from
   * Firecloud (and have thus already checked that they are still members of the appropriate
   * authorization domain.) Call it from tests in order to set up the CdrVersion used subsequently
   * when reading CDR metadata or BigQuery.
   *
   * <p>Otherwise, call {@link CdrVersionService#setCdrVersion(DbCdrVersion)} to check that the
   * requester is in the authorization domain for the CDR before using it.
   */
  public static void setCdrVersionNoCheckAuthDomain(DbCdrVersion version) {
    cdrVersion.set(version);
  }

  public static void clearCdrVersion() {
    cdrVersion.remove();
  }

  @Nonnull
  public static DbCdrVersion getCdrVersion() {
    DbCdrVersion version = cdrVersion.get();
    if (version == null) {
      // CDR versions are not available during startup. The value returned from this is then used
      // as a key into the set of CDR data sources, which don't handle null values well. Instead
      // of attempting to return something valid before we're ready, we can stop execution by
      // throwing an exception.
      // Since this is a benign situation, we want to avoid log noise, hence the stack suppression.
      throw suppressStackTrace(new IllegalStateException("No CDR version specified!"));
    }

    return version;
  }

  static <T extends Throwable> T suppressStackTrace(T t) {
    t.setStackTrace(new StackTraceElement[] {});
    return t;
  }

  /**
   * BigQueryService.getBigQueryService() operates in two modes: with and without a CDR context
   *
   * @return the CDR Context's BigQuery project if there is a CDR in context, EMPTY if not
   */
  public static Optional<String> maybeGetBigQueryProject() {
    return Optional.ofNullable(cdrVersion.get()).map(DbCdrVersion::getBigqueryProject);
  }
}
