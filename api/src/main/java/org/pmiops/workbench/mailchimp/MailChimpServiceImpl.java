package org.pmiops.workbench.mailchimp;


import com.ecwid.maleorang.MailchimpClient;
import com.ecwid.maleorang.MailchimpException;
import com.ecwid.maleorang.method.v3_0.lists.members.DeleteMemberMethod;
import com.ecwid.maleorang.method.v3_0.lists.members.EditMemberMethod.Create;
import com.ecwid.maleorang.method.v3_0.lists.members.GetMemberMethod;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Provider;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.google.CloudStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * Encapsulate mailchimp API interaction details and provide a simple/mockable interface
 * for internal use.
 */
@Service
public class MailChimpServiceImpl implements MailChimpService {

  private final Provider<CloudStorageService> cloudStorageServiceProvider;
  private String apiKey = null;
  private String listId = null;

  @Autowired
  public MailChimpServiceImpl(Provider<CloudStorageService> cloudStorageServiceProvider) {
    this.cloudStorageServiceProvider = cloudStorageServiceProvider;
  }

  // TO-DO: create a general "call MailChimp API" that handles an IOException so addUserContactEmail and getMember don't have to

  @Override
  public String addUserContactEmail(String contactEmail) throws BadRequestException {
    String userId;
    Create createRequest = new Create(
        getListId(),
        contactEmail);
    createRequest.status = MailChimpService.MAILCHIMP_PENDING;
    try {
      userId = getClient().execute(createRequest)
          .mapping.get(MailChimpService.MAILCHIMP_KEY_ID).toString();
    } catch (IOException | MailchimpException e) {
      throw new RuntimeException(e);
    }    return userId;
  }

  @Override
  public void deleteUserContactEmail(String contactEmail) throws BadRequestException {
    try {
      getClient().execute(new DeleteMemberMethod(getListId(), contactEmail));
    } catch (IOException | MailchimpException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getMember(String contactEmail) {
    Map<String, Object> mailchimpResponse = new HashMap<String, Object>();
    try {
      mailchimpResponse = getClient().execute(
          new GetMemberMethod(getListId(),
              contactEmail)).mapping;
    } catch (IOException | MailchimpException e) {
      throw new RuntimeException(e);
    }
    return mailchimpResponse.get(MailChimpService.MAILCHIMP_KEY_STATUS).toString();
  }

  private String getListId() {
    if (listId == null) {
      listId = cloudStorageServiceProvider.get().readMailChimpListId();
    }
    return listId;
  }

  MailchimpClient getClient() {
    if (apiKey == null) {
      apiKey = cloudStorageServiceProvider.get().readMailChimpApiKey();
    }
    return new MailchimpClient(apiKey);
  }
}
