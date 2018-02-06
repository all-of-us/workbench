package org.pmiops.workbench.cdr;

import org.pmiops.workbench.db.model.CdrVersion;
import org.springframework.stereotype.Service;


/**
 * Maintains state of what CDR version is being used in the context of the current request.
 */
@Service
public class CdrVersionContext {

  private static ThreadLocal<CdrVersion> cdrVersion = new ThreadLocal<>();

  public static void setCdrVersion(CdrVersion version) {
    cdrVersion.set(version);
  }

  public static void clearCdrVersion() {
    cdrVersion.remove();
  }

  public static CdrVersion getCdrVersion() {
    return cdrVersion.get();
  }
}
