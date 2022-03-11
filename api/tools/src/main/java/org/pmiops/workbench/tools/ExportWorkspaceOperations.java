package org.pmiops.workbench.tools;

import java.util.stream.StreamSupport;
import org.pmiops.workbench.db.dao.WorkspaceOperationDao;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

public class ExportWorkspaceOperations {

  private long count(Iterable<?> data) {
    return StreamSupport.stream(data.spliterator(), false).count();
  }

  @Bean
  public CommandLineRunner run(WorkspaceOperationDao workspaceOperationDao) {
    return (args) -> {
      System.out.printf(
          "running ExportWorkspaceOperations: found %d entries%n",
          count(workspaceOperationDao.findAll()));
    };
  }

  public static void main(String[] args) throws Exception {
    CommandLineToolConfig.runCommandLine(ExportWorkspaceOperations.class, args);
  }
}
