package org.pmiops.workbench.mandrill;

import org.pmiops.workbench.mandrill.model.MandrillMessage;
import org.pmiops.workbench.mandrill.model.MandrillMessageStatus;

/**
 * Encapsulate mandrill API interaction details and provide a simple/mockable interface
 * for internal use.
 */
public interface MandrillService {

  MandrillMessageStatus sendEmail(final MandrillMessage m);

}
