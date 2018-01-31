package org.pmiops.workbench.mailchimp;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import com.ecwid.maleorang.MailchimpClient;
import com.ecwid.maleorang.method.v3_0.lists.members.EditMemberMethod.Create;
import com.ecwid.maleorang.method.v3_0.lists.members.GetMemberMethod;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.google.CloudStorageService;
import org.pmiops.workbench.mailchimp.model.GetMemberResponse;
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

  @Autowired
  public MailChimpServiceImpl(Provider<CloudStorageService> cloudStorageServiceProvider) {
    this.cloudStorageServiceProvider = cloudStorageServiceProvider;
  }
  @Override
  public String addUserContactEmail(String contactEmail) throws ApiException {
    String userId;
    Create createRequest = new Create(
        cloudStorageServiceProvider.get().readMailChimpListId(),
        contactEmail);
    createRequest.status = "pending";
    try {
      userId = getClient().execute(createRequest).mapping.get("id").toString();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return userId;
  }

  @Override
  public GetMemberResponse getMember(String userEmailHash) throws ApiException {
    Map<String, Object> mailchimpResponse;
    try {
      mailchimpResponse = getClient().execute(
          new GetMemberMethod(cloudStorageServiceProvider.get().readMailChimpListId(),
          userEmailHash)).mapping;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    GetMemberResponse response = new GetMemberResponse(mailchimpResponse.get("email_address").toString(),
        mailchimpResponse.get("status").toString());
    return response;
  }

  MailchimpClient getClient() {
    if (apiKey == null) {
      apiKey = cloudStorageServiceProvider.get().readMailChimpApiKey();
    }
    return new MailchimpClient(apiKey);
  }
}
