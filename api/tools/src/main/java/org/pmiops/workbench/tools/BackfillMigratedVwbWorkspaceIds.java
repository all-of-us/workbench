package org.pmiops.workbench.tools;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.MigrationState;
import org.pmiops.workbench.vwb.wsm.WsmClient;
import org.pmiops.workbench.wsmanager.model.WorkspaceDescription;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BackfillMigratedVwbWorkspaceIds extends Tool {

  private static final Logger LOG =
      Logger.getLogger(BackfillMigratedVwbWorkspaceIds.class.getName());

  @Bean
  public CommandLineRunner run(WorkspaceDao workspaceDao, WsmClient wsmClient) {

    return (args) -> {
      LOG.info("Starting backfill for migrated_vwb_workspace_id...");

      List<DbWorkspace> migratedWorkspaces =
          StreamSupport.stream(workspaceDao.findAll().spliterator(), false)
              .filter(
                  ws ->
                      MigrationState.FINISHED.name().equals(ws.getMigrationState())
                          && ws.getMigratedVwbWorkspaceId() == null)
              .limit(1)
              .toList();

      LOG.info(
          String.format(
              "Found %s workspaces needing migrated_vwb_workspace_id backfill",
              migratedWorkspaces.size()));

      for (DbWorkspace workspace : migratedWorkspaces) {
        try {
          String namespace = workspace.getWorkspaceNamespace();

          LOG.info(String.format("Processing workspace namespace: %s", namespace));

          /*
           * RW 1.0 namespace == VWB UFID
           * So we use namespace to lookup the VWB workspace.
           */
          WorkspaceDescription vwbWorkspace = wsmClient.getWorkspaceAsService(namespace);

          if (vwbWorkspace == null || vwbWorkspace.getId() == null) {
            LOG.warning(String.format("No VWB workspace found for namespace: %s", namespace));
            continue;
          }

          String migratedWorkspaceId = vwbWorkspace.getId().toString();

          workspace.setMigratedVwbWorkspaceId(migratedWorkspaceId);
          workspace.setLastModifiedTime(Timestamp.from(Instant.now()));

          workspaceDao.save(workspace);

          LOG.info(
              String.format(
                  "Updated workspace %s with migrated_vwb_workspace_id = %s",
                  namespace, migratedWorkspaceId));

        } catch (Exception e) {
          LOG.severe(
              String.format(
                  "Failed processing workspace %s : %s",
                  workspace.getWorkspaceNamespace(), e.getMessage()));
        }
      }

      LOG.info("Backfill for migrated_vwb_workspace_id completed.");
    };
  }

  public static void main(String[] args) throws Exception {
    CommandLineToolConfig.runCommandLine(BackfillMigratedVwbWorkspaceIds.class, args);
  }
}
