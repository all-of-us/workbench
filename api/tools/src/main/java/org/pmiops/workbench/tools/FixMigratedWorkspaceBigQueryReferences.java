package org.pmiops.workbench.tools;

import com.opencsv.CSVReader;
import java.io.FileReader;
import java.util.UUID;
import java.util.logging.Logger;
import org.pmiops.workbench.vwb.wsm.WsmClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FixMigratedWorkspaceBigQueryReferences extends Tool {

  private static final Logger LOG =
      Logger.getLogger(FixMigratedWorkspaceBigQueryReferences.class.getName());

  /*
   * CSV format:
   *
   * workspace_id,workspace_user_facing_id,resource_id
   *
   * Example:
   *
   * 1ba361fa-f2a2-42fd-b926-a91fe1ba7566,aou-rw-1748338f,192a366c-762b-4973-9949-xxxx
   *
   * Mapping:
   *
   * workspace_id              -> destinationWorkspaceId
   * workspace_user_facing_id  -> only for logging/reference
   * resource_id               -> incorrect controlled BigQuery dataset resource ID
   *
   * sourceWorkspaceId:
   *
   * Controlled Tier Data Collection workspace ID
   * confirmed by Yonghao:
   *
   * 3d83ef80-77d7-43e8-a479-52946619b769
   *
   * Run example:
   *
   * ./gradlew fixMigratedWorkspaceBigQueryReferences \
   *   -PappArgs='["/tmp/migrated_workspaces_bq_cleanup.csv"]'
   */

  private static final String SOURCE_WORKSPACE_ID = "3d83ef80-77d7-43e8-a479-52946619b769";

  @Bean
  public CommandLineRunner run(WsmClient wsmClient) {
    return (args) -> {
      if (args.length != 1) {
        throw new IllegalArgumentException("Expected 1 argument: <csv_file_path>");
      }

      String inputFile = args[0];

      LOG.info("Starting BigQuery reference resource cleanup...");
      LOG.info("Reading input CSV file: " + inputFile);
      LOG.info("Using sourceWorkspaceId (Data Collection): " + SOURCE_WORKSPACE_ID);

      int successCount = 0;
      int failureCount = 0;

      try (CSVReader reader = new CSVReader(new FileReader(inputFile))) {

        String[] row;
        boolean skipHeader = true;

        while ((row = reader.readNext()) != null) {

          if (skipHeader) {
            skipHeader = false;
            continue;
          }

          try {
            if (row.length < 3) {
              throw new IllegalArgumentException("Invalid CSV row. Expected at least 3 columns.");
            }

            UUID destinationWorkspaceId = UUID.fromString(row[0].trim());

            String workspaceNamespace = row[1].trim();

            UUID resourceId = UUID.fromString(row[2].trim());

            LOG.info(
                String.format(
                    "Processing workspace namespace=%s destinationWorkspaceId=%s resourceId=%s",
                    workspaceNamespace, destinationWorkspaceId, resourceId));

            /*
             * Step 1:
             * Delete incorrect CONTROLLED BigQuery dataset resource
             */
            LOG.info("Deleting incorrect controlled BigQuery dataset resource...");

            wsmClient.deleteResource(destinationWorkspaceId, resourceId);

            /*
             * Step 2:
             * Recreate using COPY_REFERENCE
             */
            LOG.info("Recreating BigQuery dataset as REFERENCE resource...");

            wsmClient.cloneBQDataset(
                destinationWorkspaceId,
                SOURCE_WORKSPACE_ID,
                resourceId,
                UUID.randomUUID().toString());

            LOG.info(String.format("Successfully fixed workspace: %s", destinationWorkspaceId));

            successCount++;

          } catch (Exception rowException) {
            failureCount++;

            LOG.severe(String.format("Failed processing CSV row: %s", rowException.getMessage()));
          }
        }

        LOG.info(
            String.format("Cleanup completed. Success=%s Failure=%s", successCount, failureCount));

      } catch (Exception e) {
        LOG.severe(String.format("Failed reading CSV file: %s", e.getMessage()));

        throw e;
      }

      LOG.info("BigQuery reference resource cleanup finished.");
    };
  }

  public static void main(String[] args) throws Exception {
    CommandLineToolConfig.runCommandLine(FixMigratedWorkspaceBigQueryReferences.class, args);
  }
}
