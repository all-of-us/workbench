package org.pmiops.workbench.api;

import com.google.common.collect.ImmutableMap;
import java.time.Clock;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.leonardo.model.LeonardoAppType;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.notebooks.LeonardoAppClient;
import org.pmiops.workbench.notebooks.LeonardoNotebooksClient;
import org.pmiops.workbench.utils.mappers.LeonardoMapper;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AppController implements AppApiDelegate {

  // This file is used by the All of Us libraries to access workspace/CDR metadata.
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
  private static final String DELOC_PATTERN = "\\.ipynb$";

  private static final Logger log = Logger.getLogger(AppController.class.getName());

  private final Clock clock;
  private final LeonardoAppClient leonardoAppClient;
  private final Provider<DbUser> userProvider;
  private final WorkspaceAuthService workspaceAuthService;
  private final WorkspaceDao workspaceDao;
  private final FireCloudService fireCloudService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;
  private final UserRecentResourceService userRecentResourceService;
  private final LeonardoMapper leonardoMapper;

  Map<String, LeonardoAppType> TYPE_MAPPER =
      ImmutableMap.of("cromwell", LeonardoAppType.CROMWELL, "rsudio", LeonardoAppType.CUSTOM);

  @Autowired
  AppController(
      Clock clock,
      LeonardoNotebooksClient leonardoNotebooksClient,
      LeonardoAppClient leonardoAppClient,
      Provider<DbUser> userProvider,
      WorkspaceAuthService workspaceAuthService,
      WorkspaceDao workspaceDao,
      FireCloudService fireCloudService,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      UserRecentResourceService userRecentResourceService,
      LeonardoMapper leonardoMapper) {
    this.clock = clock;
    this.leonardoAppClient = leonardoAppClient;
    this.userProvider = userProvider;
    this.workspaceAuthService = workspaceAuthService;
    this.workspaceDao = workspaceDao;
    this.fireCloudService = fireCloudService;
    this.workbenchConfigProvider = workbenchConfigProvider;
    this.userRecentResourceService = userRecentResourceService;
    this.leonardoMapper = leonardoMapper;
  }

  private DbWorkspace lookupWorkspace(String workspaceNamespace) throws NotFoundException {
    return workspaceDao
        .getByNamespace(workspaceNamespace)
        .orElseThrow(() -> new NotFoundException("Workspace not found: " + workspaceNamespace));
  }

  @Override
  public ResponseEntity<EmptyResponse> createApp(String workspaceNamespace, String appType) {
    try {
      System.out.println("~~~~~~~");
      System.out.println("~~~~~~~");
      System.out.println("~~~~~~~");
      System.out.println("~~~~~~~");
      System.out.println(appType);
      if(appType.equals("Cromwell")) {
        leonardoAppClient.createLeonardoApp(
            workspaceDao.getByNamespace(workspaceNamespace).get().getGoogleProject(),
            "cromwell",
            LeonardoAppType.CROMWELL);
      } else {
        leonardoAppClient.createLeonardoApp(
            workspaceDao.getByNamespace(workspaceNamespace).get().getGoogleProject(),
            "rstudio",
            LeonardoAppType.CUSTOM);
      }
      return ResponseEntity.ok(new EmptyResponse());
    } catch (Exception e) {
      log.log(Level.WARNING, "fail", e);
      return ResponseEntity.badRequest().body(null);
    }
  }
}
