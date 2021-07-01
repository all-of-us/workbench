package org.pmiops.workbench.tools.cdrconfig;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.apache.commons.lang3.StringUtils;
import org.pmiops.workbench.db.dao.AccessTierDao;
import org.pmiops.workbench.db.dao.CdrVersionDao;
import org.pmiops.workbench.db.model.DbAccessTier;
import org.pmiops.workbench.db.model.DbCdrVersion;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.model.ArchivalStatus;
import org.pmiops.workbench.tools.CommandLineToolConfig;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/**
 * See api/project.rb update-cdr-config. Reads Access Tiers and CDR versions from a JSON file and
 * updates the database to match.
 */
@Configuration
@Import(CdrConfigVOMapperImpl.class)
public class UpdateCdrConfig {

  private static final Logger logger = Logger.getLogger(UpdateCdrConfig.class.getName());

  @Bean
  @Transactional
  public CommandLineRunner run(
      AccessTierDao accessTierDao, CdrVersionDao cdrVersionDao, CdrConfigVOMapper cdrConfigMapper)
      throws IOException {
    return (args) -> {
      if (args.length != 2) {
        throw new IllegalArgumentException(
            "Expected 2 args (file, dry_run). Got " + Arrays.asList(args));
      }

      final Gson gson =
          new GsonBuilder()
              .registerTypeAdapter(Timestamp.class, new TimestampGsonAdapter())
              .create();
      final CdrConfigVO cdrConfig;
      try (final FileReader cdrConfigReader = new FileReader(args[0])) {
        cdrConfig = gson.fromJson(cdrConfigReader, CdrConfigVO.class);
      }
      boolean dryRun = Boolean.parseBoolean(args[1]);

      preCheck(cdrConfig);

      System.out.println("~~~~~~~~~~~~~");
      System.out.println("~~~~~~~~~~~~~");
      System.out.println("~~~~~~~~~~~~~");
      System.out.println("~~~~~~~~~~~~~");
      System.out.println(cdrConfig);

      updateDB(dryRun, cdrConfig, gson, accessTierDao, cdrVersionDao, cdrConfigMapper);
    };
  }

  /**
   * Check the file for internal consistency
   *
   * <p>Access Tiers cannot:
   *
   * <ul>
   *   <li>duplicate IDs (or lack one)
   *   <li>duplicate shortNames (or lack one)
   *   <li>duplicate displayNames (or lack one)
   * </ul>
   *
   * <p>CDR Versions cannot:
   *
   * <ul>
   *   <li>duplicate IDs (or lack one)
   *   <li>have an archived default version
   *   <li>belong to a tier which is not also present in this file
   *   <li>have more or less than one default version per tier
   * </ul>
   */
  private void preCheck(CdrConfigVO cdrConfig) {
    Set<Long> accessTierIds = new HashSet<>();
    Set<String> accessTierShortNames = new HashSet<>();
    Set<String> accessTierDisplayNames = new HashSet<>();
    for (AccessTierVO t : cdrConfig.accessTiers) {
      final long id = t.accessTierId;
      if (id == 0) {
        throw new IllegalArgumentException("Input JSON contains Access Tier without an ID");
      }
      if (!accessTierIds.add(id)) {
        throw new IllegalArgumentException(
            String.format("Input JSON contains duplicated Access Tier ID %d", id));
      }

      final String shortName = t.shortName;
      if (StringUtils.isBlank(shortName)) {
        throw new IllegalArgumentException("Input JSON contains Access Tier without a shortName");
      }
      if (!accessTierShortNames.add(shortName)) {
        throw new IllegalArgumentException(
            String.format("Input JSON contains duplicated Access Tier shortName '%s'", shortName));
      }

      final String displayName = t.displayName;
      if (StringUtils.isBlank(displayName)) {
        throw new IllegalArgumentException("Input JSON contains Access Tier without a displayName");
      }
      if (!accessTierDisplayNames.add(displayName)) {
        throw new IllegalArgumentException(
            String.format(
                "Input JSON contains duplicated Access Tier displayName '%s'", displayName));
      }
    }

    Set<Long> cdrVersionIds = new HashSet<>();
    Map<String, Long> cdrDefaultVersionPerTier = new HashMap<>();
    for (CdrVersionVO v : cdrConfig.cdrVersions) {
      long id = v.cdrVersionId;
      if (id == 0) {
        throw new IllegalArgumentException(
            String.format("Input JSON CDR Version '%s' is missing an ID", v.name));
      }
      if (!cdrVersionIds.add(id)) {
        throw new IllegalArgumentException(
            String.format("Input JSON contains duplicated CDR Version ID %d", id));
      }

      String accessTier = v.accessTier;
      if (StringUtils.isBlank(accessTier)) {
        throw new IllegalArgumentException(
            String.format("CDR version %d is missing an Access Tier", id));
      }

      if (!accessTierShortNames.contains(accessTier)) {
        throw new IllegalArgumentException(
            String.format(
                "CDR version %d is a member of Access Tier '%s' which is not present in the input file",
                id, accessTier));
      }

      if (v.isDefault != null && v.isDefault) {
        if (v.archivalStatus != DbStorageEnums.archivalStatusToStorage(ArchivalStatus.LIVE)) {
          throw new IllegalArgumentException(
              String.format("Archived CDR Version %d cannot be the default", id));
        }

        if (cdrDefaultVersionPerTier.containsKey(v.accessTier)) {
          throw new IllegalArgumentException(
              String.format(
                  "Must be exactly one default CDR version for Access Tier '%s'. Attempted to set both %d and %d as default versions.",
                  v.accessTier, cdrDefaultVersionPerTier.get(v.accessTier), v.cdrVersionId));
        } else {
          cdrDefaultVersionPerTier.put(v.accessTier, v.cdrVersionId);
        }
      }
    }

    accessTierShortNames.forEach(
        t -> {
          if (!cdrDefaultVersionPerTier.containsKey(t)) {
            throw new IllegalArgumentException(
                String.format("Missing default CDR version for Access Tier '%s'.", t));
          }
        });
  }

  /**
   * Update the DB from the input JSON
   *
   * <p>Apply updates in this order to ensure valid references:
   *
   * <ol>
   *   <li>Add new Access Tiers and update existing Access Tiers
   *   <li>Add new CDR Versions and update existing CDR Versions
   *   <li>Remove old CDR Versions
   *   <li>Remove old Access Tiers
   * </ol>
   */
  private void updateDB(
      boolean dryRun,
      CdrConfigVO cdrConfig,
      Gson gson,
      AccessTierDao accessTierDao,
      CdrVersionDao cdrVersionDao,
      CdrConfigVOMapper cdrConfigMapper) {
    String dryRunSuffix = dryRun ? " (dry run)" : "";

    List<DbAccessTier> accessTiers = cdrConfigMapper.accessTiers(cdrConfig);

    Map<Long, DbAccessTier> currentAccessTiers = Maps.newHashMap();
    for (DbAccessTier accessTier : accessTierDao.findAll()) {
      currentAccessTiers.put(accessTier.getAccessTierId(), accessTier);
    }

    // Add new Access Tiers and update existing

    for (DbAccessTier accessTier : accessTiers) {
      DbAccessTier existingAccessTier = currentAccessTiers.remove(accessTier.getAccessTierId());
      if (existingAccessTier == null) {
        logger.info(
            String.format(
                "Inserting new Access Tier %d '%s'%s: %s",
                accessTier.getAccessTierId(),
                accessTier.getDisplayName(),
                dryRunSuffix,
                gson.toJson(accessTier)));
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
                  gson.toJson(accessTier)));
          if (!dryRun) {
            accessTierDao.save(accessTier);
          }
        }
      }
    }

    // Add new CDR Versions and update existing CDR Versions

    Map<Long, DbCdrVersion> currentCdrVersions = Maps.newHashMap();
    for (DbCdrVersion cdrVersion : cdrVersionDao.findAll()) {
      currentCdrVersions.put(cdrVersion.getCdrVersionId(), cdrVersion);
    }

    List<DbCdrVersion> cdrVersions = cdrConfigMapper.cdrVersions(cdrConfig, accessTierDao);

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

    // Remove old CDR Versions

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
        cdrVersionDao.deleteById(cdrVersion.getCdrVersionId());
      }
    }

    // Remove old Access Tiers

    for (DbAccessTier accessTier : currentAccessTiers.values()) {
      logger.info(
          String.format(
              "Deleting Access Tier %d '%s' no longer in file%s: %s",
              accessTier.getAccessTierId(),
              accessTier.getDisplayName(),
              dryRunSuffix,
              gson.toJson(accessTier)));
      if (!dryRun) {
        // Note: this will fail if the database still has references to the Access Tier being
        // deleted.
        accessTierDao.deleteById(accessTier.getAccessTierId());
      }
    }
  }

  public static void main(String[] args) throws Exception {
    CommandLineToolConfig.runCommandLine(UpdateCdrConfig.class, args);
  }
}
