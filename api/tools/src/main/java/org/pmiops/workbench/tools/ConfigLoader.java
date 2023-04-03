package org.pmiops.workbench.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.diff.JsonDiff;
import com.google.common.collect.ImmutableMap;
import java.util.logging.Logger;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfig;
import org.pmiops.workbench.config.FeaturedWorkspacesConfig;
import org.pmiops.workbench.config.FileConfigs;
import org.pmiops.workbench.config.FileConfigs.ConfigFormatException;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.ConfigDao;
import org.pmiops.workbench.db.model.DbConfig;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
/**
 * Command-line tool to load a WorkbenchConfig or CdrBigQuerySchemaConfig from a local file and
 * store it in the MySQL database for the current environment.
 *
 * <p>Run by api/project.rb update-cloud-config and (locally) docker-compose run api-scripts
 * ./gradlew loadConfig, which is automatically invoked during api/project.rb dev-up.
 */
public class ConfigLoader extends Tool {

  private static final Logger log = Logger.getLogger(ConfigLoader.class.getName());

  private static final ImmutableMap<String, Class<?>> CONFIG_CLASS_MAP =
      ImmutableMap.of(
          DbConfig.MAIN_CONFIG_ID,
          WorkbenchConfig.class,
          DbConfig.CDR_BIGQUERY_SCHEMA_CONFIG_ID,
          CdrBigQuerySchemaConfig.class,
          DbConfig.FEATURED_WORKSPACES_CONFIG_ID,
          FeaturedWorkspacesConfig.class);

  @Bean
  public CommandLineRunner run(ConfigDao configDao) {
    return (args) -> {
      if (args.length != 2) {
        throw new IllegalArgumentException("Expected arguments: <config key> <config file>");
      }
      String configKey = args[0];
      String configFile = args[1];

      Class<?> configClass = CONFIG_CLASS_MAP.get(configKey);
      if (configClass == null) {
        throw new IllegalArgumentException("Unrecognized config key: " + configKey);
      }

      JsonNode newJson = null;
      try {
        newJson = FileConfigs.loadConfig(configFile, configClass);
      } catch (ConfigFormatException e) {
        log.severe(
            String.format(
                "Configuration doesn't match {0} format; see diff.", configClass.getSimpleName()));
        log.severe(e.getJsonDiff().toString());
        System.exit(1);
      }

      DbConfig existingConfig = configDao.findById(configKey).orElse(null);
      if (existingConfig == null) {
        log.info("No configuration exists, creating one.");
        DbConfig config = new DbConfig();
        config.setConfigId(configKey);
        config.setConfiguration(newJson.toString());
        configDao.save(config);
      } else {
        ObjectMapper jackson = new ObjectMapper();
        JsonNode existingJson = jackson.readTree(existingConfig.getConfiguration());
        JsonNode diff = JsonDiff.asJson(existingJson, newJson);
        if (diff.size() == 0) {
          log.info("No change in configuration; exiting.");
        } else {
          log.info("Updating configuration:");
          log.info(diff.toString());
          existingConfig.setConfiguration(newJson.toString());
          configDao.save(existingConfig);
        }
      }
      log.info("Done.");
    };
  }

  public static void main(String[] args) throws Exception {
    CommandLineToolConfig.runCommandLine(ConfigLoader.class, args);
  }
}
