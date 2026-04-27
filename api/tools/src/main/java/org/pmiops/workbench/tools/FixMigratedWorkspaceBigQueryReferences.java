package org.pmiops.workbench.tools;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.MigrationState;
import org.pmiops.workbench.vwb.wsm.WsmClient;
import org.pmiops.workbench.wsmanager.model.ResourceDescription;
import org.pmiops.workbench.wsmanager.model.ResourceType;
import org.pmiops.workbench.wsmanager.model.StewardshipType;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FixMigratedWorkspaceBigQueryReferences extends Tool {

  private static final Logger LOG =
      Logger.getLogger(FixMigratedWorkspaceBigQueryReferences.class.getName());

  @Bean
  public CommandLineRunner run(WorkspaceDao workspaceDao, WsmClient wsmClient) {

    return (args) -> {
      LOG.info("Starting cleanup for migrated workspaces with incorrect BigQuery resources...");

      List<DbWorkspace> migratedWorkspaces =
          StreamSupport.stream(workspaceDao.findAll().spliterator(), false)
              .filter(ws -> MigrationState.FINISHED.name().equals(ws.getMigrationState()))
              .toList();

      LOG.info("Found " + migratedWorkspaces.size() + " migrated workspaces");

      for (DbWorkspace workspace : migratedWorkspaces) {
        try {
          String namespace = workspace.getWorkspaceNamespace();

          LOG.info("Processing workspace: " + namespace);

          // Step 1:
          // Backfill migrated_vwb_workspace_id if missing
          if (workspace.getMigratedVwbWorkspaceId() == null) {
            UUID vwbWorkspaceId = wsmClient.getMigratedWorkspaceId(namespace);

            if (vwbWorkspaceId == null) {
              LOG.warning(
                  String.format(
                      "Skipping workspace %s because VWB workspace was not found", namespace));
              continue;
            }

            workspace.setMigratedVwbWorkspaceId(vwbWorkspaceId.toString());
            workspaceDao.save(workspace);

            LOG.info(
                String.format(
                    "Backfilled migratedVwbWorkspaceId for %s -> %s", namespace, vwbWorkspaceId));
          }

          UUID destinationWorkspaceId = UUID.fromString(workspace.getMigratedVwbWorkspaceId());

          LOG.info(String.format("Using migrated VWB workspaceId: %s", destinationWorkspaceId));

          // Step 2:
          // Get workspace resources from WSM
          List<ResourceDescription> resources =
              wsmClient.getWorkspaceResources(destinationWorkspaceId);

          Optional<ResourceDescription> controlledBigQueryDataset =
              resources.stream()
                  .filter(
                      resource ->
                          resource.getMetadata() != null
                              && resource.getMetadata().getResourceType()
                                  == ResourceType.BIG_QUERY_DATASET
                              && resource.getMetadata().getStewardshipType()
                                  == StewardshipType.CONTROLLED)
                  .findFirst();

          if (controlledBigQueryDataset.isEmpty()) {
            LOG.info(
                String.format(
                    "No incorrect controlled BigQuery dataset found for workspace %s", namespace));
            continue;
          }

          ResourceDescription resource = controlledBigQueryDataset.get();
          UUID resourceId = resource.getMetadata().getResourceId();

          LOG.info(
              String.format(
                  "Found incorrect controlled BigQuery dataset resource: %s", resourceId));

          // Step 3:
          // Delete incorrect controlled resource
          wsmClient.deleteResource(destinationWorkspaceId, resourceId);

          LOG.info(
              String.format("Deleted incorrect controlled resource for workspace %s", namespace));

          // Step 4:
          // Recreate using cloneBigQueryDataset with COPY_REFERENCE
          String jobId = UUID.randomUUID().toString();

          wsmClient.cloneBQDataset(
              destinationWorkspaceId, destinationWorkspaceId.toString(), resourceId, jobId);

          LOG.info(
              String.format("Recreated BigQuery dataset as REFERENCE for workspace %s", namespace));

          LOG.info(String.format("Cleanup completed for workspace: %s", namespace));

        } catch (Exception e) {
          LOG.severe(
              String.format(
                  "Failed processing workspace %s: %s",
                  workspace.getWorkspaceNamespace(), e.getMessage()));
        }
      }

      LOG.info("Cleanup script finished.");
    };
  }

  public static void main(String[] args) {
    CommandLineToolConfig.runCommandLine(FixMigratedWorkspaceBigQueryReferences.class, args);
  }
}
