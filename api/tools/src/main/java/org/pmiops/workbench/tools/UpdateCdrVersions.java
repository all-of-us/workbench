package org.pmiops.workbench.tools;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.model.ArchivalStatus;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * See api/project.rb update-cdr-versions. Reads CDR versions from a JSON file and updates the
 * database to match.
 */
@Configuration
public class UpdateCdrVersions {

  private static final Logger logger = Logger.getLogger(UpdateCdrVersions.class.getName());

  @Bean
  public CommandLineRunner run(CdrVersionDao cdrVersionDao, WorkspaceDao workspaceDao)
      throws IOException {
    return (args) -> {
      if (args.length != 2) {
        throw new IllegalArgumentException(
            "Expected 2 args (file, dry_run). Got " + Arrays.asList(args));
      }
      boolean dryRun = Boolean.parseBoolean(args[1]);
      FileReader cdrVersionsReader = new FileReader(args[0]);
      Gson gson =
          new GsonBuilder()
              .registerTypeAdapter(Timestamp.class, new TimestampGsonAdapter())
              .create();
      List<DbCdrVersion> cdrVersions =
          gson.fromJson(cdrVersionsReader, new TypeToken<List<DbCdrVersion>>() {}.getType());

      Set<Long> ids = new HashSet<>();
      Set<Long> defaultIds = new HashSet<>();
      for (DbCdrVersion v : cdrVersions) {
        if (v.getCdrVersionId() == 0) {
          throw new IllegalArgumentException(
              String.format("Input JSON CDR version '%s' is missing an ID", v.getName()));
        }
        if (!ids.add(v.getCdrVersionId())) {
          throw new IllegalArgumentException(
              String.format(
                  "Input JSON contains duplicated CDR version ID %d", v.getCdrVersionId()));
        }
        if (v.getIsDefault()) {
          if (ArchivalStatus.LIVE != v.getArchivalStatusEnum()) {
            throw new IllegalArgumentException(
                String.format(
                    "Archived CDR version cannot also be the default", v.getCdrVersionId()));
          }
          defaultIds.add(v.getCdrVersionId());
        }
      }
      if (defaultIds.size() != 1) {
        throw new IllegalArgumentException(
            String.format(
                "Must be exactly one default CDR version, got %d: %s",
                defaultIds.size(), Joiner.on(", ").join(defaultIds)));
      }

      Map<Long, DbCdrVersion> currentCdrVersions = Maps.newHashMap();
      for (DbCdrVersion cdrVersion : cdrVersionDao.findAll()) {
        currentCdrVersions.put(cdrVersion.getCdrVersionId(), cdrVersion);
      }
      String dryRunSuffix = dryRun ? " (dry run)" : "";
      for (DbCdrVersion cdrVersion : cdrVersions) {
        DbCdrVersion existingCdrVersion = currentCdrVersions.remove(cdrVersion.getCdrVersionId());
        if (existingCdrVersion == null) {
          logger.info(
              String.format(
                  "Inserting new CDR version %d '%s'%s: %s",
                  cdrVersion.getCdrVersionId(),
                  cdrVersion.getName(),
                  dryRunSuffix,
                  gson.toJson(cdrVersion)));
          if (!dryRun) {
            cdrVersionDao.save(cdrVersion);
          }
        } else {
          if (cdrVersion.equals(existingCdrVersion)) {
            logger.info(
                String.format(
                    "CDR version %d '%s' unchanged.",
                    cdrVersion.getCdrVersionId(), cdrVersion.getName()));
          } else {
            logger.info(
                String.format(
                    "Updating CDR version %d '%s'%s: %s",
                    cdrVersion.getCdrVersionId(),
                    cdrVersion.getName(),
                    dryRunSuffix,
                    gson.toJson(cdrVersion)));
            if (!dryRun) {
              cdrVersionDao.save(cdrVersion);
            }
          }
        }
      }
      for (DbCdrVersion cdrVersion : currentCdrVersions.values()) {
        logger.info(
            String.format(
                "Deleting CDR version %d '%s' no longer in file%s: %s",
                cdrVersion.getCdrVersionId(),
                cdrVersion.getName(),
                dryRunSuffix,
                gson.toJson(cdrVersion)));
        if (!dryRun) {
          // Note: this will fail if the database still has references to the CDR version being
          // deleted.
          cdrVersionDao.delete(cdrVersion.getCdrVersionId());
        }
      }
    };
  }

  public static void main(String[] args) throws Exception {
    new SpringApplicationBuilder(CommandLineToolConfig.class)
        .child(UpdateCdrVersions.class)
        .web(false)
        .run(args);
  }
}
