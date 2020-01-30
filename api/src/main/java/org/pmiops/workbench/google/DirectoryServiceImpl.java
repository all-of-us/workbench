/**
 * Note: this file is tested with integration tests rather than unit tests. See
 * src/integration/.../DirectoryServiceImplIntegrationTest.java for test cases.
 */
package org.pmiops.workbench.google;

import static com.google.api.client.googleapis.util.Utils.getDefaultJsonFactory;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.services.directory.Directory;
import com.google.api.services.directory.DirectoryScopes;
import com.google.api.services.directory.model.User;
import com.google.api.services.directory.model.UserEmail;
import com.google.api.services.directory.model.UserName;
import com.google.api.services.directory.model.Users;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.OAuth2Credentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.iam.credentials.v1.IamCredentialsClient;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.inject.Provider;
import org.pmiops.workbench.auth.Constants;
import org.pmiops.workbench.auth.DelegatedUserCredentials;
import org.pmiops.workbench.auth.ServiceAccounts;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.exceptions.ExceptionUtils;
import org.pmiops.workbench.monitoring.GaugeDataCollector;
import org.pmiops.workbench.monitoring.MeasurementBundle;
import org.pmiops.workbench.monitoring.attachments.MetricLabel;
import org.pmiops.workbench.monitoring.views.GaugeMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class DirectoryServiceImpl implements DirectoryService, GaugeDataCollector {

  private static final Logger log = LoggerFactory.getLogger(DirectoryService.class.getName());
  private static final String ALLOWED =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()";
  private static final String APPLICATION_NAME = "All of Us Researcher Workbench";
  // Matches the API org unit path defined in the Gsuite UI where researcher accounts reside.
  private static final String GSUITE_WORKBENCH_ORG_UNIT_PATH = "/workbench-users";
  // Name of the GSuite custom schema containing AOU custom fields.
  private static final String GSUITE_AOU_SCHEMA_NAME = "All_of_Us_Workbench";
  // Name of the "contact email" custom field, which is stored within the AoU GSuite custom schema.
  private static final String GSUITE_FIELD_CONTACT_EMAIL = "Contact_email_address";
  // Name of the "institution" custom field, whose value is the same for all Workbench users.
  private static final String GSUITE_FIELD_INSTITUTION = "Institution";
  private static final String INSTITUTION_FIELD_VALUE = "All of Us Research Workbench";
  private static final int MAX_USERS_LIST_PAGE_SIZE = 500;
  private static final String EMAIL_USER_FIELD = "email";
  private static final String USER_VIEW_TYPE = "domain_public";

  private static final String ADMIN_SERVICE_ACCOUNT_NAME = "gsuite-admin";

  // The username of the G Suite user which will be used to make service-level Directory API
  // requests. A user with this name must exist in each environment's associated G Suite operational
  // unit, and that user should have full admin privileges (e.g. to create, update, delete users).
  private static final String DIRECTORY_SERVICE_USERNAME = "directory-service";

  private static SecureRandom rnd = new SecureRandom();

  // This list must exactly match the scopes allowed via the GSuite Domain Admin page here:
  // https://admin.google.com/fake-research-aou.org/AdminHome?chromeless=1#OGX:ManageOauthClients
  // replace 'fake-research-aou.org' with the specific domain that you want to manage
  // For example, ADMIN_DIRECTORY_USER does not encapsulate ADMIN_DIRECTORY_USER_READONLY — it must
  // be explicit.
  // The "Client Name" field in that form must be the client ID of the service account. The field
  // will accept the email address of the service account and lookup the correct client ID giving
  // the impression that the email address is an acceptable substitute, but testing shows that this
  // doesn't actually work.
  private static final List<String> SCOPES =
      Arrays.asList(
          DirectoryScopes.ADMIN_DIRECTORY_USER_ALIAS,
              DirectoryScopes.ADMIN_DIRECTORY_USER_ALIAS_READONLY,
          DirectoryScopes.ADMIN_DIRECTORY_USER, DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY);

  private final Provider<ServiceAccountCredentials> googleCredentialsProvider;
  private final Provider<WorkbenchConfig> configProvider;
  private final HttpTransport httpTransport;
  private final GoogleRetryHandler retryHandler;
  private final IamCredentialsClient iamCredentialsClient;

  @Autowired
  public DirectoryServiceImpl(
      @Qualifier(Constants.GSUITE_ADMIN_CREDS)
          Provider<ServiceAccountCredentials> googleCredentialsProvider,
      Provider<WorkbenchConfig> configProvider,
      HttpTransport httpTransport,
      GoogleRetryHandler retryHandler,
      IamCredentialsClient iamCredentialsClient) {
    this.googleCredentialsProvider = googleCredentialsProvider;
    this.configProvider = configProvider;
    this.httpTransport = httpTransport;
    this.retryHandler = retryHandler;
    this.iamCredentialsClient = iamCredentialsClient;
  }

  private Directory getGoogleDirectoryService() {
    final OAuth2Credentials delegatedCreds;
    if (configProvider.get().featureFlags.useKeylessDelegatedCredentials) {
      delegatedCreds =
          new DelegatedUserCredentials(
              ServiceAccounts.getServiceAccountEmail(
                  ADMIN_SERVICE_ACCOUNT_NAME, configProvider.get().server.projectId),
              DIRECTORY_SERVICE_USERNAME + "@" + gSuiteDomain(),
              SCOPES,
              iamCredentialsClient,
              httpTransport);
    } else {
      delegatedCreds =
          googleCredentialsProvider
              .get()
              .createScoped(SCOPES)
              .createDelegated("directory-service@" + gSuiteDomain());
    }

    return new Directory.Builder(
            httpTransport, getDefaultJsonFactory(), new HttpCredentialsAdapter(delegatedCreds))
        .build();
  }

  private String gSuiteDomain() {
    return configProvider.get().googleDirectoryService.gSuiteDomain;
  }

  private String getTopLevelGSuiteDomain() {
    final String domain = gSuiteDomain();
    final String[] parts = domain.split("\\.");
    // return the last two parts
    final int numParts = parts.length;
    final int firstIndex = numParts - 2;
    return String.format("%s.%s", parts[firstIndex], parts[firstIndex + 1]);
  }

  @Override
  public User getUserByUsername(String username) {
    return getUser(username + "@" + gSuiteDomain());
  }

  /**
   * Fetches a user by their GSuite email address.
   *
   * <p>If the user is not found, a null value will be returned (no exception is thrown).
   *
   * @param email
   * @return
   */
  @Override
  public User getUser(String email) {
    try {
      // We use the "full" projection to include custom schema fields in the Directory API response.
      return retryHandler.runAndThrowChecked(
          (context) ->
              getGoogleDirectoryService().users().get(email).setProjection("full").execute());
    } catch (GoogleJsonResponseException e) {
      // Handle the special case where we're looking for a not found user by returning
      // null.
      if (e.getDetails().getCode() == HttpStatus.NOT_FOUND.value()) {
        return null;
      }
      throw ExceptionUtils.convertGoogleIOException(e);
    } catch (IOException e) {
      throw ExceptionUtils.convertGoogleIOException(e);
    }
  }

  @Override
  public boolean isUsernameTaken(String username) {
    return getUserByUsername(username) != null;
  }

  // Returns a user's contact email address via the custom schema in the directory API.
  public String getContactEmailAddress(String username) {
    return (String)
        getUserByUsername(username)
            .getCustomSchemas()
            .get(GSUITE_AOU_SCHEMA_NAME)
            .get(GSUITE_FIELD_CONTACT_EMAIL);
  }

  public static void addCustomSchemaAndEmails(User user, String primaryEmail, String contactEmail) {
    // GSuite custom fields for Workbench user accounts.
    // See the Moodle integration doc (broad.io/aou-moodle) for more details, as this
    // was primarily set up for Moodle SSO integration.
    Map<String, Object> aouCustomFields = new HashMap<>();
    // The value of this field must match one of the allowed values in the Moodle installation.
    // Since this value is unlikely to ever change, we use a hard-coded constant rather than an env
    // variable.
    aouCustomFields.put(GSUITE_FIELD_INSTITUTION, INSTITUTION_FIELD_VALUE);

    if (contactEmail != null) {
      // This gives us a structured place to store researchers' contact email addresses, in
      // case we want to pass it to other systems (e.g. Zendesk or Moodle) via SAML mapped fields.
      aouCustomFields.put(GSUITE_FIELD_CONTACT_EMAIL, contactEmail);
    }

    // In addition to the custom schema value, we store each user's contact email as a secondary
    // email address with type "home". This makes it show up nicely in GSuite admin as the
    // user's "Secondary email".
    List<UserEmail> emails =
        Lists.newArrayList(
            new UserEmail().setType("work").setAddress(primaryEmail).setPrimary(true));
    if (contactEmail != null) {
      emails.add(new UserEmail().setType("home").setAddress(contactEmail));
    }
    user.setEmails(emails)
        .setRecoveryEmail(contactEmail)
        .setCustomSchemas(Collections.singletonMap(GSUITE_AOU_SCHEMA_NAME, aouCustomFields));
  }

  @Override
  public User createUser(
      String givenName, String familyName, String username, String contactEmail) {
    String primaryEmail = username + "@" + gSuiteDomain();
    String password = randomString();

    User user =
        new User()
            .setPrimaryEmail(primaryEmail)
            .setPassword(password)
            .setName(new UserName().setGivenName(givenName).setFamilyName(familyName))
            .setChangePasswordAtNextLogin(true)
            .setOrgUnitPath(GSUITE_WORKBENCH_ORG_UNIT_PATH);
    addCustomSchemaAndEmails(user, primaryEmail, contactEmail);

    retryHandler.run((context) -> getGoogleDirectoryService().users().insert(user).execute());
    return user;
  }

  @Override
  public User resetUserPassword(String email) {
    User user = getUser(email);
    String password = randomString();
    user.setPassword(password);
    retryHandler.run(
        (context) -> getGoogleDirectoryService().users().update(email, user).execute());
    return user;
  }

  @Override
  public void deleteUser(String username) {
    try {
      retryHandler.runAndThrowChecked(
          (context) ->
              getGoogleDirectoryService()
                  .users()
                  .delete(username + "@" + gSuiteDomain())
                  .execute());
    } catch (GoogleJsonResponseException e) {
      if (e.getDetails().getCode() == HttpStatus.NOT_FOUND.value()) {
        // Deleting a user that doesn't exist will have no effect.
        return;
      }
      throw ExceptionUtils.convertGoogleIOException(e);
    } catch (IOException e) {
      throw ExceptionUtils.convertGoogleIOException(e);
    }
  }

  @Override
  public Collection<MeasurementBundle> getGaugeData() {
    ImmutableSet.Builder<MeasurementBundle> resultBuilder = ImmutableSet.builder();

    final String localDomain = gSuiteDomain();
    addDomainCountMeasurement(resultBuilder, localDomain, countUsersInDomain(localDomain));

    final String topLevelDomain = getTopLevelGSuiteDomain();
    // Avoid creating duplicate data point if the local domain is the top domain
    if (!localDomain.equals(topLevelDomain)) {
      addDomainCountMeasurement(resultBuilder, topLevelDomain, countUsersInDomain(topLevelDomain));
    }
    return resultBuilder.build();
  }

  private void addDomainCountMeasurement(
      ImmutableSet.Builder<MeasurementBundle> resultBuilder,
      String gSuiteDomain,
      long domainUserCount) {
    resultBuilder.add(
        MeasurementBundle.builder()
            .addMeasurement(GaugeMetric.GSUITE_USER_COUNT, domainUserCount)
            .addTag(MetricLabel.GSUITE_DOMAIN, gSuiteDomain)
            .build());
  }

  private long countUsersInDomain(String gSuiteDomain) {
    long result = 0;
    try {
      final Directory directoryService = getGoogleDirectoryService();
      Optional<String> nextPageToken = Optional.empty();
      do {
        final Directory.Users.List listQuery =
            directoryService
                .users()
                .list()
                .setDomain(gSuiteDomain)
                .setViewType(USER_VIEW_TYPE)
                .setCustomFieldMask("email")
                .setMaxResults(MAX_USERS_LIST_PAGE_SIZE)
                .setOrderBy(EMAIL_USER_FIELD);
        nextPageToken.ifPresent(listQuery::setPageToken);

        final Users usersQueryResult = listQuery.execute();

        result += usersQueryResult.getUsers().size();
        nextPageToken = Optional.ofNullable(usersQueryResult.getNextPageToken());
      } while (nextPageToken.isPresent());
      return result;
    } catch (IOException e) {
      log.warn("Failed to retrieve GSuite User List.", e);
      return 0;
    }
  }

  private String randomString() {
    return IntStream.range(0, 17)
        .boxed()
        .map(x -> ALLOWED.charAt(rnd.nextInt(ALLOWED.length())))
        .map(Object::toString)
        .collect(Collectors.joining(""));
  }
}
