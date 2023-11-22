package org.pmiops.workbench.api;

import com.google.common.base.Strings;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import javax.inject.Provider;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.interactiveanalysis.InteractiveAnalysisService;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.leonardo.LeonardoApiHelper;
import org.pmiops.workbench.leonardo.LeonardoLabelHelper;
import org.pmiops.workbench.leonardo.PersistentDiskUtils;
import org.pmiops.workbench.leonardo.model.LeonardoClusterError;
import org.pmiops.workbench.leonardo.model.LeonardoGetRuntimeResponse;
import org.pmiops.workbench.leonardo.model.LeonardoListRuntimeResponse;
import org.pmiops.workbench.leonardo.model.LeonardoRuntimeStatus;
import org.pmiops.workbench.model.AppType;
import org.pmiops.workbench.model.Disk;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.GceWithPdConfig;
import org.pmiops.workbench.model.PersistentDiskRequest;
import org.pmiops.workbench.model.Runtime;
import org.pmiops.workbench.model.RuntimeLocalizeRequest;
import org.pmiops.workbench.model.RuntimeLocalizeResponse;
import org.pmiops.workbench.model.RuntimeStatus;
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

  @Autowired
  RuntimeController(
      LeonardoApiClient leonardoNotebooksClient,
      Provider<DbUser> userProvider,
      WorkspaceAuthService workspaceAuthService,
      WorkspaceService workspaceService,
      LeonardoMapper leonardoMapper,
      LeonardoApiHelper leonardoApiHelper,
      InteractiveAnalysisService interactiveAnalysisService) {
    this.leonardoNotebooksClient = leonardoNotebooksClient;
    this.userProvider = userProvider;
    this.workspaceAuthService = workspaceAuthService;
    this.workspaceService = workspaceService;
    this.leonardoMapper = leonardoMapper;
    this.leonardoApiHelper = leonardoApiHelper;
    this.interactiveAnalysisService = interactiveAnalysisService;
  }

  @Override
  public ResponseEntity<Runtime> getRuntime(String workspaceNamespace) {
    DbUser user = userProvider.get();
    leonardoApiHelper.enforceComputeSecuritySuspension(user);

    DbWorkspace dbWorkspace = workspaceService.lookupWorkspaceByNamespace(workspaceNamespace);
    String googleProject = dbWorkspace.getGoogleProject();
    try {
      LeonardoGetRuntimeResponse leoRuntimeResponse =
          leonardoNotebooksClient.getRuntime(googleProject, user.getRuntimeName());
      if (LeonardoRuntimeStatus.ERROR.equals(leoRuntimeResponse.getStatus())) {
        log.warning(
            String.format(
                "Observed Leonardo runtime with unexpected error status:\n%s",
                formatRuntimeErrors(leoRuntimeResponse.getErrors())));
      }
      return ResponseEntity.ok(leonardoMapper.toApiRuntime(leoRuntimeResponse));
    } catch (NotFoundException e) {
      return ResponseEntity.ok(getOverrideFromListRuntimes(googleProject));
    }
  }

  private String formatRuntimeErrors(@Nullable List<LeonardoClusterError> errors) {
    if (errors == null || errors.isEmpty()) {
      return "no error messages";
    }
    return errors.stream()
        .map(err -> String.format("error %d: %s", err.getErrorCode(), err.getErrorMessage()))
        .collect(Collectors.joining("\n"));
  }

  private Runtime getOverrideFromListRuntimes(String googleProject) {
    Optional<LeonardoListRuntimeResponse> mostRecentRuntimeMaybe =
        leonardoNotebooksClient.listRuntimesByProject(googleProject, true).stream()
            .min(
                (a, b) -> {
                  String aCreatedDate, bCreatedDate;
                  if (a.getAuditInfo() == null || a.getAuditInfo().getCreatedDate() == null) {
                    aCreatedDate = "";
                  } else {
                    aCreatedDate = a.getAuditInfo().getCreatedDate();
                  }

                  if (b.getAuditInfo() == null || b.getAuditInfo().getCreatedDate() == null) {
                    bCreatedDate = "";
                  } else {
                    bCreatedDate = b.getAuditInfo().getCreatedDate();
                  }

                  return bCreatedDate.compareTo(aCreatedDate);
                });

    LeonardoListRuntimeResponse mostRecentRuntime =
        mostRecentRuntimeMaybe.orElseThrow(NotFoundException::new);

    @SuppressWarnings("unchecked")
    Map<String, String> runtimeLabels = (Map<String, String>) mostRecentRuntime.getLabels();

    if (runtimeLabels != null
        && LeonardoMapper.RUNTIME_CONFIGURATION_TYPE_ENUM_TO_STORAGE_MAP
            .values()
            .contains(runtimeLabels.get(LeonardoLabelHelper.LEONARDO_LABEL_AOU_CONFIG))) {
      try {
        Runtime runtime = leonardoMapper.toApiRuntime(mostRecentRuntime);
        if (!RuntimeStatus.DELETED.equals(runtime.getStatus())) {
          log.warning(
              "Runtimes returned from ListRuntimes should be DELETED but found "
                  + runtime.getStatus());
        }
        return runtime.status(RuntimeStatus.DELETED);
      } catch (RuntimeException e) {
        log.warning(
            "RuntimeException during LeonardoListRuntimeResponse -> Runtime mapping "
                + e.toString());
      }
    }

    throw new NotFoundException();
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

    DbWorkspace dbWorkspace = workspaceService.lookupWorkspaceByNamespace(workspaceNamespace);

    GceWithPdConfig gceWithPdConfig = runtime.getGceWithPdConfig();
    if (gceWithPdConfig != null) {
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
                    .map(leonardoMapper::toApiListDisksResponse)
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
          LeonardoLabelHelper.upsertLeonardoLabel(
              labels,
              LeonardoLabelHelper.LEONARDO_LABEL_IS_RUNTIME,
              LeonardoLabelHelper.LEONARDO_LABEL_IS_RUNTIME_TRUE);
      labels =
          LeonardoLabelHelper.upsertLeonardoLabel(
              labels, LeonardoLabelHelper.LEONARDO_LABEL_WORKSPACE_NAMESPACE, workspaceNamespace);
      labels =
          LeonardoLabelHelper.upsertLeonardoLabel(
              labels, LeonardoLabelHelper.LEONARDO_LABEL_WORKSPACE_NAME, dbWorkspace.getName());
      persistentDiskRequest.labels(labels);
    }

    DbUser user = userProvider.get();
    leonardoApiHelper.enforceComputeSecuritySuspension(user);

    String firecloudWorkspaceName = dbWorkspace.getFirecloudName();
    workspaceAuthService.enforceWorkspaceAccessLevel(
        workspaceNamespace, firecloudWorkspaceName, WorkspaceAccessLevel.WRITER);
    workspaceAuthService.validateActiveBilling(workspaceNamespace, firecloudWorkspaceName);

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
    workspaceAuthService.validateActiveBilling(workspaceNamespace, firecloudWorkspaceName);

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
      String workspaceNamespace, RuntimeLocalizeRequest body) {
    DbUser user = userProvider.get();
    leonardoApiHelper.enforceComputeSecuritySuspension(user);
    DbWorkspace dbWorkspace = workspaceService.lookupWorkspaceByNamespace(workspaceNamespace);
    workspaceAuthService.enforceWorkspaceAccessLevel(
        dbWorkspace.getWorkspaceNamespace(),
        dbWorkspace.getFirecloudName(),
        WorkspaceAccessLevel.WRITER);
    workspaceAuthService.validateActiveBilling(
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
                    body.isPlaygroundMode(),
                    true)));
  }
}
