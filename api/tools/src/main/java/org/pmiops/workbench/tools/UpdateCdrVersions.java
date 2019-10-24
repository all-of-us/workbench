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
import org.pmiops.workbench.db.model.CdrVersion;
import org.pmiops.workbench.model.ArchivalStatus;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * See api/project.rb update-cdr-versions. Reads CDR versions from a JSON file and updates the
 * database to match.
 */
@SpringBootApplication
@EnableJpaRepositories("org.pmiops.workbench.db.dao")
@EntityScan("org.pmiops.workbench.db.model")
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
      List<CdrVersion> cdrVersionEntities =
          gson.fromJson(cdrVersionsReader, new TypeToken<List<CdrVersion>>() {}.getType());

      Set<Long> ids = new HashSet<>();
      Set<Long> defaultIds = new HashSet<>();
      for (CdrVersion v : cdrVersionEntities) {
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

      Map<Long, CdrVersion> currentCdrVersions = Maps.newHashMap();
      for (CdrVersion cdrVersionEntity : cdrVersionDao.findAll()) {
        currentCdrVersions.put(cdrVersionEntity.getCdrVersionId(), cdrVersionEntity);
      }
      String dryRunSuffix = dryRun ? " (dry run)" : "";
      for (CdrVersion cdrVersionEntity : cdrVersionEntities) {
        CdrVersion existingCdrVersionEntity = currentCdrVersions.remove(cdrVersionEntity.getCdrVersionId());
        if (existingCdrVersionEntity == null) {
          logger.info(
              String.format(
                  "Inserting new CDR version %d '%s'%s: %s",
                  cdrVersionEntity.getCdrVersionId(),
                  cdrVersionEntity.getName(),
                  dryRunSuffix,
                  gson.toJson(cdrVersionEntity)));
          if (!dryRun) {
            cdrVersionDao.save(cdrVersionEntity);
          }
        } else {
          if (cdrVersionEntity.equals(existingCdrVersionEntity)) {
            logger.info(
                String.format(
                    "CDR version %d '%s' unchanged.",
                    cdrVersionEntity.getCdrVersionId(), cdrVersionEntity.getName()));
          } else {
            logger.info(
                String.format(
                    "Updating CDR version %d '%s'%s: %s",
                    cdrVersionEntity.getCdrVersionId(),
                    cdrVersionEntity.getName(),
                    dryRunSuffix,
                    gson.toJson(cdrVersionEntity)));
            if (!dryRun) {
              cdrVersionDao.save(cdrVersionEntity);
            }
          }
        }
      }
      for (CdrVersion cdrVersionEntity : currentCdrVersions.values()) {
        logger.info(
            String.format(
                "Deleting CDR version %d '%s' no longer in file%s: %s",
                cdrVersionEntity.getCdrVersionId(),
                cdrVersionEntity.getName(),
                dryRunSuffix,
                gson.toJson(cdrVersionEntity)));
        if (!dryRun) {
          // Note: this will fail if the database still has references to the CDR version being
          // deleted.
          cdrVersionDao.delete(cdrVersionEntity.getCdrVersionId());
        }
      }
    };
  }

  public static void main(String[] args) throws Exception {
    new SpringApplicationBuilder(UpdateCdrVersions.class).web(false).run(args);
  }
}
