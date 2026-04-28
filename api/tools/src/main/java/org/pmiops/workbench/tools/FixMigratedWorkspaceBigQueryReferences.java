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
   * destinationWorkspaceId,sourceWorkspaceId,resourceId
   *
   * Example:
   *
   * destinationWorkspaceId,sourceWorkspaceId,resourceId
   * 1ba361fa-f2a2-42fd-b926-a91fe1ba7566,3d83ef80-77d7-43e8-a479-52946619b769,e761c0c3-8741-45f0-944c-d15e4cffb5ae
   *
   * destinationWorkspaceId = migrated VWB workspace
   * sourceWorkspaceId      = data collection workspace ID
   * resourceId             = incorrect controlled BigQuery dataset resource ID
   *
   * Run example:
   *
   * ./gradlew fixMigratedWorkspaceBigQueryReferences \
   *   -PappArgs='["/tmp/migrated_workspaces_bq_cleanup.csv"]'
   *
   */

  @Bean
  public CommandLineRunner run(WsmClient wsmClient) {
    return (args) -> {
      if (args.length != 1) {
        throw new IllegalArgumentException("Expected 1 argument: <csv_file_path>");
      }

      String inputFile = args[0];

      LOG.info("Starting BigQuery reference resource cleanup script...");
      LOG.info("Reading input CSV file: " + inputFile);

      try (CSVReader reader = new CSVReader(new FileReader(inputFile))) {

        String[] row;
        boolean skipHeader = true;
        int successCount = 0;
        int failureCount = 0;

        while ((row = reader.readNext()) != null) {

          if (skipHeader) {
            skipHeader = false;
            continue;
          }

          try {
            if (row.length < 3) {
              throw new IllegalArgumentException("Invalid CSV row. Expected 3 columns.");
            }

            UUID destinationWorkspaceId = UUID.fromString(row[0].trim());

            String sourceWorkspaceId = row[1].trim();

            UUID resourceId = UUID.fromString(row[2].trim());

            LOG.info(
                String.format(
                    "Processing destinationWorkspaceId=%s sourceWorkspaceId=%s resourceId=%s",
                    destinationWorkspaceId, sourceWorkspaceId, resourceId));

            /*
             * Delete incorrect CONTROLLED BigQuery dataset
             */
            LOG.info("Deleting incorrect controlled BigQuery dataset resource...");

            wsmClient.deleteResource(destinationWorkspaceId, resourceId);

            /*
             * Recreate using COPY_REFERENCE
             */
            LOG.info("Recreating BigQuery dataset as REFERENCE resource...");

            wsmClient.cloneBQDataset(
                destinationWorkspaceId,
                sourceWorkspaceId,
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

      LOG.info("BigQuery reference resource cleanup script finished.");
    };
  }

  public static void main(String[] args) throws Exception {
    CommandLineToolConfig.runCommandLine(FixMigratedWorkspaceBigQueryReferences.class, args);
  }
}
