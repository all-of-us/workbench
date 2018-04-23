package org.pmiops.workbench.mailchimp;


import com.ecwid.maleorang.MailchimpClient;
import com.ecwid.maleorang.MailchimpException;
import com.ecwid.maleorang.MailchimpMethod;
import com.ecwid.maleorang.MailchimpObject;
import com.ecwid.maleorang.method.v3_0.lists.members.DeleteMemberMethod;
import com.ecwid.maleorang.method.v3_0.lists.members.EditMemberMethod.Create;
import com.ecwid.maleorang.method.v3_0.lists.members.GetMemberMethod;
import java.io.IOException;
import javax.inject.Provider;

import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.model.ErrorResponse;
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

  @Override
  public String addUserContactEmail(String contactEmail) {
    String userId;
    Create createRequest = new Create(getListId(), contactEmail);
    createRequest.status = MailChimpService.MAILCHIMP_PENDING;
    userId = executeMailChimpRequest(createRequest)
          .mapping.get(MailChimpService.MAILCHIMP_KEY_ID).toString();
    return userId;
  }

  @Override
  public void deleteUserContactEmail(String contactEmail) {
    DeleteMemberMethod delete = new DeleteMemberMethod(getListId(), contactEmail);
    executeMailChimpRequest(delete);
  }

  @Override
  public String getMember(String contactEmail) {
    String status;
    GetMemberMethod getMember = new GetMemberMethod(getListId(), contactEmail);
    status = executeMailChimpRequest(getMember)
        .mapping.get(MailChimpService.MAILCHIMP_KEY_STATUS).toString();
    return status;
  }

  // general function that handles MailChimp client creation and error handling
  private <R extends MailchimpObject> MailchimpObject
      executeMailChimpRequest(MailchimpMethod<R> method) {
    if (apiKey == null) {
      apiKey = cloudStorageServiceProvider.get().readMailChimpApiKey();
    }

    try(MailchimpClient client = new MailchimpClient(apiKey)) {
      return client.execute(method);
    } catch (IOException e) {
      WorkbenchException we = new WorkbenchException(new ErrorResponse().statusCode(500).errorClassName(e.getClass().getName()).message(e.getMessage()));
      throw we;
    } catch (MailchimpException e) {
      WorkbenchException we = new WorkbenchException(new ErrorResponse().statusCode(e.code).errorClassName(e.getClass().getName()).message(e.getMessage()));
      throw we;
    }
  }

  private String getListId() {
    if (listId == null) {
      listId = cloudStorageServiceProvider.get().readMailChimpListId();
    }
    return listId;
  }
}
