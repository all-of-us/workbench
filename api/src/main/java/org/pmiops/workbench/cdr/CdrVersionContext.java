package org.pmiops.workbench.cdr;

import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
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
