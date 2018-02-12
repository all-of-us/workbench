package org.pmiops.workbench.mailchimp;


/**
 * Encapsulate mailchimp API interaction details and provide a simple/mockable interface
 * for internal use.
 */
public interface MailChimpService {
  static final String MAILCHIMP_PENDING = "pending";
  static final String MAILCHIMP_KEY_ID = "id";
  static final String MAILCHIMP_KEY_STATUS = "status";

  String addUserContactEmail(String contactEmail);

  String getMember(String userEmailHash);
}
