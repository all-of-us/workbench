package org.pmiops.workbench.tools;

import java.io.BufferedReader;
import java.io.FileReader;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Provider;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.pmiops.workbench.db.dao.RdrExportDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.VerifiedInstitutionalAffiliationDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.rdr.RdrExportService;
import org.pmiops.workbench.rdr.RdrExportServiceImpl;
import org.pmiops.workbench.rdr.api.RdrApi;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

/**
 * Workbench send recently created/modified workspace information to RDR daily. The workspaces
 * picked up for export are as follows:
 *
 * <p>1) workspace id does not exist in rdr_export OR 2) workspace lastModifiedTime >
 * export_date_time in rdr_export table
 *
 * <p>There are sometimes (maybe in case of bugs), where already exported workspace data are not in
 * sync between workbench and RDR . In such cases, we need to make the workspace eligible for
 * re-export. The best way to do that without manipulating real workspace data is to remove the
 * entry from rdr_export table.
 *
 * <p>DeleteFromRdrExport takes in the list of workspaceIds as an argument and deletes them from
 * rdr_export table
 */
public class DeleteFromRdrExport {
  private static final Logger log = Logger.getLogger(DeleteWorkspaces.class.getName());

  private static Option workspaceListFilename =
      Option.builder()
          .longOpt("workspace-list-filename")
          .desc("File containing list of workspaces Ids")
          .required()
          .hasArg()
          .build();
  private static Option dryRunOpt =
      Option.builder()
          .longOpt("dry-run")
          .desc("If specified, the tool runs in dry run mode; no modifications are made")
          .build();

  private static Options options =
      new Options().addOption(workspaceListFilename).addOption(dryRunOpt);

  public static void main(String[] args) {
    CommandLineToolConfig.runCommandLine(DeleteFromRdrExport.class, args);
  }

  @Bean
  public RdrExportService rdrExportService(
      Clock clock,
      Provider<RdrApi> rdrApiProvider,
      RdrExportDao rdrExportDao,
      WorkspaceDao workspaceDao,
      UserDao userDao,
      VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao) {
    return new RdrExportServiceImpl(
        clock,
        rdrApiProvider,
        rdrExportDao,
        null,
        workspaceDao,
        null,
        null,
        userDao,
        verifiedInstitutionalAffiliationDao);
  }

  @Bean
  public CommandLineRunner run(RdrExportService rdrExportService) {
    AtomicInteger counter = new AtomicInteger();

    return (args) -> {
      CommandLine opts = new DefaultParser().parse(options, args);
      boolean dryRun = opts.hasOption(dryRunOpt.getLongOpt());
      try (BufferedReader reader =
          new BufferedReader(
              new FileReader(opts.getOptionValue(workspaceListFilename.getLongOpt())))) {
        List<Long> workspaceIdListToDelete = new ArrayList<Long>();
        reader
            .lines()
            .forEach(
                line -> {
                  workspaceIdListToDelete.addAll(
                      Stream.of(line.split(",")).map(Long::parseLong).collect(Collectors.toList()));
                });
        Collection<List<Long>> result =
            workspaceIdListToDelete.stream()
                .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / 20))
                .values();
        result
            .parallelStream()
            .forEach(
                (workspaceIdList) -> {
                  if (!dryRun) {
                    System.out.println("Deleting following workspace Ids from rdr_export");
                    workspaceIdList.forEach(System.out::println);
                    rdrExportService.deleteWorkspaceExportEntries(workspaceIdList);
                  } else {
                    System.out.println(
                        "Dry RUN TRUE: Deleting following workspace Ids from rdr_export");
                    workspaceIdList.forEach(System.out::println);
                  }
                });
      }
    };
  }
}
