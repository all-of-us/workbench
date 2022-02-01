package org.pmiops.workbench.cdr;

import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.exceptions.ServerErrorException;

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
      throw new ServerErrorException("No CDR version specified!");
    }

    return version;
  }

  /**
   * BigQueryService.getBigQueryService() operates in two modes: with and without a CDR context
   * 
   * @return the CDR Context's BigQuery project if there is a CDR in context, null if not
   */
  @Nullable
  public static String nullableGetBigQueryProject() {
    return Optional.ofNullable(cdrVersion.get()).map(DbCdrVersion::getBigqueryProject).orElse(null);
  }
}
