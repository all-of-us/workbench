package org.pmiops.workbench.mailchimp;

import java.util.List;

/**
 * Encapsulate mailchimp API interaction details and provide a simple/mockable interface
 * for internal use.
 */
public interface MailChimpService {
  String addUserContactEmail(String listId, String contactEmail) throws ApiException;

  String getUserVerificationStatus(String listId, String userEmailHash) throws ApiException;
}
