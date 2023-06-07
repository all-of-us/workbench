package org.pmiops.workbench.interactiveanalysis;

import com.google.common.annotations.VisibleForTesting;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.json.JSONObject;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.notebooks.model.StorageLink;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.pmiops.workbench.workspaces.resources.UserRecentResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Services for managing RW interactive analysis tools, e.g. Jupyter, Rstudio. */
@Service
public class InteractiveAnalysisService {

  private static final Logger log = Logger.getLogger(InteractiveAnalysisService.class.getName());

  private final WorkspaceService workspaceService;
  private final FireCloudService fireCloudService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final Provider<DbUser> userProvider;
  private final UserRecentResourceService userRecentResourceService;

  private final LeonardoApiClient leonardoNotebooksClient;

  private static final String AOU_CONFIG_FILENAME = ".all_of_us_config.json";
  private static final String WORKSPACE_NAMESPACE_KEY = "WORKSPACE_NAMESPACE";
  private static final String WORKSPACE_ID_KEY = "WORKSPACE_ID";
  private static final String API_HOST_KEY = "API_HOST";
  private static final String BUCKET_NAME_KEY = "BUCKET_NAME";
  private static final String CDR_VERSION_CLOUD_PROJECT = "CDR_VERSION_CLOUD_PROJECT";
  private static final String CDR_VERSION_BIGQUERY_DATASET = "CDR_VERSION_BIGQUERY_DATASET";
  // The billing project to use for the analysis.
  private static final String BILLING_CLOUD_PROJECT = "BILLING_CLOUD_PROJECT";
  private static final String DATA_URI_PREFIX = "data:application/json;base64,";
  @VisibleForTesting static final String JUPYTER_DELOC_PATTERN = "\\.ipynb$";

  @VisibleForTesting static final String RSTUDIO_DELOC_PATTERN = "\\.(Rmd|R)$";

  @Autowired
  public InteractiveAnalysisService(
      WorkspaceService workspaceService,
      FireCloudService fireCloudService,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      Provider<DbUser> userProvider,
      UserRecentResourceService userRecentResourceService,
      LeonardoApiClient leonardoNotebooksClient) {
    this.workspaceService = workspaceService;
    this.fireCloudService = fireCloudService;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.userProvider = userProvider;
    this.userRecentResourceService = userRecentResourceService;
    this.leonardoNotebooksClient = leonardoNotebooksClient;
  }

  public String localize(
      String workspaceNamespace,
      String appName,
      List<String> fileNames,
      boolean isPlayground,
      boolean isGceRuntime) {
    DbWorkspace dbWorkspace = workspaceService.lookupWorkspaceByNamespace(workspaceNamespace);
    final RawlsWorkspaceDetails firecloudWorkspace;
    try {
      firecloudWorkspace =
          fireCloudService
              .getWorkspace(dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName())
              .getWorkspace();
    } catch (NotFoundException e) {
      throw new NotFoundException(
          String.format(
              "workspace %s/%s not found or not accessible",
              dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName()));
    }
    DbCdrVersion cdrVersion = dbWorkspace.getCdrVersion();

    // For the common case where the notebook cluster matches the workspace
    // namespace, simply name the directory as the workspace ID; else we
    // include the namespace in the directory name to avoid possible conflicts
    // in workspace IDs.
    String gcsNotebooksDir = "gs://" + firecloudWorkspace.getBucketName() + "/notebooks";
    long workspaceId = dbWorkspace.getWorkspaceId();

    fileNames.forEach(
        notebookName ->
            userRecentResourceService.updateNotebookEntry(
                workspaceId, userProvider.get().getUserId(), gcsNotebooksDir + "/" + notebookName));

    String workspacePath = dbWorkspace.getFirecloudName();
    String googleProjectId = dbWorkspace.getGoogleProject();
    // Use current dir if not Jupyter
    String editDir = isGceRuntime ? "workspaces/" + workspacePath : "";
    String playgroundDir =
        isGceRuntime ? "workspaces_playground/" + workspacePath : "workspaces_playground";
    String targetDir = isPlayground ? playgroundDir : editDir;

    if (isGceRuntime) {
      leonardoNotebooksClient.createStorageLinkForRuntime(
          googleProjectId,
          appName,
          new StorageLink()
              .cloudStorageDirectory(gcsNotebooksDir)
              .localBaseDirectory(editDir)
              .localSafeModeBaseDirectory(playgroundDir)
              .pattern(JUPYTER_DELOC_PATTERN));
    } else {
      // For now if the request is not for GCE runtime, that would be RStudio. When supporting more
      // apps, consider to use AppType.
      leonardoNotebooksClient.createStorageLinkForApp(
          googleProjectId,
          appName,
          new StorageLink()
              .cloudStorageDirectory(gcsNotebooksDir)
              .localBaseDirectory(editDir)
              .localSafeModeBaseDirectory(playgroundDir)
              .pattern(RSTUDIO_DELOC_PATTERN));
    }

    // Always localize config files; usually a no-op after the first call.
    Map<String, String> localizeMap = new HashMap<>();

    // The Welder extension offers direct links to/from playground mode; write the AoU config file
    // to both locations so notebooks will work in either directory.

    String aouConfigUri =
        aouConfigDataUri(
            firecloudWorkspace,
            cdrVersion,
            workspaceNamespace,
            workbenchConfigProvider.get().server.apiBaseUrl);

    // If current dir is empty (e.g. creating RStudio notebook), don't append '/', otherwise, it
    // becomes root directory.
    String aouConfigEditDir =
        editDir.isEmpty() ? AOU_CONFIG_FILENAME : editDir + "/" + AOU_CONFIG_FILENAME;
    String localizeTargetDir = targetDir.isEmpty() ? "" : targetDir + "/";

    localizeMap.put(aouConfigEditDir, aouConfigUri);
    localizeMap.put(playgroundDir + "/" + AOU_CONFIG_FILENAME, aouConfigUri);

    // Localize the requested notebooks, if any.
    localizeMap.putAll(
        fileNames.stream()
            .collect(
                Collectors.toMap(
                    name -> localizeTargetDir + name, name -> gcsNotebooksDir + "/" + name)));

    if (isGceRuntime) {
      leonardoNotebooksClient.localizeForRuntime(googleProjectId, appName, localizeMap);
    } else {
      leonardoNotebooksClient.localizeForApp(googleProjectId, appName, localizeMap);
    }
    return targetDir;
  }

  @VisibleForTesting
  static String aouConfigDataUri(
      RawlsWorkspaceDetails fcWorkspace,
      DbCdrVersion cdrVersion,
      String cdrBillingCloudProject,
      String apiServerBaseUrl) {
    JSONObject config = new JSONObject();

    String host;
    try {
      host = new URL(apiServerBaseUrl).getHost();
    } catch (MalformedURLException e) {
      log.log(Level.SEVERE, "bad apiBaseUrl config value; failing", e);
      throw new ServerErrorException("Failed to generate AoU notebook config");
    }
    config.put(WORKSPACE_NAMESPACE_KEY, fcWorkspace.getNamespace());
    config.put(WORKSPACE_ID_KEY, fcWorkspace.getName());
    config.put(BUCKET_NAME_KEY, fcWorkspace.getBucketName());
    config.put(API_HOST_KEY, host);
    config.put(CDR_VERSION_CLOUD_PROJECT, cdrVersion.getBigqueryProject());
    config.put(CDR_VERSION_BIGQUERY_DATASET, cdrVersion.getBigqueryDataset());
    config.put(BILLING_CLOUD_PROJECT, cdrBillingCloudProject);
    return jsonToDataUri(config);
  }

  private static String jsonToDataUri(JSONObject json) {
    return DATA_URI_PREFIX + Base64.getUrlEncoder().encodeToString(json.toString().getBytes());
  }
}
