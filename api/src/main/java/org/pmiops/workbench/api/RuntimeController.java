package org.pmiops.workbench.api;

import static org.pmiops.workbench.leonardo.LeonardoLabelHelper.LEONARDO_LABEL_IS_RUNTIME;
import static org.pmiops.workbench.leonardo.LeonardoLabelHelper.LEONARDO_LABEL_IS_RUNTIME_TRUE;
import static org.pmiops.workbench.leonardo.LeonardoLabelHelper.LEONARDO_LABEL_WORKSPACE_NAME;
import static org.pmiops.workbench.leonardo.LeonardoLabelHelper.LEONARDO_LABEL_WORKSPACE_NAMESPACE;
import static org.pmiops.workbench.leonardo.LeonardoLabelHelper.upsertLeonardoLabel;

import com.google.common.base.Strings;
import jakarta.annotation.Nullable;
import jakarta.inject.Provider;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.interactiveanalysis.InteractiveAnalysisService;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoClusterError;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoGetRuntimeResponse;
import org.pmiops.workbench.legacy_leonardo_client.model.LeonardoRuntimeStatus;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.leonardo.LeonardoApiHelper;
import org.pmiops.workbench.leonardo.PersistentDiskUtils;
import org.pmiops.workbench.model.AppType;
import org.pmiops.workbench.model.Disk;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.GceConfig;
import org.pmiops.workbench.model.GceWithPdConfig;
import org.pmiops.workbench.model.PersistentDiskRequest;
import org.pmiops.workbench.model.Runtime;
import org.pmiops.workbench.model.RuntimeLocalizeRequest;
import org.pmiops.workbench.model.RuntimeLocalizeResponse;
import org.pmiops.workbench.model.UpdateRuntimeRequest;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.utils.mappers.LeonardoMapper;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RuntimeController implements RuntimeApiDelegate {

  private static final Logger log = Logger.getLogger(RuntimeController.class.getName());

  private final LeonardoApiClient leonardoNotebooksClient;
  private final Provider<DbUser> userProvider;
  private final WorkspaceAuthService workspaceAuthService;
  private final WorkspaceService workspaceService;

  private final LeonardoMapper leonardoMapper;
  private final LeonardoApiHelper leonardoApiHelper;
  private final InteractiveAnalysisService interactiveAnalysisService;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  @Autowired
  RuntimeController(
      LeonardoApiClient leonardoNotebooksClient,
      Provider<DbUser> userProvider,
      WorkspaceAuthService workspaceAuthService,
      WorkspaceService workspaceService,
      LeonardoMapper leonardoMapper,
      LeonardoApiHelper leonardoApiHelper,
      InteractiveAnalysisService interactiveAnalysisService,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.leonardoNotebooksClient = leonardoNotebooksClient;
    this.userProvider = userProvider;
    this.workspaceAuthService = workspaceAuthService;
    this.workspaceService = workspaceService;
    this.leonardoMapper = leonardoMapper;
    this.leonardoApiHelper = leonardoApiHelper;
    this.interactiveAnalysisService = interactiveAnalysisService;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  @Override
  public ResponseEntity<Runtime> getRuntime(String workspaceNamespace) {
    DbUser user = userProvider.get();
    leonardoApiHelper.enforceComputeSecuritySuspension(user);

    DbWorkspace dbWorkspace = workspaceService.lookupWorkspaceByNamespace(workspaceNamespace);
    String googleProject = dbWorkspace.getGoogleProject();

    LeonardoGetRuntimeResponse leoRuntimeResponse =
        leonardoNotebooksClient.getRuntime(googleProject, user.getRuntimeName());
    if (LeonardoRuntimeStatus.ERROR.equals(leoRuntimeResponse.getStatus())) {
      log.warning(
          String.format(
              "Observed Leonardo runtime with unexpected error status:\n%s",
              formatRuntimeErrors(leoRuntimeResponse.getErrors())));
    }
    return ResponseEntity.ok(leonardoMapper.toApiRuntime(leoRuntimeResponse));
  }

  private String formatRuntimeErrors(@Nullable List<LeonardoClusterError> errors) {
    if (errors == null || errors.isEmpty()) {
      return "no error messages";
    }
    return errors.stream()
        .map(err -> String.format("error %d: %s", err.getErrorCode(), err.getErrorMessage()))
        .collect(Collectors.joining("\n"));
  }

  @Override
  public ResponseEntity<EmptyResponse> createRuntime(String workspaceNamespace, Runtime runtime) {
    long configCount =
        Stream.ofNullable(runtime)
            .flatMap(
                r -> Stream.of(r.getGceConfig(), r.getDataprocConfig(), r.getGceWithPdConfig()))
            .filter(Objects::nonNull)
            .count();
    if (configCount != 1) {
      throw new BadRequestException(
          "Exactly one of GceConfig or DataprocConfig or GceWithPdConfig must be provided");
    }
    GceConfig gceConfig = runtime.getGceConfig();
    GceWithPdConfig gceWithPdConfig = runtime.getGceWithPdConfig();

    List<String> gceVmZones = workbenchConfigProvider.get().firecloud.gceVmZones;

    DbWorkspace dbWorkspace = workspaceService.lookupWorkspaceByNamespace(workspaceNamespace);
    if (gceConfig != null) {
      if (Strings.isNullOrEmpty(gceConfig.getZone())) {
        throw new BadRequestException("GceConfig must contain a zone");
      }
      if (!gceVmZones.contains(gceConfig.getZone())) {
        throw new BadRequestException("Invalid zone");
      }
    }

    if (gceWithPdConfig != null) {
      if (Strings.isNullOrEmpty(gceWithPdConfig.getZone())) {
        throw new BadRequestException("GceWithPdConfig must contain a zone");
      }
      if (!gceVmZones.contains(gceWithPdConfig.getZone())) {
        throw new BadRequestException("Invalid zone");
      }
      PersistentDiskRequest persistentDiskRequest = gceWithPdConfig.getPersistentDisk();
      if (persistentDiskRequest == null) {
        throw new BadRequestException("GceWithPdConfig must contain a PersistentDiskRequest");
      }

      if (Strings.isNullOrEmpty(persistentDiskRequest.getName())) {
        // If persistentDiskRequest.getName() is empty, UI wants API to create a new disk.
        // Check with Leo again see if user have READY disk, if so, block this request or logging
        List<Disk> diskList =
            PersistentDiskUtils.findTheMostRecentActiveDisks(
                leonardoNotebooksClient
                    .listPersistentDiskByProjectCreatedByCreator(dbWorkspace.getGoogleProject())
                    .stream()
                    .map(leonardoMapper::toApiDisk)
                    .collect(Collectors.toList()));
        List<Disk> runtimeDisks =
            diskList.stream().filter(Disk::isGceRuntime).collect(Collectors.toList());
        if (!runtimeDisks.isEmpty()) {
          // Find active disks for runtime VM. Block user from creating new disk.
          throw new BadRequestException(
              String.format(
                  "Can not create new runtime with new PD if user has active runtime PD. "
                      + "Existing disks: %s, GceWithPdConfig in the request: %s",
                  PersistentDiskUtils.prettyPrintDiskNames(runtimeDisks),
                  runtime.getGceWithPdConfig()));
        }
        persistentDiskRequest.name(userProvider.get().generatePDName());
      }

      var labels = persistentDiskRequest.getLabels();
      labels =
          upsertLeonardoLabel(labels, LEONARDO_LABEL_IS_RUNTIME, LEONARDO_LABEL_IS_RUNTIME_TRUE);
      labels = upsertLeonardoLabel(labels, LEONARDO_LABEL_WORKSPACE_NAMESPACE, workspaceNamespace);
      labels = upsertLeonardoLabel(labels, LEONARDO_LABEL_WORKSPACE_NAME, dbWorkspace.getName());
      persistentDiskRequest.labels(labels);
    }

    DbUser user = userProvider.get();
    leonardoApiHelper.enforceComputeSecuritySuspension(user);

    String firecloudWorkspaceName = dbWorkspace.getFirecloudName();
    workspaceAuthService.enforceWorkspaceAccessLevel(
        workspaceNamespace, firecloudWorkspaceName, WorkspaceAccessLevel.WRITER);
    workspaceAuthService.validateInitialCreditUsage(workspaceNamespace, firecloudWorkspaceName);

    leonardoNotebooksClient.createRuntime(
        runtime.googleProject(dbWorkspace.getGoogleProject()).runtimeName(user.getRuntimeName()),
        workspaceNamespace,
        firecloudWorkspaceName);
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<EmptyResponse> updateRuntime(
      String workspaceNamespace, UpdateRuntimeRequest runtimeRequest) {
    if (runtimeRequest == null || runtimeRequest.getRuntime() == null) {
      throw new BadRequestException("Runtime cannot be empty for an update request");
    }

    if (!(runtimeRequest.getRuntime().getGceConfig() != null
        ^ runtimeRequest.getRuntime().getGceWithPdConfig() != null
        ^ runtimeRequest.getRuntime().getDataprocConfig() != null)) {
      throw new BadRequestException(
          "Exactly one of GceConfig, GceWithPdConfig, or DataprocConfig must be provided");
    }

    DbUser user = userProvider.get();
    leonardoApiHelper.enforceComputeSecuritySuspension(user);

    DbWorkspace dbWorkspace = workspaceService.lookupWorkspaceByNamespace(workspaceNamespace);
    String firecloudWorkspaceName = dbWorkspace.getFirecloudName();
    workspaceAuthService.enforceWorkspaceAccessLevel(
        workspaceNamespace, firecloudWorkspaceName, WorkspaceAccessLevel.WRITER);
    workspaceAuthService.validateInitialCreditUsage(workspaceNamespace, firecloudWorkspaceName);

    leonardoNotebooksClient.updateRuntime(
        runtimeRequest
            .getRuntime()
            .googleProject(dbWorkspace.getGoogleProject())
            .runtimeName(user.getRuntimeName()));

    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<EmptyResponse> deleteRuntime(
      String workspaceNamespace, Boolean deleteDisk) {
    DbUser user = userProvider.get();

    DbWorkspace dbWorkspace = workspaceService.lookupWorkspaceByNamespace(workspaceNamespace);

    leonardoNotebooksClient.deleteRuntime(
        dbWorkspace.getGoogleProject(), user.getRuntimeName(), Boolean.TRUE.equals(deleteDisk));
    return ResponseEntity.ok(new EmptyResponse());
  }

  @Override
  public ResponseEntity<RuntimeLocalizeResponse> localize(
      String workspaceNamespace, Boolean localizeAllFiles, RuntimeLocalizeRequest body) {
    DbUser user = userProvider.get();
    leonardoApiHelper.enforceComputeSecuritySuspension(user);
    DbWorkspace dbWorkspace = workspaceService.lookupWorkspaceByNamespace(workspaceNamespace);
    workspaceAuthService.enforceWorkspaceAccessLevel(
        dbWorkspace.getWorkspaceNamespace(),
        dbWorkspace.getFirecloudName(),
        WorkspaceAccessLevel.WRITER);
    workspaceAuthService.validateInitialCreditUsage(
        dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName());

    AppType appType = null; // Jupyter uses GCE, so it doesn't have a GKE App Type
    return ResponseEntity.ok(
        new RuntimeLocalizeResponse()
            .runtimeLocalDirectory(
                interactiveAnalysisService.localize(
                    workspaceNamespace,
                    userProvider.get().getRuntimeName(),
                    appType,
                    body.getNotebookNames(),
                    true,
                    localizeAllFiles)));
  }
}
