package org.pmiops.workbench.mandrill;

import org.pmiops.workbench.mandrill.model.MandrillMessage;
import org.pmiops.workbench.mandrill.model.MandrillMessageStatuses;

/**
 * Encapsulate mandrill API interaction details and provide a simple/mockable interface
 * for internal use.
 */
public interface MandrillService {

  MandrillMessageStatuses sendEmail(final MandrillMessage m);

}
