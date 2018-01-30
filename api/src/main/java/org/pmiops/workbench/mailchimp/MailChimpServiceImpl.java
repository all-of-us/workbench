package org.pmiops.workbench.mailchimp;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.mailchimp.api.MailApi;
import org.pmiops.workbench.mailchimp.model.ContactEmailVerificationRequest;
import org.pmiops.workbench.mailchimp.model.EmailListAddResponse;
import org.pmiops.workbench.mailchimp.model.VerificationStatusResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * Encapsulate mailchimp API interaction details and provide a simple/mockable interface
 * for internal use.
 */
public class MailChimpServiceImpl implements MailChimpService{
  private static final Logger log = Logger.getLogger(MailChimpServiceImpl.class.getName());

  private final Provider<MailApi> mailApiProvider;

  @Autowired
  public MailChimpServiceImpl(
      Provider<MailApi> mailApiProvider) {
    this.mailApiProvider = mailApiProvider;
  }
  public EmailListAddResponse addMember(String listId, ContactEmailVerificationRequest request) throws ApiException {
    return mailApiProvider.get().addMember(listId, request);
  }

  public VerificationStatusResponse getUserVerificationStatus(String listId, String userEmailHash) throws ApiException {
    return mailApiProvider.get().getUserVerificationStatus(listId, userEmailHash);
  }
}
