package org.pmiops.workbench.tools;

import static org.pmiops.workbench.utils.BillingUtils.isInitialCredits;

import com.opencsv.CSVWriter;
import jakarta.annotation.Nullable;
import jakarta.inject.Provider;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FireCloudServiceImpl;
import org.pmiops.workbench.firecloud.FirecloudApiClientFactory;
import org.pmiops.workbench.google.GoogleConfig;
import org.pmiops.workbench.leonardo.LeonardoApiClient;
import org.pmiops.workbench.leonardo.LeonardoApiClientFactory;
import org.pmiops.workbench.leonardo.LeonardoApiClientImpl;
import org.pmiops.workbench.leonardo.LeonardoConfig;
import org.pmiops.workbench.leonardo.LeonardoLabelHelper;
import org.pmiops.workbench.leonardo.model.LeonardoListPersistentDiskResponse;
import org.pmiops.workbench.sam.SamRetryHandler;
import org.pmiops.workbench.utils.mappers.LeonardoMapperImpl;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/** Retrieve details about all persistent disks in an environment. */
@Configuration
@Import({
  FireCloudServiceImpl.class,
  FirecloudApiClientFactory.class,
  GoogleConfig.class, // injects com.google.cloud.iam.credentials.v1.IamCredentialsClient
  LeonardoApiClientFactory.class,
  LeonardoApiClientImpl.class,
  LeonardoConfig.class,
  LeonardoMapperImpl.class,
  SamRetryHandler.class,
})
public class ListDisks extends Tool {

  private static final Logger log = Logger.getLogger(ListDisks.class.getName());

  private static final String MISSING = "[missing workspace]";

  private static Option outputFileOpt =
      Option.builder().longOpt("output").desc("Output file name").required().hasArg().build();

  private static Options options = new Options().addOption(outputFileOpt);

  // Leonardo supports Azure but we do not, so we know this is always a Google Project
  private String getGoogleProject(LeonardoListPersistentDiskResponse response) {
    return response.getCloudContext().getCloudResource();
  }

  private ListDisksRow toDiskRow(
      LeonardoListPersistentDiskResponse response,
      String workspaceNamespace,
      String workspaceDisplayName,
      String workspaceTerraName,
      String workspaceCreator,
      String billingAccountType) {

    String name = response.getName();
    String environmentType = LeonardoLabelHelper.getEnvironmentType(response.getLabels());
    String sizeInGb = response.getSize().toString();
    String googleProject = getGoogleProject(response);
    String status = response.getStatus().toString();
    String creator = response.getAuditInfo().getCreator();
    String createdDate = response.getAuditInfo().getCreatedDate();
    String dateAccessed = response.getAuditInfo().getDateAccessed();

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
        workspaceCreator,
        billingAccountType);
  }

  private void listDisks(
      LeonardoApiClient leonardoApiClient,
      WorkspaceDao workspaceDao,
      WorkbenchConfig workbenchConfig,
      CommandLine opts)
      throws IOException {
    String outputFile = opts.getOptionValue(outputFileOpt.getLongOpt());

    log.info("Step 1 of 3: Retrieving disk information from Leonardo");

    List<LeonardoListPersistentDiskResponse> disks = leonardoApiClient.listDisksAsService();

    log.info("Step 2 of 3: Associating disk information with RWB workspaces");

    Set<String> googleProjects =
        disks.stream().map(this::getGoogleProject).collect(Collectors.toSet());

    Map<String, DbWorkspace> workspacesByProject =
        workspaceDao.findAllByGoogleProjectIn(googleProjects).stream()
            .collect(Collectors.toMap(DbWorkspace::getGoogleProject, Function.identity()));

    List<ListDisksRow> diskRows =
        disks.stream()
            .map(
                resp -> {
                  String googleProject = getGoogleProject(resp);
                  @Nullable DbWorkspace dbWorkspace = workspacesByProject.get(googleProject);

                  return dbWorkspace == null
                      ? toDiskRow(resp, MISSING, MISSING, MISSING, MISSING, MISSING)
                      : toDiskRow(
                          resp,
                          dbWorkspace.getWorkspaceNamespace(),
                          dbWorkspace.getName(),
                          dbWorkspace.getFirecloudName(),
                          dbWorkspace.getCreator().getUsername(),
                          isInitialCredits(dbWorkspace.getBillingAccountName(), workbenchConfig)
                              ? "initial credits"
                              : "user provided");
                })
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
            "Workspace Creator",
            "Billing Account Type");

    try (CSVWriter writer = new CSVWriter(new FileWriter(outputFile))) {
      writer.writeNext(header.toArray());
      diskRows.stream().map(ListDisksRow::toArray).forEach(writer::writeNext);
      writer.flush();
    }
  }

  @Bean
  public CommandLineRunner run(
      LeonardoApiClient leonardoApiClient,
      WorkspaceDao workspaceDao,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    return args -> {
      // project.rb swallows exceptions, so we need to catch and log them here
      try {
        listDisks(
            leonardoApiClient,
            workspaceDao,
            workbenchConfigProvider.get(),
            new DefaultParser().parse(options, args));
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
