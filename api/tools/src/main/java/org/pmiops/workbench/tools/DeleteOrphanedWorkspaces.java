package org.pmiops.workbench.tools;

import java.util.List;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.pmiops.workbench.actionaudit.auditors.BillingProjectAuditorImpl;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.firecloud.FireCloudServiceImpl;
import org.pmiops.workbench.impersonation.ImpersonatedFirecloudServiceImpl;
import org.pmiops.workbench.impersonation.ImpersonatedWorkspaceService;
import org.pmiops.workbench.impersonation.ImpersonatedWorkspaceServiceImpl;
import org.pmiops.workbench.utils.mappers.FirecloudMapperImpl;
import org.pmiops.workbench.utils.mappers.WorkspaceMapperImpl;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@Import({
    BillingProjectAuditorImpl.class,
    FireCloudServiceImpl.class,
    FirecloudMapperImpl.class,
    ImpersonatedFirecloudServiceImpl.class,
    ImpersonatedWorkspaceServiceImpl.class,
    UserDao.class,
    WorkspaceDao.class,
    WorkspaceMapperImpl.class,
})
public class DeleteOrphanedWorkspaces extends Tool {
  private static final Logger LOG = Logger.getLogger(DeleteOrphanedWorkspaces.class.getName());

  private static final List<String> ALLOWED_RW_ENVS = List.of("all-of-us-workbench-test");

  private static final Option RW_PROJ_OPT =
      Option.builder()
          .longOpt("project")
          .desc("The AoU project to access.")
          .required()
          .hasArg()
          .build();

  private static final Options OPTIONS = new Options().addOption(RW_PROJ_OPT);

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

      var workspaces =
          workspaceService.getOwnedWorkspacesOrphanedInRawls(
              "puppeteer-tester-7@fake-research-aou.org");
      LOG.info(String.format("Saw %d Rawls workspaces", workspaces.size()));
      workspaces.stream()
          .limit(5)
          .forEach(
              ws ->
                  LOG.info(
                      String.format(
                          "Saw Rawls workspace %s/%s",
                          ws.getWorkspace().getNamespace(), ws.getWorkspace().getName())));
    };
  }
}
