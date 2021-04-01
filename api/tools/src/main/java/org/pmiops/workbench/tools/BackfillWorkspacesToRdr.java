package org.pmiops.workbench.tools;

import java.math.BigInteger;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchLocationConfigService;
import org.pmiops.workbench.db.dao.RdrExportDao;
import org.pmiops.workbench.rdr.RdrTaskQueue;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * This will trigger a process to backfill the RDR database. This may be needed when new data is
 * being added to the RDR export. It will take all of the *unchanged* workspaces from the RDR and
 * push them to the same task queue that is used for the nightly export, adding the backfill flag to
 * the return endpoint. The backfill will not trigger a manual review on the RDR side, as it should
 * only consist of programmatic changes. The usual manual review will still take place for all
 * modified workspaces. This differs from DeleteFromRdrExport in that it does not take in a list of
 * IDS to export / backfill and does not modify the rdr_export table. Also, the last_modified times
 * would need to change in order for the RDR side to process the workspace if the backfill flag is
 * not set.
 */
@Import({
  WorkbenchLocationConfigService.class,
})
public class BackfillWorkspacesToRdr {
  private static final Logger log = Logger.getLogger(DeleteWorkspaces.class.getName());

  // I haven't read the entire commons cli code, but it looks like it is limited to the Number type,
  // we really want an Integer
  // https://github.com/apache/commons-cli/blob/98d06d37bc7058bbfb2704c9620669c66e279f4a/src/main/java/org/apache/commons/cli/PatternOptionBuilder.java#L98
  private static Option limitOpt =
      Option.builder()
          .longOpt("limit")
          .type(Number.class)
          .desc(
              "If specified, the tool will only export the number of workspaces specified by the"
                  + " limit")
          .hasArg()
          .build();

  private static Option dryRunOpt =
      Option.builder()
          .longOpt("dry-run")
          .desc("When true, print the number of workspaces that will be exported, will not export")
          .build();

  private static Options options = new Options().addOption(limitOpt).addOption(dryRunOpt);

  public static void main(String[] args) {
    CommandLineToolConfig.runCommandLine(BackfillWorkspacesToRdr.class, args);
  }

  @Bean
  private RdrTaskQueue rdrTaskQueue(
      WorkbenchLocationConfigService locationConfigService,
      Provider<WorkbenchConfig> configProvider) {
    return new RdrTaskQueue(locationConfigService, configProvider);
  }

  @Bean
  public CommandLineRunner run(RdrExportDao rdrExportDao, RdrTaskQueue rdrTaskQueue) {
    return (args) -> {
      CommandLine opts = new DefaultParser().parse(options, args);
      Integer limit = ((Number) opts.getParsedOptionValue("limit")).intValue();
      boolean dryRun = opts.hasOption(dryRunOpt.getLongOpt());

      // Only backfill the workspaces that have not changed. The changed workspaces will be handled
      // by the nightly cron job. This way changed workspaces won't slip past the manual review
      List<BigInteger> nativeWorkspaceListToExport =
          limit != null
              ? rdrExportDao.findTopUnchangedDbWorkspaceIds(limit)
              : rdrExportDao.findAllUnchangedDbWorkspaceIds();

      List<Long> workspaceListToExport =
          nativeWorkspaceListToExport.stream()
              .map(BigInteger::longValue)
              .collect(Collectors.toList());
      if (dryRun) {
        System.out.printf(
            "\nThis is a dry run. %d workspaces will be exported when running this command.\n\n",
            workspaceListToExport.size());
      } else {
        rdrTaskQueue.groupIdsAndPushTask(
            workspaceListToExport, RdrTaskQueue.EXPORT_USER_PATH + "?backfill=true");
        System.out.printf("\n\n%d workspaces queued for export\n\n", workspaceListToExport.size());
      }
    };
  }
}
