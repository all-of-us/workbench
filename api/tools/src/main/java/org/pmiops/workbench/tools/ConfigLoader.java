package org.pmiops.workbench.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.diff.JsonDiff;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;
import org.pmiops.workbench.config.CdrBigQuerySchemaConfig;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.ConfigDao;
import org.pmiops.workbench.db.model.Config;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories("org.pmiops.workbench.db.dao")
@EntityScan("org.pmiops.workbench.db.model")
/**
 * Run by api/project.rb update-cloud-config and (locally) docker-compose run update-config, which
 * is automatically invoked during api/project.rb dev-up.
 */
public class ConfigLoader {

  private static final Logger log = Logger.getLogger(ConfigLoader.class.getName());

  private static final ImmutableMap<String, Class<?>> CONFIG_CLASS_MAP =
      ImmutableMap.of(Config.MAIN_CONFIG_ID, WorkbenchConfig.class,
          Config.CDR_BIGQUERY_SCHEMA_CONFIG_ID, CdrBigQuerySchemaConfig.class);

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

      ObjectMapper jackson = new ObjectMapper();
      String rawJson = new String(Files.readAllBytes(Paths.get(configFile)), Charset.defaultCharset());
      // Strip all lines starting with '//'.
      String strippedJson = rawJson.replaceAll("\\s*//.*", "");
      JsonNode newJson = jackson.readTree(strippedJson);

      // Make sure the config parses to the appropriate configuration format,
      // and has the same representation after being marshalled back to JSON.
      Gson gson = new Gson();
      Object configObj = gson.fromJson(newJson.toString(), configClass);
      String marshalledJson = gson.toJson(configObj, configClass);
      JsonNode marshalledNode = jackson.readTree(marshalledJson);
      JsonNode marshalledDiff = JsonDiff.asJson(newJson, marshalledNode);
      if (marshalledDiff.size() > 0) {
        log.info(String.format("Configuration doesn't match {0} format; see diff.",
            configClass.getSimpleName()));
        log.info(marshalledDiff.toString());
        System.exit(1);
      }
      Config existingConfig = configDao.findOne(configKey);
      if (existingConfig == null) {
        log.info("No configuration exists, creating one.");
        Config config = new Config();
        config.setConfigId(configKey);
        config.setConfiguration(newJson.toString());
        configDao.save(config);
      } else {
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
    new SpringApplicationBuilder(ConfigLoader.class).web(false).run(args);
  }

}
