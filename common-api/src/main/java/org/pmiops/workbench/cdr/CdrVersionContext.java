package org.pmiops.workbench.cdr;

import org.pmiops.workbench.db.model.CdrVersion;

/** Maintains state of what CDR version is being used in the context of the current request. */
public class CdrVersionContext {

  // why are we using a ThreadLocal here?
  private static ThreadLocal<CdrVersion> cdrVersion = new ThreadLocal<>();

  /**
   * Call this method from source only if you've already fetched the workspace for the user from
   * Firecloud (and have thus already checked that they are still members of the appropriate
   * authorization domain.) Call it from tests in order to set up the CdrVersion used subsequently
   * when reading CDR metadata or BigQuery.
   *
   * <p>Otherwise, call {@link CdrVersionService#setCdrVersion(CdrVersion)} to check that the
   * requester is in the authorization domain for the CDR before using it.
   */
  public static void setCdrVersionNoCheckAuthDomain(CdrVersion version) {
    cdrVersion.set(version);
  }

  public static void clearCdrVersion() {
    cdrVersion.remove();
  }

  public static CdrVersion getCdrVersion() {
    return cdrVersion.get();
  }
}
