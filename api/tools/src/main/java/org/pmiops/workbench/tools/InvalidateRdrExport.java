package org.pmiops.workbench.tools;

import java.io.BufferedReader;
import java.io.FileReader;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.RdrExportDao;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.VerifiedInstitutionalAffiliationDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbRdrExport;
import org.pmiops.workbench.db.model.RdrEntityEnums;
import org.pmiops.workbench.model.RdrEntity;
import org.pmiops.workbench.rdr.RdrExportService;
import org.pmiops.workbench.rdr.RdrExportServiceImpl;
import org.pmiops.workbench.rdr.api.RdrApi;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

/**
 * The Workbench sends recently created/modified entity (workspace or user) information to RDR
 * daily. The entities picked up for export are as follows:
 *
 * <p>1) entity id does not exist in rdr_export OR 2) entity lastModifiedTime > export_date_time in
 * rdr_export table
 *
 * <p>By removing entities from the rdr_export table, we invalidate our tracking and cause them to
 * be resent during the next RDR export. This may be necessary when new fields are added to the RDR
 * models, which should be backfilled for all existing data.
 *
 * <p>IMPORTANT: This mechanism may be used for RdrResaercher/users, but should rarely be used for
 * workspaces. For workspaces, we have implemented a separate mechanism: see
 * BackfillWorkspacesToRdr. Workspaces require special care as any update to an RDR workspace entity
 * results in manual review downstream. Users are not subject to this same process and therefore
 * have no special backfill support.
 *
 * <p>InvalidateFromRdrExport takes in the list of entity IDs (workspace or user database IDs) as an
 * argument and deletes them from rdr_export table.
 */
public class InvalidateRdrExport extends Action {
  private static Option idListFilenameOpt =
      Option.builder()
          .longOpt("id-list-filename")
          .desc("File containing list of entity ids (workspace or user database IDs)")
          .hasArg()
          .build();
  private static Option entityTypeOpt =
      Option.builder()
          .longOpt("entity-type")
          .desc("the entity type to migrate")
          .required()
          .hasArg()
          .build();
  private static Option dryRunOpt =
      Option.builder()
          .longOpt("dry-run")
          .desc("If specified, the tool runs in dry run mode; no modifications are made")
          .build();

  private static Options options =
      new Options().addOption(idListFilenameOpt).addOption(entityTypeOpt).addOption(dryRunOpt);

  public static void main(String[] args) {
    CommandLineToolConfig.runCommandLine(InvalidateRdrExport.class, args);
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
  public CommandLineRunner run(RdrExportService rdrExportService, RdrExportDao rdrExportDao) {
    AtomicInteger counter = new AtomicInteger();

    return (args) -> {
      CommandLine opts = new DefaultParser().parse(options, args);
      boolean dryRun = opts.hasOption(dryRunOpt.getLongOpt());

      RdrEntity entityType = RdrEntity.fromValue(opts.getOptionValue(entityTypeOpt).toUpperCase());

      List<Long> idsToDelete;
      if (opts.hasOption(idListFilenameOpt)) {
        try (BufferedReader reader =
            new BufferedReader(
                new FileReader(opts.getOptionValue(idListFilenameOpt.getLongOpt())))) {
          idsToDelete = reader.lines().map(Long::parseLong).collect(Collectors.toList());
        }
      } else {
        idsToDelete =
            rdrExportDao.findAllByEntityType(RdrEntityEnums.entityToStorage(entityType)).stream()
                .map(DbRdrExport::getEntityId)
                .collect(Collectors.toList());
      }

      String dryRunPrefix = dryRun ? "DRY RUN: " : "";
      System.out.printf(
          "%sStarting invalidation of %d %s entities\n",
          dryRunPrefix, idsToDelete.size(), entityType);
      idsToDelete.stream()
          .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / 20))
          .values()
          .parallelStream()
          .forEach(
              (idList) -> {
                System.out.printf(
                    "%sInvalidating the following %s IDs from the rdr_export table\n",
                    dryRunPrefix, entityType);
                idList.forEach(System.out::println);
                if (!dryRun) {
                  rdrExportService.deleteRdrExportEntries(entityType, idList);
                }
              });
    };
  }
}
