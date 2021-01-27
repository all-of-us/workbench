package org.pmiops.workbench.tools.cdrConfig;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.model.ArchivalStatus;
import org.pmiops.workbench.tools.CommandLineToolConfig;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

/**
 * See api/project.rb update-cdr-config. Reads Access Tiers and CDR versions from a JSON file and
 * updates the database to match.
 */
@Configuration
public class UpdateCdrConfig {

  private static final Logger logger = Logger.getLogger(UpdateCdrConfig.class.getName());

  @Bean
  @Transactional
  public CommandLineRunner run(
      AccessTierDao accessTierDao, CdrVersionDao cdrVersionDao, WorkspaceDao workspaceDao)
      throws IOException {
    return (args) -> {
      if (args.length != 2) {
        throw new IllegalArgumentException(
            "Expected 2 args (file, dry_run). Got " + Arrays.asList(args));
      }

      // it's small and we're parsing it multiple times, so read it into memory
      final String configFile = new String(Files.readAllBytes(Paths.get(args[0])));
      boolean dryRun = Boolean.parseBoolean(args[1]);

      final Map<Long, Long> accessTierMap = generateAccessTierMap(configFile);

      // no adapters necessary - Gson can infer the structure of DbAccessTier

      final Gson accessTiersGson = new GsonBuilder().create();
      final AccessTierExtractor accessTiers =
          accessTiersGson.fromJson(configFile, AccessTierExtractor.class);

      // DbCdrVersion requires Gson adapters for Timestamp formatting and DbAccessTier resolution

      final Gson cdrVersionsGson =
          new GsonBuilder()
              .registerTypeAdapter(Timestamp.class, new TimestampGsonAdapter())
              .registerTypeAdapter(DbAccessTier.class, new DbAccessTierIdGsonAdapter(accessTierDao))
              .create();
      final CdrVersionExtractor cdrVersions =
          cdrVersionsGson.fromJson(configFile, CdrVersionExtractor.class);

      preCheck(accessTiers.accessTiers, cdrVersions.cdrVersions, accessTierMap);

      updateDB(
          dryRun,
          accessTiers,
          accessTiersGson,
          cdrVersions,
          cdrVersionsGson,
          accessTierMap,
          accessTierDao,
          cdrVersionDao);
    };
  }

  // return a map of CDR Version IDs to Access Tier IDs, for later DB resolution
  private Map<Long, Long> generateAccessTierMap(String configFile) {
    Gson gson = new GsonBuilder().create();

    final AccessTierMappingExtractor mapper =
        gson.fromJson(configFile, AccessTierMappingExtractor.class);

    Map<Long, Long> accessTierMap = Maps.newHashMap();
    mapper.cdrVersions.forEach(v -> accessTierMap.put(v.cdrVersionId, v.accessTier));
    return accessTierMap;
  }

  /**
   * Check the file for internal consistency
   *
   * <p>Access Tiers cannot duplicate IDs
   *
   * <p>CDR Versions cannot:
   *
   * <p>- duplicate IDs
   *
   * <p>- lack an ID
   *
   * <p>- have more than one default version
   *
   * <p>- have an archived default version
   *
   * <p>- belong to a tier which is not also present in this file
   */
  private void preCheck(
      List<DbAccessTier> accessTiers,
      List<DbCdrVersion> cdrVersions,
      Map<Long, Long> accessTierMap) {
    Set<Long> accessTierIds = new HashSet<>();
    accessTiers.forEach(
        t -> {
          long id = t.getAccessTierId();
          if (!accessTierIds.add(id)) {
            throw new IllegalArgumentException(
                String.format("Input JSON contains duplicated Access Tier ID %d", id));
          }
        });

    Set<Long> cdrVersionIds = new HashSet<>();
    Set<Long> cdrDefaultVersionIds = new HashSet<>();
    cdrVersions.forEach(
        v -> {
          long id = v.getCdrVersionId();
          if (id == 0) {
            throw new IllegalArgumentException(
                String.format("Input JSON CDR Version '%s' is missing an ID", v.getName()));
          }
          if (!cdrVersionIds.add(id)) {
            throw new IllegalArgumentException(
                String.format("Input JSON contains duplicated CDR Version ID %d", id));
          }

          if (v.getIsDefault()) {
            if (ArchivalStatus.LIVE != v.getArchivalStatusEnum()) {
              throw new IllegalArgumentException(
                  String.format("Archived CDR Version %d cannot also be the default", id));
            }

            cdrDefaultVersionIds.add(id);
          }

          long accessTierId = accessTierMap.get(v.getCdrVersionId());

          if (accessTierId == 0) {
            throw new IllegalArgumentException(
                String.format("CDR version %d is missing an Access Tier", id));
          }

          if (!accessTierIds.contains(accessTierId)) {
            throw new IllegalArgumentException(
                String.format(
                    "CDR version %d is a member of Access Tier %d which is not present in the input file",
                    id, accessTierId));
          }
        });

    if (cdrDefaultVersionIds.size() != 1) {
      throw new IllegalArgumentException(
          String.format(
              "Must be exactly one default CDR version, got %d: %s",
              cdrDefaultVersionIds.size(), Joiner.on(", ").join(cdrDefaultVersionIds)));
    }
  }

  /**
   * Update the DB from the input JSON
   *
   * <p>Apply updates in this order to ensure valid references:
   *
   * <p>1. Add new Access Tiers and update existing Access Tiers
   *
   * <p>2. Resolve CDR Version references to tiers
   *
   * <p>3. Add new CDR Versions and update existing CDR Versions
   *
   * <p>4. Remove old CDR Versions
   *
   * <p>5. Remove old Access Tiers
   */
  private void updateDB(
      boolean dryRun,
      AccessTierExtractor accessTiers,
      Gson accessTierGson,
      CdrVersionExtractor cdrVersions,
      Gson cdrVersionsGson,
      Map<Long, Long> accessTierMap,
      AccessTierDao accessTierDao,
      CdrVersionDao cdrVersionDao) {
    String dryRunSuffix = dryRun ? " (dry run)" : "";

    Map<Long, DbAccessTier> currentAccessTiers = Maps.newHashMap();
    for (DbAccessTier accessTier : accessTierDao.findAll()) {
      currentAccessTiers.put(accessTier.getAccessTierId(), accessTier);
    }

    // 1. Add new Access Tiers and update existing

    for (DbAccessTier accessTier : accessTiers.accessTiers) {
      DbAccessTier existingAccessTier = currentAccessTiers.remove(accessTier.getAccessTierId());
      if (existingAccessTier == null) {
        logger.info(
            String.format(
                "Inserting new Access Tier %d '%s'%s: %s",
                accessTier.getAccessTierId(),
                accessTier.getDisplayName(),
                dryRunSuffix,
                accessTierGson.toJson(accessTier)));
        if (!dryRun) {
          accessTierDao.save(accessTier);
        }
      } else {
        if (accessTier.equals(existingAccessTier)) {
          logger.info(
              String.format(
                  "Access Tier %d '%s' unchanged.",
                  accessTier.getAccessTierId(), accessTier.getDisplayName()));
        } else {
          logger.info(
              String.format(
                  "Updating AccessTier %d '%s'%s: %s",
                  accessTier.getAccessTierId(),
                  accessTier.getDisplayName(),
                  dryRunSuffix,
                  accessTierGson.toJson(accessTier)));
          if (!dryRun) {
            accessTierDao.save(accessTier);
          }
        }
      }
    }

    // 2. Resolve CDR Version references to tiers

    List<DbCdrVersion> resolvedCdrVersions =
        cdrVersions.cdrVersions.stream()
            .map(
                v -> {
                  v.setAccessTier(accessTierDao.findOne(accessTierMap.get(v.getCdrVersionId())));
                  return v;
                })
            .collect(Collectors.toList());

    // 3. Add new CDR Versions and update existing CDR Versions

    Map<Long, DbCdrVersion> currentCdrVersions = Maps.newHashMap();
    for (DbCdrVersion cdrVersion : cdrVersionDao.findAll()) {
      currentCdrVersions.put(cdrVersion.getCdrVersionId(), cdrVersion);
    }
    for (DbCdrVersion cdrVersion : resolvedCdrVersions) {
      DbCdrVersion existingCdrVersion = currentCdrVersions.remove(cdrVersion.getCdrVersionId());
      if (existingCdrVersion == null) {
        logger.info(
            String.format(
                "Inserting new CDR version %d '%s'%s: %s",
                cdrVersion.getCdrVersionId(),
                cdrVersion.getName(),
                dryRunSuffix,
                cdrVersionsGson.toJson(cdrVersion)));
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
                  cdrVersionsGson.toJson(cdrVersion)));
          if (!dryRun) {
            cdrVersionDao.save(cdrVersion);
          }
        }
      }
    }

    // 4. Remove old CDR Versions

    for (DbCdrVersion cdrVersion : currentCdrVersions.values()) {
      logger.info(
          String.format(
              "Deleting CDR version %d '%s' no longer in file%s: %s",
              cdrVersion.getCdrVersionId(),
              cdrVersion.getName(),
              dryRunSuffix,
              cdrVersionsGson.toJson(cdrVersion)));
      if (!dryRun) {
        // Note: this will fail if the database still has references to the CDR version being
        // deleted.
        cdrVersionDao.delete(cdrVersion.getCdrVersionId());
      }
    }

    // 5. Remove old Access Tiers

    for (DbAccessTier accessTier : currentAccessTiers.values()) {
      logger.info(
          String.format(
              "Deleting Access Tier %d '%s' no longer in file%s: %s",
              accessTier.getAccessTierId(),
              accessTier.getDisplayName(),
              dryRunSuffix,
              accessTierGson.toJson(accessTier)));
      if (!dryRun) {
        // Note: this will fail if the database still has references to the Access Tier being
        // deleted.
        accessTierDao.delete(accessTier.getAccessTierId());
      }
    }
  }

  public static void main(String[] args) throws Exception {
    CommandLineToolConfig.runCommandLine(UpdateCdrConfig.class, args);
  }
}
