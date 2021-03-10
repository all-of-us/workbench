package org.pmiops.workbench.tools;

import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.config.WorkbenchLocationConfigService;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.rdr.RdrTaskQueue;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/** Add desc */
@Import({
  WorkbenchLocationConfigService.class,
})
public class BackfillWorkspacesToRdr {
  private static final Logger log = Logger.getLogger(DeleteWorkspaces.class.getName());

  public static void main(String[] args) {
    CommandLineToolConfig.runCommandLine(BackfillWorkspacesToRdr.class, args);
  }

  @Bean
  private RdrTaskQueue rdrTaskQueue(
      WorkbenchLocationConfigService locationConfigService,
      Provider<WorkbenchConfig> configProvider) {
    return new RdrTaskQueue(locationConfigService, configProvider);
  }

  @Bean
  public CommandLineRunner run(WorkspaceDao workspaceDao, RdrTaskQueue rdrTaskQueue) {
    return (args) -> {
      List<Long> workspaceListToExport =
          workspaceDao.findAllDbWorkspaceIds().stream().collect(Collectors.toList());

      rdrTaskQueue.groupIdsAndPushTask(
          workspaceListToExport, RdrTaskQueue.EXPORT_USER_PATH + "?backfill=true");
    };
  }
}
