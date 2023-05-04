package org.pmiops.workbench.tools;

import com.google.common.primitives.Ints;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.pmiops.workbench.actionaudit.ActionAuditServiceImpl;
import org.pmiops.workbench.actionaudit.auditors.BillingProjectAuditorImpl;
import org.pmiops.workbench.audit.ActionAuditSpringConfiguration;
import org.pmiops.workbench.firecloud.FireCloudServiceImpl;
import org.pmiops.workbench.firecloud.FirecloudApiClientFactory;
import org.pmiops.workbench.google.GoogleConfig;
import org.pmiops.workbench.iam.SamApiClientFactory;
import org.pmiops.workbench.iam.SamRetryHandler;
import org.pmiops.workbench.impersonation.ImpersonatedFirecloudServiceImpl;
import org.pmiops.workbench.impersonation.ImpersonatedWorkspaceService;
import org.pmiops.workbench.impersonation.ImpersonatedWorkspaceServiceImpl;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.FirecloudMapperImpl;
import org.pmiops.workbench.utils.mappers.WorkspaceMapperImpl;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@Import({
  ActionAuditServiceImpl.class,
  ActionAuditSpringConfiguration.class, // injects com.google.cloud.logging.Logging
  BillingProjectAuditorImpl.class,
  CommonMappers.class,
  FireCloudServiceImpl.class,
  FirecloudApiClientFactory.class,
  FirecloudMapperImpl.class,
  GoogleConfig.class, // injects com.google.cloud.iam.credentials.v1.IamCredentialsClient
  ImpersonatedFirecloudServiceImpl.class,
  ImpersonatedWorkspaceServiceImpl.class,
  SamApiClientFactory.class,
  SamRetryHandler.class,
  WorkspaceMapperImpl.class,
})
public class DeleteOrphanedWorkspaces extends Tool {
  private static final Logger LOG = Logger.getLogger(DeleteOrphanedWorkspaces.class.getName());

  private static final List<String> ALLOWED_RW_ENVS = List.of("all-of-us-workbench-test");

  private static final Option RW_PROJ_OPT =
      Option.builder()
          .longOpt("project")
          .desc("The AoU project (environment) to access.")
          .required()
          .hasArg()
          .build();
  private static final Option USERNAME_OPT =
      Option.builder()
          .longOpt("username")
          .desc("The user whose workspaces we want to delete.")
          .required()
          .hasArg()
          .build();

  private static final Option LIMIT_OPT =
      Option.builder()
          .longOpt("limit")
          .desc("The maximum number of workspaces to delete, per step. Optional.")
          .hasArg()
          .build();

  private static final Option DELETE_OPT =
      Option.builder()
          .longOpt("delete")
          .desc("Enable to actually delete the workspaces.  Defaults to count only.")
          .build();

  private static final Options OPTIONS =
      new Options()
          .addOption(RW_PROJ_OPT)
          .addOption(USERNAME_OPT)
          .addOption(LIMIT_OPT)
          .addOption(DELETE_OPT);

  public static void main(String[] args) {
    CommandLineToolConfig.runCommandLine(DeleteOrphanedWorkspaces.class, args);
  }

  @Bean
  public CommandLineRunner run(ImpersonatedWorkspaceService workspaceService) {
    return (args) -> {
      final CommandLine opts = new DefaultParser().parse(OPTIONS, args);
      final String rwEnvOpt = opts.getOptionValue(RW_PROJ_OPT.getLongOpt());
      if (!ALLOWED_RW_ENVS.contains(rwEnvOpt)) {
        throw new IllegalArgumentException("Unsupported RW environment: " + rwEnvOpt);
      }
      final String usernameOpt = opts.getOptionValue(USERNAME_OPT.getLongOpt());
      final Optional<Integer> limitOpt =
          opts.hasOption(LIMIT_OPT)
              ? Optional.ofNullable(Ints.tryParse(opts.getOptionValue(LIMIT_OPT.getLongOpt())))
              : Optional.empty();
      // default to false
      final boolean deleteOpt =
          opts.hasOption(DELETE_OPT)
              && Boolean.parseBoolean(opts.getOptionValue(DELETE_OPT.getLongOpt()));

      var workspaces = workspaceService.getOwnedWorkspacesOrphanedInRawls(usernameOpt);
      LOG.info(
          String.format(
              "Found %d Rawls workspaces which are not present in the %s DB",
              workspaces.size(), rwEnvOpt));

      if (deleteOpt) {
        limitOpt
            .map(
                l -> {
                  LOG.info(String.format("Limiting to the first %d workspaces.", l));
                  return workspaces.stream().limit(l);
                })
            .orElse(workspaces.stream())
            .forEach(
                ws -> {
                  LOG.info(
                      String.format(
                          "Deleting Rawls workspace %s/%s",
                          ws.getWorkspace().getNamespace(), ws.getWorkspace().getName()));
                  workspaceService.deleteOrphanedRawlsWorkspace(
                      usernameOpt,
                      ws.getWorkspace().getNamespace(),
                      ws.getWorkspace().getGoogleProject(),
                      ws.getWorkspace().getName());
                });
      } else {
        LOG.info("Not deleting. Enable deletion by passing the --delete argument.");
      }
    };
  }
}
