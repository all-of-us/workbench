package org.pmiops.workbench.mailchimp;

import java.util.List;
import java.util.HashMap;
import org.pmiops.workbench.mailchimp.model.GetMemberResponse;

/**
 * Encapsulate mailchimp API interaction details and provide a simple/mockable interface
 * for internal use.
 */
public interface MailChimpService {
  String addUserContactEmail(String contactEmail) throws ApiException;

  GetMemberResponse getMember(String userEmailHash) throws ApiException;
}
