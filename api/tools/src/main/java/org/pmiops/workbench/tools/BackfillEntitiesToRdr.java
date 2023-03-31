package org.pmiops.workbench.tools;

import com.google.cloud.tasks.v2.CloudTasksClient;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import javax.inject.Provider;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.pmiops.workbench.cloudtasks.CloudTasksConfig;
import org.pmiops.workbench.cloudtasks.TaskQueueService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchLocationConfigService;
import org.pmiops.workbench.db.dao.RdrExportDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.VerifiedInstitutionalAffiliationDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.model.RdrEntity;
import org.pmiops.workbench.rdr.RdrExportService;
import org.pmiops.workbench.rdr.RdrExportServiceImpl;
import org.pmiops.workbench.rdr.api.RdrApi;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * This will trigger a process to backfill the RDR database. This may be needed when new data is
 * being added to the RDR export. It will take all of the *unchanged* entities from the Workbench
 * and push them to the same task queue that is used for the nightly export, with the backfill flag
 * included. The backfill will not trigger a manual review on the RDR side, as it should only
 * consist of programmatic changes. The usual manual review will still take place for all modified
 * workspaces. This differs from InvalidateRdrExport in that it passes the backfill flag. Also, the
 * last_modified times would need to change in order for the RDR side to process the workspace if
 * the backfill flag is not set.
 */
@Import({
  CloudTasksConfig.class,
  WorkbenchLocationConfigService.class,
})
public class BackfillEntitiesToRdr extends Action {
  // I haven't read the entire commons cli code, but it looks like it is limited to the Number type,
  // we really want an Integer
  // https://github.com/apache/commons-cli/blob/98d06d37bc7058bbfb2704c9620669c66e279f4a/src/main/java/org/apache/commons/cli/PatternOptionBuilder.java#L98
  private static Option limitOpt =
      Option.builder()
          .longOpt("limit")
          .type(Number.class)
          .desc(
              "If specified, the tool will only export the number of entities specified by the"
                  + " limit")
          .hasArg()
          .build();
  private static Option entityTypeOpt =
      Option.builder()
          .longOpt("entity-type")
          .desc("the entity type to backfill")
          .required()
          .hasArg()
          .build();
  private static Option dryRunOpt =
      Option.builder()
          .longOpt("dry-run")
          .desc("When true, print the number of entities that will be exported, will not export")
          .build();

  private static Options options =
      new Options().addOption(entityTypeOpt).addOption(limitOpt).addOption(dryRunOpt);

  public static void main(String[] args) {
    CommandLineToolConfig.runCommandLine(BackfillEntitiesToRdr.class, args);
  }

  @Bean
  public RdrExportService rdrExportService(
      Clock clock,
      Provider<WorkbenchConfig> workbenchConfigProvider,
      Provider<RdrApi> rdrApiProvider,
      RdrExportDao rdrExportDao,
      WorkspaceDao workspaceDao,
      UserDao userDao,
      VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao) {
    return new RdrExportServiceImpl(
        clock,
        workbenchConfigProvider,
        rdrApiProvider,
        rdrExportDao,
        null,
        workspaceDao,
        null,
        null,
        null,
        userDao,
        verifiedInstitutionalAffiliationDao);
  }

  @Bean
  public TaskQueueService rdrTaskQueue(
      WorkbenchLocationConfigService locationConfigService,
      Provider<CloudTasksClient> cloudTasksClientProvider,
      Provider<WorkbenchConfig> configProvider) {
    return new TaskQueueService(
        locationConfigService, cloudTasksClientProvider, configProvider, () -> null);
  }

  @Bean
  public CommandLineRunner run(
      RdrExportService rdrExportService, TaskQueueService taskQueueService) {
    return (args) -> {
      CommandLine opts = new DefaultParser().parse(options, args);

      Optional<Integer> limit =
          Optional.ofNullable(opts.getParsedOptionValue(limitOpt))
              .map(opt -> ((Number) opt).intValue());
      RdrEntity entityType = RdrEntity.fromValue(opts.getOptionValue(entityTypeOpt).toUpperCase());
      boolean dryRun = opts.hasOption(dryRunOpt);

      // Only backfill the workspaces that have not changed. The changed workspaces will be handled
      // by the nightly cron job. This way changed workspaces won't slip past the manual review
      List<Long> idsToExport = rdrExportService.findUnchangedEntitiesForBackfill(entityType);
      if (limit.isPresent() && idsToExport.size() > limit.get()) {
        idsToExport = idsToExport.subList(0, limit.get());
      }
      if (dryRun) {
        System.out.printf(
            "\nThis is a dry run. %d %s entities will be exported when running this command.\n\n",
            idsToExport.size(), entityType);
      } else {
        if (RdrEntity.USER.equals(entityType)) {
          taskQueueService.groupAndPushRdrResearcherTasks(idsToExport, /* backfill */ true);
        } else if (RdrEntity.WORKSPACE.equals(entityType)) {
          taskQueueService.groupAndPushRdrWorkspaceTasks(idsToExport, /* backfill */ true);
        } else {
          throw new IllegalArgumentException("unknown entity type: " + entityType);
        }
        System.out.printf(
            "\n\n%d %s entities queued for export\n\n", idsToExport.size(), entityType);
      }
    };
  }
}
