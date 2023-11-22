package org.pmiops.workbench.firecloud;

import com.google.api.client.http.HttpStatusCodes;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.dsde.workbench.client.sam.api.TermsOfServiceApi;
import org.broadinstitute.dsde.workbench.client.sam.model.TermsOfServiceComplianceStatus;
import org.json.JSONException;
import org.json.JSONObject;
import org.pmiops.workbench.calhoun.CalhounRetryHandler;
import org.pmiops.workbench.calhoun.api.ConvertApi;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.firecloud.api.GroupsApi;
import org.pmiops.workbench.firecloud.api.NihApi;
import org.pmiops.workbench.firecloud.api.ProfileApi;
import org.pmiops.workbench.firecloud.api.StaticNotebooksApi;
import org.pmiops.workbench.firecloud.api.StatusApi;
import org.pmiops.workbench.firecloud.model.FirecloudManagedGroupWithMembers;
import org.pmiops.workbench.firecloud.model.FirecloudMe;
import org.pmiops.workbench.firecloud.model.FirecloudNihStatus;
import org.pmiops.workbench.firecloud.model.FirecloudProfile;
import org.pmiops.workbench.notebooks.NotebookUtils;
import org.pmiops.workbench.rawls.RawlsConfig;
import org.pmiops.workbench.rawls.RawlsRetryHandler;
import org.pmiops.workbench.rawls.api.BillingV2Api;
import org.pmiops.workbench.rawls.api.WorkspacesApi;
import org.pmiops.workbench.rawls.model.RawlsCreateRawlsV2BillingProjectFullRequest;
import org.pmiops.workbench.rawls.model.RawlsManagedGroupRef;
import org.pmiops.workbench.rawls.model.RawlsUpdateRawlsBillingAccountRequest;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceACL;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceACLUpdate;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceACLUpdateResponseList;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceListResponse;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceRequest;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceRequestClone;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceResponse;
import org.pmiops.workbench.sam.SamRetryHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.RetryException;
import org.springframework.stereotype.Service;

@Service
// TODO: consider retrying internally when FireCloud returns a 503
public class FireCloudServiceImpl implements FireCloudService {

  public static final int PROJECT_BILLING_ID_SIZE = 8;
  public static final String TERMS_OF_SERVICE_BODY = "app.terra.bio/#terms-of-service";

  private static final Logger log = Logger.getLogger(FireCloudServiceImpl.class.getName());

  private final Provider<WorkbenchConfig> configProvider;

  private final Provider<BillingV2Api> serviceAccountBillingV2ApiProvider;
  private final Provider<BillingV2Api> endUserBillingV2ApiProvider;
  private final Provider<GroupsApi> groupsApiProvider;
  private final Provider<NihApi> nihApiProvider;
  private final Provider<ProfileApi> profileApiProvider;
  private final Provider<StatusApi> statusApiProvider;

  // We call some of the endpoints in these APIs with the user's credentials
  // and others with the app's Service Account credentials

  private final Provider<StaticNotebooksApi> endUserStaticNotebooksApiProvider;
  private final Provider<ConvertApi> endUserStaticConvertApiProvider;

  private final Provider<WorkspacesApi> endUserWorkspacesApiProvider;
  private final Provider<org.pmiops.workbench.firecloud.api.WorkspacesApi>
      fcEndUserWorkspacesApiProvider;
  private final Provider<WorkspacesApi> endUserLenientTimeoutWorkspacesApiProvider;
  private final Provider<WorkspacesApi> serviceAccountWorkspaceApiProvider;

  // old Terms of Service endpoints, before Nov 2023 update
  @Deprecated(forRemoval = true)
  private final Provider<org.pmiops.workbench.firecloud.api.TermsOfServiceApi>
      firecloudTermsOfServiceApiProvider;

  private final Provider<TermsOfServiceApi> termsOfServiceApiProvider;

  private final Provider<LoadingCache<String, FirecloudManagedGroupWithMembers>>
      requestScopedGroupCacheProvider;

  private final CalhounRetryHandler calhounRetryHandler;
  private final FirecloudRetryHandler retryHandler;
  private final RawlsRetryHandler rawlsRetryHandler;
  private final SamRetryHandler samRetryHandler;

  private static final String MEMBER_ROLE = "member";
  private static final String STATUS_SUBSYSTEMS_KEY = "systems";

  private static final String OWNER_FC_ROLE = "owner";
  private static final String THURLOE_STATUS_NAME = "Thurloe";
  private static final String SAM_STATUS_NAME = "Sam";
  private static final String RAWLS_STATUS_NAME = "Rawls";
  private static final String GOOGLE_BUCKETS_STATUS_NAME = "GoogleBuckets";

  // All options are defined in this document:
  // https://docs.google.com/document/d/1YS95Q7ViRztaCSfPK-NS6tzFPrVpp5KUo0FaWGx7VHw/edit#
  public static final List<String> FIRECLOUD_WORKSPACE_REQUIRED_FIELDS =
      ImmutableList.of(
          "accessLevel",
          "workspace.workspaceId",
          "workspace.name",
          "workspace.namespace",
          "workspace.googleProject",
          "workspace.bucketName",
          "workspace.createdBy");

  public static final List<String> FIRECLOUD_WORKSPACE_REQUIRED_FIELDS_FOR_CLONE_FILE_TRANSFER =
      ImmutableList.of("workspace.completedCloneWorkspaceFileTransfer");

  @Autowired
  public FireCloudServiceImpl(
      Provider<WorkbenchConfig> configProvider,
      Provider<ProfileApi> profileApiProvider,
      @Qualifier(RawlsConfig.SERVICE_ACCOUNT_BILLING_V2_API)
          Provider<BillingV2Api> serviceAccountBillingV2ApiProvider,
      @Qualifier(RawlsConfig.END_USER_STATIC_BILLING_V2_API)
          Provider<BillingV2Api> endUserBillingV2ApiProvider,
      Provider<GroupsApi> groupsApiProvider,
      Provider<NihApi> nihApiProvider,
      Provider<ConvertApi> endUserStaticConvertApiProvider,
      @Qualifier(RawlsConfig.END_USER_WORKSPACE_API)
          Provider<WorkspacesApi> endUserWorkspacesApiProvider,
      @Qualifier(FireCloudConfig.END_USER_WORKSPACE_API)
          Provider<org.pmiops.workbench.firecloud.api.WorkspacesApi> fcEndUserWorkspacesApiProvider,
      @Qualifier(RawlsConfig.END_USER_LENIENT_TIMEOUT_WORKSPACE_API)
          Provider<WorkspacesApi> endUserLenientTimeoutWorkspacesApiProvider,
      @Qualifier(RawlsConfig.SERVICE_ACCOUNT_WORKSPACE_API)
          Provider<WorkspacesApi> serviceAccountWorkspaceApiProvider,
      Provider<StatusApi> statusApiProvider,
      @Qualifier(FireCloudConfig.END_USER_STATIC_NOTEBOOKS_API)
          Provider<StaticNotebooksApi> endUserStaticNotebooksApiProvider,
      @Qualifier(FireCloudCacheConfig.SERVICE_ACCOUNT_REQUEST_SCOPED_GROUP_CACHE)
          Provider<LoadingCache<String, FirecloudManagedGroupWithMembers>>
              requestScopedGroupCacheProvider,
      FirecloudRetryHandler retryHandler,
      RawlsRetryHandler rawlsRetryHandler,
      CalhounRetryHandler calhounRetryHandler,
      SamRetryHandler samRetryHandler,

      // old Terms of Service endpoints, before Nov 2023 update
      @Deprecated(forRemoval = true)
          Provider<org.pmiops.workbench.firecloud.api.TermsOfServiceApi>
              firecloudTermsOfServiceApiProvider,
      Provider<TermsOfServiceApi> termsOfServiceApiProvider) {
    this.configProvider = configProvider;
    this.profileApiProvider = profileApiProvider;
    this.serviceAccountBillingV2ApiProvider = serviceAccountBillingV2ApiProvider;
    this.endUserBillingV2ApiProvider = endUserBillingV2ApiProvider;
    this.groupsApiProvider = groupsApiProvider;
    this.nihApiProvider = nihApiProvider;
    this.endUserStaticConvertApiProvider = endUserStaticConvertApiProvider;
    this.endUserWorkspacesApiProvider = endUserWorkspacesApiProvider;
    this.fcEndUserWorkspacesApiProvider = fcEndUserWorkspacesApiProvider;
    this.endUserLenientTimeoutWorkspacesApiProvider = endUserLenientTimeoutWorkspacesApiProvider;
    this.serviceAccountWorkspaceApiProvider = serviceAccountWorkspaceApiProvider;
    this.statusApiProvider = statusApiProvider;
    this.endUserStaticNotebooksApiProvider = endUserStaticNotebooksApiProvider;
    this.requestScopedGroupCacheProvider = requestScopedGroupCacheProvider;
    this.retryHandler = retryHandler;
    this.rawlsRetryHandler = rawlsRetryHandler;
    this.calhounRetryHandler = calhounRetryHandler;
    this.samRetryHandler = samRetryHandler;
    this.firecloudTermsOfServiceApiProvider = firecloudTermsOfServiceApiProvider;
    this.termsOfServiceApiProvider = termsOfServiceApiProvider;
  }

  @Override
  @VisibleForTesting
  public String getApiBasePath() {
    return statusApiProvider.get().getApiClient().getBasePath();
  }

  @Override
  public boolean getFirecloudStatus() {
    try {
      statusApiProvider.get().status();
    } catch (ApiException e) {
      log.log(Level.WARNING, "Firecloud status check request failed", e);
      String response = e.getResponseBody();
      try {
        JSONObject errorBody = new JSONObject(response);
        JSONObject subSystemStatus = errorBody.getJSONObject(STATUS_SUBSYSTEMS_KEY);
        if (subSystemStatus != null) {
          return systemOkay(subSystemStatus, THURLOE_STATUS_NAME)
              && systemOkay(subSystemStatus, SAM_STATUS_NAME)
              && systemOkay(subSystemStatus, RAWLS_STATUS_NAME)
              && systemOkay(subSystemStatus, GOOGLE_BUCKETS_STATUS_NAME);
        }
      } catch (JSONException ignored) {
        // noop - FC status has already failed at this point.
      }
      return false;
    }
    return true;
  }

  private boolean systemOkay(JSONObject systemList, String systemName) {
    return systemList.getJSONObject(systemName).getBoolean("ok");
  }

  @Override
  public FirecloudMe getMe() {
    ProfileApi profileApi = profileApiProvider.get();
    return retryHandler.run((context) -> profileApi.me());
  }

  @Override
  public void registerUser() {
    // TODO: make these fields not required in Firecloud and stop passing them in, or prompt for
    // them (RW-29)
    // August 2023 note: Attempting to register a user without setting these fields does not work.
    FirecloudProfile profile =
        new FirecloudProfile()
            .firstName("None")
            .lastName("None")
            .title("None")
            .institute("None")
            .institutionalProgram("None")
            .programLocationCity("None")
            .programLocationState("None")
            .programLocationCountry("None")
            .pi("None")
            .nonProfitStatus("None");

    retryHandler.run(
        (context) -> {
          profileApiProvider.get().setProfile(profile);
          return null;
        });
  }

  @Override
  public String createAllOfUsBillingProject(String billingProjectName, String servicePerimeter) {
    if (billingProjectName.contains(WORKSPACE_DELIMITER)) {
      throw new IllegalArgumentException(
          String.format(
              "Attempting to create billing project with name (%s) that contains workspace delimiter (%s)",
              billingProjectName, WORKSPACE_DELIMITER));
    }

    RawlsCreateRawlsV2BillingProjectFullRequest request =
        new RawlsCreateRawlsV2BillingProjectFullRequest()
            .billingAccount(configProvider.get().billing.freeTierBillingAccountName())
            .projectName(billingProjectName)
            .servicePerimeter(servicePerimeter);
    BillingV2Api billingV2Api = serviceAccountBillingV2ApiProvider.get();
    rawlsRetryHandler.run(
        (context) -> {
          billingV2Api.createBillingProjectFullV2(request);
          return null;
        });
    return billingProjectName;
  }

  @Override
  public void deleteBillingProject(String billingProjectName) {
    BillingV2Api billingV2Api = serviceAccountBillingV2ApiProvider.get();
    rawlsRetryHandler.run(
        (context) -> {
          billingV2Api.deleteBillingProjectV2(billingProjectName);
          return null;
        });
  }

  @Override
  public void updateBillingAccount(String billingProjectName, String billingAccount) {
    rawlsRetryHandler.run(
        (context) -> {
          endUserBillingV2ApiProvider
              .get()
              .updateBillingProjectBillingAccount(
                  new RawlsUpdateRawlsBillingAccountRequest().billingAccount(billingAccount),
                  billingProjectName);
          return null;
        });
  }

  @Override
  public void updateBillingAccountAsService(String billingProjectName, String billingAccount) {
    rawlsRetryHandler.run(
        (context) -> {
          serviceAccountBillingV2ApiProvider
              .get()
              .updateBillingProjectBillingAccount(
                  new RawlsUpdateRawlsBillingAccountRequest().billingAccount(billingAccount),
                  billingProjectName);
          return null;
        });
  }

  private void addRoleToBillingProject(String email, String projectName, String role) {
    Preconditions.checkArgument(email.contains("@"));
    BillingV2Api billingV2Api = serviceAccountBillingV2ApiProvider.get();

    rawlsRetryHandler.run(
        (context) -> {
          billingV2Api.addUserToBillingProjectV2(projectName, role, email);
          return null;
        });
  }

  @Override
  public void addOwnerToBillingProject(String ownerEmail, String billingProjectName) {
    addRoleToBillingProject(ownerEmail, billingProjectName, OWNER_FC_ROLE);
  }

  @Override
  public void removeOwnerFromBillingProjectAsService(
      String ownerEmailToRemove, String projectName) {
    Preconditions.checkArgument(ownerEmailToRemove.contains("@"));

    final BillingV2Api billingV2Api = serviceAccountBillingV2ApiProvider.get();
    rawlsRetryHandler.run(
        (context) -> {
          billingV2Api.removeUserFromBillingProjectV2(
              projectName, OWNER_FC_ROLE, ownerEmailToRemove);
          return null;
        });
  }

  @Override
  public RawlsWorkspaceDetails createWorkspace(
      String workspaceNamespace, String workspaceName, String authDomainName) {
    WorkspacesApi workspacesApi = endUserLenientTimeoutWorkspacesApiProvider.get();
    RawlsWorkspaceRequest workspaceRequest =
        new RawlsWorkspaceRequest()
            .namespace(workspaceNamespace)
            .name(workspaceName)
            .bucketLocation(configProvider.get().firecloud.workspaceBucketLocation)
            .authorizationDomain(
                ImmutableList.of(new RawlsManagedGroupRef().membersGroupName(authDomainName)))
            .attributes(new HashMap<>());

    return rawlsRetryHandler.run((context) -> workspacesApi.createWorkspace(workspaceRequest));
  }

  @Override
  public RawlsWorkspaceDetails cloneWorkspace(
      String fromWorkspaceNamespace,
      String fromFirecloudName,
      String toWorkspaceNamespace,
      String toFirecloudName,
      String authDomainName) {
    WorkspacesApi workspacesApi = endUserLenientTimeoutWorkspacesApiProvider.get();
    RawlsWorkspaceRequestClone cloneRequest =
        new RawlsWorkspaceRequestClone()
            .namespace(toWorkspaceNamespace)
            .name(toFirecloudName)
            // We copy only the notebooks/ subdirectory as a heuristic to avoid unintentionally
            // propagating copies of large data files elsewhere in the bucket.
            .copyFilesWithPrefix(NotebookUtils.NOTEBOOKS_WORKSPACE_DIRECTORY + "/")
            .authorizationDomain(
                ImmutableList.of(new RawlsManagedGroupRef().membersGroupName(authDomainName)))
            .bucketLocation(configProvider.get().firecloud.workspaceBucketLocation)
            .attributes(new HashMap<>());
    return rawlsRetryHandler.run(
        (context) -> workspacesApi.clone(cloneRequest, fromWorkspaceNamespace, fromFirecloudName));
  }

  @Override
  public RawlsWorkspaceACLUpdateResponseList updateWorkspaceACL(
      String workspaceNamespace, String firecloudName, List<RawlsWorkspaceACLUpdate> aclUpdates) {
    WorkspacesApi workspacesApi = endUserWorkspacesApiProvider.get();
    return rawlsRetryHandler.run(
        (context) -> workspacesApi.updateACL(aclUpdates, workspaceNamespace, firecloudName, false));
  }

  @Override
  public RawlsWorkspaceACL getWorkspaceAclAsService(
      String workspaceNamespace, String firecloudName) {
    WorkspacesApi workspacesApi = serviceAccountWorkspaceApiProvider.get();
    return rawlsRetryHandler.run(
        (context) -> workspacesApi.getACL(workspaceNamespace, firecloudName));
  }

  @Override
  public RawlsWorkspaceResponse getWorkspaceAsService(
      String workspaceNamespace, String firecloudName) {
    WorkspacesApi workspacesApi = serviceAccountWorkspaceApiProvider.get();
    return rawlsRetryHandler.run(
        (context) ->
            workspacesApi.listWorkspaceDetails(
                workspaceNamespace, firecloudName, FIRECLOUD_WORKSPACE_REQUIRED_FIELDS));
  }

  @Override
  public RawlsWorkspaceResponse getWorkspace(String workspaceNamespace, String firecloudName) {
    WorkspacesApi workspacesApi = endUserWorkspacesApiProvider.get();
    return rawlsRetryHandler.run(
        (context) ->
            workspacesApi.listWorkspaceDetails(
                workspaceNamespace, firecloudName, FIRECLOUD_WORKSPACE_REQUIRED_FIELDS));
  }

  @Override
  public Optional<RawlsWorkspaceResponse> getWorkspace(DbWorkspace dbWorkspace) {
    try {
      final RawlsWorkspaceResponse result =
          getWorkspace(dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName());
      return Optional.of(result);
    } catch (WorkbenchException e) {
      log.log(
          Level.INFO,
          e,
          () ->
              String.format(
                  "Exception encountered retrieving workspace with DbWorkspace %s",
                  dbWorkspace.toString()));
      return Optional.empty();
    }
  }

  @Override
  public List<RawlsWorkspaceListResponse> getWorkspaces() {
    return rawlsRetryHandler.run(
        (context) ->
            endUserWorkspacesApiProvider.get().listWorkspaces(FIRECLOUD_WORKSPACE_REQUIRED_FIELDS));
  }

  @Override
  public void deleteWorkspace(String workspaceNamespace, String firecloudName) {
    retryHandler.run(
        (context) -> {
          fcEndUserWorkspacesApiProvider.get().deleteWorkspace(workspaceNamespace, firecloudName);
          return null;
        });
  }

  @Override
  public FirecloudManagedGroupWithMembers getGroup(String groupName) {
    GroupsApi groupsApi = groupsApiProvider.get();
    return retryHandler.run((context) -> groupsApi.getGroup(groupName));
  }

  @Override
  public FirecloudManagedGroupWithMembers createGroup(String groupName) {
    GroupsApi groupsApi = groupsApiProvider.get();
    return retryHandler.run((context) -> groupsApi.createGroup(groupName));
  }

  @Override
  public void addUserToGroup(String email, String groupName) {
    GroupsApi groupsApi = groupsApiProvider.get();
    retryHandler.run(
        (context) -> {
          groupsApi.addUserToGroup(groupName, MEMBER_ROLE, email);
          return null;
        });
  }

  @Override
  public void removeUserFromGroup(String email, String groupName) {
    GroupsApi groupsApi = groupsApiProvider.get();
    retryHandler.run(
        (context) -> {
          groupsApi.removeUserFromGroup(groupName, MEMBER_ROLE, email);
          return null;
        });
  }

  @Override
  public boolean isUserMemberOfGroupWithCache(String email, String groupName) {
    return retryHandler.run(
        (context) -> {
          FirecloudManagedGroupWithMembers group = null;
          try {
            group = requestScopedGroupCacheProvider.get().get(groupName);
          } catch (ExecutionException e) {
            // This is not expected, but might be possible if we access an entry at the exact time
            // at which is is expiring from the cache. Just retry.
            throw new RetryException("cache concurrent access failure", e);
          }
          return group.getMembersEmails().contains(email)
              || group.getAdminsEmails().contains(email);
        });
  }

  @Override
  public String staticJupyterNotebooksConvert(byte[] notebook) {
    return retryHandler.run(
        (context) -> endUserStaticNotebooksApiProvider.get().convertNotebook(notebook));
  }

  @Override
  public String staticRstudioNotebooksConvert(byte[] notebook) {
    return calhounRetryHandler.run(
        (context) -> {
          try {
            return Files.asCharSource(
                    endUserStaticConvertApiProvider.get().convertRmd(notebook),
                    Charset.defaultCharset())
                .read();
          } catch (IOException e) {
            throw new WorkbenchException("fail to read RStudio raw content", e);
          }
        });
  }

  @Override
  public FirecloudNihStatus getNihStatus() {
    NihApi nihApi = nihApiProvider.get();
    return retryHandler.run(
        (context) -> {
          try {
            return nihApi.nihStatus();
          } catch (ApiException e) {
            if (e.getCode() == HttpStatusCodes.STATUS_CODE_NOT_FOUND) {
              return null;
            } else {
              throw e;
            }
          }
        });
  }

  @Override
  public String createBillingProjectName() {
    String randomString =
        Hashing.sha256()
            .hashUnencodedChars(UUID.randomUUID().toString())
            .toString()
            .substring(0, PROJECT_BILLING_ID_SIZE);

    String projectNamePrefix = configProvider.get().billing.projectNamePrefix;
    if (!projectNamePrefix.endsWith("-")) {
      projectNamePrefix = projectNamePrefix + "-";
    }
    return projectNamePrefix + randomString;
  }

  @Override
  public boolean workspaceFileTransferComplete(String workspaceNamespace, String fireCloudName) {
    WorkspacesApi workspacesApi = endUserWorkspacesApiProvider.get();
    return rawlsRetryHandler.run(
        (context) -> {
          RawlsWorkspaceDetails fcWorkspaceDetails =
              workspacesApi
                  .listWorkspaceDetails(
                      workspaceNamespace,
                      fireCloudName,
                      FIRECLOUD_WORKSPACE_REQUIRED_FIELDS_FOR_CLONE_FILE_TRANSFER)
                  .getWorkspace();
          return fcWorkspaceDetails != null
              && notebookTransferComplete(
                  fcWorkspaceDetails
                      .getCompletedCloneWorkspaceFileTransfer()
                      .format(DateTimeFormatter.ISO_DATE_TIME));
        });
  }

  private boolean notebookTransferComplete(String fileTransferTime) {
    return !(StringUtils.isEmpty(fileTransferTime) || fileTransferTime.equals("0"));
  }

  // old Terms of Service endpoints, before Nov 2023 update

  @Deprecated(forRemoval = true)
  @Override
  public void acceptTermsOfService() {
    org.pmiops.workbench.firecloud.api.TermsOfServiceApi termsOfServiceApi =
        firecloudTermsOfServiceApiProvider.get();
    retryHandler.run((context) -> termsOfServiceApi.acceptTermsOfService(TERMS_OF_SERVICE_BODY));
  }

  @Deprecated(forRemoval = true)
  @Override
  public boolean getUserTermsOfServiceStatusDeprecated() {
    org.pmiops.workbench.firecloud.api.TermsOfServiceApi termsOfServiceApi =
        firecloudTermsOfServiceApiProvider.get();
    return retryHandler.run((context) -> termsOfServiceApi.getTermsOfServiceStatus());
  }

  // new Terms of Service endpoints, after Nov 2023 update

  @Override
  public TermsOfServiceComplianceStatus getUserTermsOfServiceStatus() {
    TermsOfServiceApi termsOfServiceApi = termsOfServiceApiProvider.get();
    return samRetryHandler.run((context) -> termsOfServiceApi.getTermsOfServiceComplianceStatus());
  }
}
