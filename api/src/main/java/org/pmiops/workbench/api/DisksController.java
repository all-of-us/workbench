package org.pmiops.workbench.api;

import org.pmiops.workbench.actionaudit.auditors.LeonardoRuntimeAuditor;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserRecentResourceService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.leonardo.model.*;
import org.pmiops.workbench.model.*;
import org.pmiops.workbench.model.Runtime;
import org.pmiops.workbench.notebooks.LeonardoNotebooksClient;
import org.pmiops.workbench.utils.mappers.LeonardoMapper;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Provider;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
public class DisksController implements DisksApiDelegate{
    private static final Logger log = Logger.getLogger(DisksController.class.getName());
    private static final String DISK_NAME_PREFIX = "all-of-us-pd-";
    private final LeonardoNotebooksClient leonardoNotebooksClient;
    private final Provider<DbUser> userProvider;
    private final WorkspaceDao workspaceDao;
    private final LeonardoMapper leonardoMapper;

    @Autowired
    public DisksController(LeonardoNotebooksClient leonardoNotebooksClient, Provider<DbUser> userProvider, WorkspaceDao workspaceDao, LeonardoMapper leonardoMapper){
        this.leonardoNotebooksClient = leonardoNotebooksClient;
        this.userProvider = userProvider;
        this.workspaceDao = workspaceDao;
        this.leonardoMapper = leonardoMapper;
    }
    private DbWorkspace lookupWorkspace(String workspaceNamespace) throws org.pmiops.workbench.exceptions.NotFoundException {
        return workspaceDao
                .getByNamespace(workspaceNamespace)
                .orElseThrow(() -> new NotFoundException("Workspace not found: " + workspaceNamespace));
    }
    @Override
    public ResponseEntity<EmptyResponse> createDisk(String workspaceNamespace, Disk disk) {
        DbWorkspace dbWorkspace = lookupWorkspace(workspaceNamespace);
        String firecloudWorkspaceName = dbWorkspace.getFirecloudName();
//        workspaceAuthService.enforceWorkspaceAccessLevel(
//                workspaceNamespace, firecloudWorkspaceName, WorkspaceAccessLevel.WRITER);
//        workspaceAuthService.validateActiveBilling(workspaceNamespace, firecloudWorkspaceName);
        disk.setName(DISK_NAME_PREFIX + userProvider.get().getUserId());

        leonardoNotebooksClient.createPersistentDisk(dbWorkspace.getGoogleProject(), disk.getName(),disk.getSize(), LeonardoDiskType.fromValue(disk.getDiskType().toString()), disk.getBlockSize());
        return ResponseEntity.ok(new EmptyResponse());
    }

    @Override
    public ResponseEntity<Disk> getDisk(String workspaceNamespace) {
        DbWorkspace dbWorkspace = lookupWorkspace(workspaceNamespace);
        String googleProject = dbWorkspace.getGoogleProject();
        try {
            LeonardoGetPersistentDiskResponse response =
                    leonardoNotebooksClient.getPersistentDisk(workspaceNamespace, DISK_NAME_PREFIX + userProvider.get().getUserId());
            if (LeonardoRuntimeStatus.ERROR.equals(response.getStatus())) {
                log.warning(
                        String.format(
                                "Observed Leonardo runtime with unexpected error status:\n%s",
                                response.getStatus()));
            }
            Disk disk = new Disk();
            disk.name(DISK_NAME_PREFIX + userProvider.get().getUserId());
            disk.diskType(DiskType.fromValue(response.getDiskType().toString()));
            disk.blockSize(response.getBlockSize());
            disk.size(response.getSize());
//            disk.status(DiskStatus.valueOf(response.getStatus().toString()));

            return ResponseEntity.ok(disk);
//            return ResponseEntity.ok(leonardoMapper.toApiRuntime(response));
        } catch (NotFoundException e) {
            return (ResponseEntity<Disk>) ResponseEntity.notFound();
        }
    }

    @Override
    public ResponseEntity<EmptyResponse> deleteDisk(String workspaceNamespace) {
        DbWorkspace dbWorkspace = lookupWorkspace(workspaceNamespace);
//        String firecloudWorkspaceName = dbWorkspace.getFirecloudName();
//        workspaceAuthService.enforceWorkspaceAccessLevel(
//                workspaceNamespace, firecloudWorkspaceName, WorkspaceAccessLevel.WRITER);

        leonardoNotebooksClient.deletePersistentDisk(
                dbWorkspace.getGoogleProject(), DISK_NAME_PREFIX + userProvider.get().getUserId());
        return ResponseEntity.ok(new EmptyResponse());
    }

    private Runtime getOverrideFromListRuntimes(String googleProject) {
        Optional<LeonardoListRuntimeResponse> mostRecentRuntimeMaybe =
                leonardoNotebooksClient.listRuntimesByProject(googleProject, true).stream()
                        .sorted(
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
                                })
                        .findFirst();

        if (!mostRecentRuntimeMaybe.isPresent()) {
            throw new NotFoundException();
        }

        LeonardoListRuntimeResponse mostRecentRuntime = mostRecentRuntimeMaybe.get();

        @SuppressWarnings("unchecked")
        Map<String, String> runtimeLabels = (Map<String, String>) mostRecentRuntime.getLabels();

        if (runtimeLabels != null
                && LeonardoMapper.RUNTIME_CONFIGURATION_TYPE_ENUM_TO_STORAGE_MAP
                .values()
                .contains(runtimeLabels.get(LeonardoMapper.RUNTIME_LABEL_AOU_CONFIG))) {
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
}
