package org.pmiops.workbench.mailchimp;

import java.util.List;
import org.pmiops.workbench.mailchimp.model.ContactEmailVerificationRequest;
import org.pmiops.workbench.mailchimp.model.EmailListAddResponse;
import org.pmiops.workbench.mailchimp.model.VerificationStatusResponse;

/**
 * Encapsulate mailchimp API interaction details and provide a simple/mockable interface
 * for internal use.
 */
public interface MailChimpService {
  EmailListAddResponse addMember(String listId, ContactEmailVerificationRequest request) throws ApiException;

  VerificationStatusResponse getUserVerificationStatus(String listId, String userEmailHash) throws ApiException;
}
