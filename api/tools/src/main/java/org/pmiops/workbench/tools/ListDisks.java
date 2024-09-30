package org.pmiops.workbench.tools;

import static org.pmiops.workbench.leonardo.LeonardoConfig.SERVICE_DISKS_API;
import static org.pmiops.workbench.leonardo.LeonardoLabelHelper.LEONARDO_DISK_LABEL_KEYS;

import jakarta.inject.Provider;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.pmiops.workbench.firecloud.FirecloudApiClientFactory;
import org.pmiops.workbench.google.GoogleConfig;
import org.pmiops.workbench.leonardo.ApiException;
import org.pmiops.workbench.leonardo.LeonardoApiClientFactory;
import org.pmiops.workbench.leonardo.LeonardoConfig;
import org.pmiops.workbench.leonardo.api.DisksApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/** Retrieve details about all persistent disks in an environment. */
@Configuration
@Import({
  FirecloudApiClientFactory.class,
  GoogleConfig.class, // injects com.google.cloud.iam.credentials.v1.IamCredentialsClient
  LeonardoConfig.class,
  LeonardoApiClientFactory.class,
})
public class ListDisks extends Tool {

  private static final Logger log = Logger.getLogger(ListDisks.class.getName());

  // TODO
  private static Options options = new Options();

  @Bean
  public CommandLineRunner run(@Qualifier(SERVICE_DISKS_API) Provider<DisksApi> disksApiProvider) {
    return args -> {
      // project.rb swallows exceptions, so we need to catch and log them here
      try {
        listDisks(disksApiProvider.get(), new DefaultParser().parse(options, args));
      } catch (Exception e) {
        log.severe("Error: " + e.getMessage());
        e.printStackTrace();
      }
    };
  }

  private void listDisks(DisksApi disksApi, CommandLine opts) throws ApiException {
    log.severe(
        "Counting disks: "
            + disksApi
                .listDisks(
                    /* labels */ null, /* includeDeleted */ false, LEONARDO_DISK_LABEL_KEYS, null)
                .size());
  }

  public static void main(String[] args) {
    CommandLineToolConfig.runCommandLine(ListDisks.class, args);
  }
}
