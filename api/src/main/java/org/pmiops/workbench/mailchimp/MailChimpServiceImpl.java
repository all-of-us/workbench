package org.pmiops.workbench.mailchimp;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import com.ecwid.maleorang.MailchimpClient;
import com.ecwid.maleorang.MailchimpException;
import com.ecwid.maleorang.method.v3_0.lists.members.EditMemberMethod.Create;
import com.ecwid.maleorang.method.v3_0.lists.members.GetMemberMethod;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.google.CloudStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * Encapsulate mailchimp API interaction details and provide a simple/mockable interface
 * for internal use.
 */
@Service
public class MailChimpServiceImpl implements MailChimpService {

  private static final Logger log = Logger.getLogger(MailChimpServiceImpl.class.getName());

  private final Provider<CloudStorageService> cloudStorageServiceProvider;
  private String apiKey = null;
  private String listId = null;
  @Autowired
  public MailChimpServiceImpl(Provider<CloudStorageService> cloudStorageServiceProvider) {
    this.cloudStorageServiceProvider = cloudStorageServiceProvider;
  }
  @Override
  public String addUserContactEmail(String contactEmail) {
    String userId;
    if (listId == null) {
      listId = cloudStorageServiceProvider.get().readMailChimpListId();
    }
    Create createRequest = new Create(
        listId,
        contactEmail);
    createRequest.status = MailChimpService.MAILCHIMP_PENDING;
    try {
      userId = getClient().execute(createRequest)
          .mapping.get(MailChimpService.MAILCHIMP_KEY_ID).toString();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return userId;
  }

  @Override
  public String getMember(String contactEmail) throws NotFoundException {
    Map<String, Object> mailchimpResponse;
    try {
      mailchimpResponse = getClient().execute(
          new GetMemberMethod(listId,
          contactEmail)).mapping;
    } catch (MailchimpException e) {
      if (e.code == 404) {
        throw new NotFoundException();
      } else {
        throw new RuntimeException(e);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return mailchimpResponse.get(MailChimpService.MAILCHIMP_KEY_STATUS).toString();
  }

  MailchimpClient getClient() {
    if (apiKey == null) {
      apiKey = cloudStorageServiceProvider.get().readMailChimpApiKey();
    }
    if (listId == null) {
      listId = cloudStorageServiceProvider.get().readMailChimpListId();
    }
    return new MailchimpClient(apiKey);
  }
}
