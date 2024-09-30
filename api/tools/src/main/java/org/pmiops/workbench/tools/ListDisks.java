package org.pmiops.workbench.tools;

import static org.pmiops.workbench.leonardo.LeonardoConfig.SERVICE_DISKS_API;
import static org.pmiops.workbench.leonardo.LeonardoLabelHelper.LEONARDO_DISK_LABEL_KEYS;

import com.opencsv.CSVWriter;
import jakarta.inject.Provider;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FirecloudApiClientFactory;
import org.pmiops.workbench.google.GoogleConfig;
import org.pmiops.workbench.leonardo.ApiException;
import org.pmiops.workbench.leonardo.LeonardoApiClientFactory;
import org.pmiops.workbench.leonardo.LeonardoConfig;
import org.pmiops.workbench.leonardo.LeonardoLabelHelper;
import org.pmiops.workbench.leonardo.api.DisksApi;
import org.pmiops.workbench.leonardo.model.LeonardoListPersistentDiskResponse;
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

  private static Option outputFileOpt =
      Option.builder().longOpt("output").desc("Output file name").required().hasArg().build();

  private static Options options = new Options().addOption(outputFileOpt);

  // Leonardo supports Azure but we do not, so we know this is always a Google Project
  private String getGoogleProject(LeonardoListPersistentDiskResponse response) {
    return response.getCloudContext().getCloudResource();
  }

  private ListDisksRow toDiskRow(
      LeonardoListPersistentDiskResponse response, Optional<DbWorkspace> dbWorkspace) {
    String name = response.getName();
    String environmentType = LeonardoLabelHelper.getEnvironmentType(response.getLabels());
    String sizeInGb = response.getSize().toString();
    String googleProject = getGoogleProject(response);
    String status = response.getStatus().toString();
    String creator = response.getAuditInfo().getCreator();
    String createdDate = response.getAuditInfo().getCreatedDate();
    String dateAccessed = response.getAuditInfo().getDateAccessed();

    String workspaceNamespace =
        dbWorkspace.map(DbWorkspace::getWorkspaceNamespace).orElse("[missing workspace]");
    String workspaceDisplayName =
        dbWorkspace.map(DbWorkspace::getName).orElse("[missing workspace]");
    String workspaceTerraName =
        dbWorkspace.map(DbWorkspace::getFirecloudName).orElse("[missing workspace]");
    String workspaceCreator =
        dbWorkspace
            .map(DbWorkspace::getCreator)
            .map(DbUser::getUsername)
            .orElse("[missing workspace]");

    return new ListDisksRow(
        name,
        environmentType,
        sizeInGb,
        googleProject,
        status,
        creator,
        createdDate,
        dateAccessed,
        workspaceNamespace,
        workspaceDisplayName,
        workspaceTerraName,
        workspaceCreator);
  }

  private void listDisks(DisksApi disksApi, WorkspaceDao workspaceDao, CommandLine opts)
      throws ApiException, IOException {
    String outputFile = opts.getOptionValue(outputFileOpt.getLongOpt());

    log.info("Step 1 of 3: Retrieving disk information from Leonardo");

    List<LeonardoListPersistentDiskResponse> disks =
        disksApi.listDisks(
            /* labels */ null, /* includeDeleted */ false, LEONARDO_DISK_LABEL_KEYS, null);

    log.info("Step 2 of 3: Associating disk information with RWB workspaces");

    Set<String> googleProjects =
        disks.stream().map(this::getGoogleProject).collect(Collectors.toSet());

    Map<String, DbWorkspace> workspacesByProject =
        workspaceDao.findAllByGoogleProjectIn(googleProjects).stream()
            .collect(Collectors.toMap(DbWorkspace::getGoogleProject, Function.identity()));

    List<ListDisksRow> diskRows =
        disks.stream()
            .map(
                resp ->
                    toDiskRow(
                        resp, Optional.ofNullable(workspacesByProject.get(getGoogleProject(resp)))))
            .toList();

    log.info("Step 3 of 3: Saving the disk list to " + outputFile);

    ListDisksRow header =
        new ListDisksRow(
            "Name",
            "Environment Type",
            "Size (GB)",
            "Google Project",
            "Status",
            "Creator",
            "Created Date",
            "Date Accessed",
            "Workspace Namespace",
            "Workspace Display Name",
            "Workspace Terra Name",
            "Workspace Creator");

    try (CSVWriter writer = new CSVWriter(new FileWriter(outputFile))) {
      writer.writeNext(header.toArray());
      diskRows.stream().map(ListDisksRow::toArray).forEach(writer::writeNext);
      writer.flush();
    }
  }

  @Bean
  public CommandLineRunner run(
      @Qualifier(SERVICE_DISKS_API) Provider<DisksApi> disksApiProvider,
      WorkspaceDao workspaceDao) {
    return args -> {
      // project.rb swallows exceptions, so we need to catch and log them here
      try {
        listDisks(disksApiProvider.get(), workspaceDao, new DefaultParser().parse(options, args));
      } catch (Exception e) {
        log.severe("Error: " + e.getMessage());
        e.printStackTrace();
      }
    };
  }

  public static void main(String[] args) {
    CommandLineToolConfig.runCommandLine(ListDisks.class, args);
  }
}
