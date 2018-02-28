package org.pmiops.workbench.mailchimp;


import org.pmiops.workbench.exceptions.BadRequestException;
import org.springframework.http.ResponseEntity;

/**
 * Encapsulate mailchimp API interaction details and provide a simple/mockable interface
 * for internal use.
 */
public interface MailChimpService {
  static final String MAILCHIMP_PENDING = "pending";
  static final String MAILCHIMP_KEY_ID = "id";
  static final String MAILCHIMP_KEY_STATUS = "status";

  String addUserContactEmail(String contactEmail);
  void deleteUserContactEmail(String contactEmail);
  String getMember(String userEmailHash);
}
