package org.pmiops.workbench.tools;

import com.opencsv.CSVWriter;
import java.io.FileWriter;
import java.sql.Timestamp;
import java.util.stream.StreamSupport;
import org.pmiops.workbench.db.dao.WorkspaceOperationDao;
import org.pmiops.workbench.db.model.DbWorkspaceOperation;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

public class ExportWorkspaceOperations {
  private final String OUTFILE = "workspace_operations.txt";

  private String[] outputOperationHeaders() {
    return new String[] {"id", "status", "creation_time", "last_modified_time", "seconds_elapsed"};
  }

  private String elapsed(Timestamp creationTime, Timestamp lastModifiedTime) {
    long seconds = (lastModifiedTime.getTime() - creationTime.getTime()) / 1000;
    return String.format("%d seconds", seconds);
  }

  private String[] outputOperationRow(DbWorkspaceOperation dbWorkspaceOperation) {
    return new String[] {
      String.valueOf(dbWorkspaceOperation.getId()),
      dbWorkspaceOperation.getStatus().toString(),
      dbWorkspaceOperation.getCreationTime().toString(),
      dbWorkspaceOperation.getLastModifiedTime().toString(),
      elapsed(dbWorkspaceOperation.getCreationTime(), dbWorkspaceOperation.getLastModifiedTime())
    };
  }

  @Bean
  public CommandLineRunner run(WorkspaceOperationDao workspaceOperationDao) {
    return (args) -> {
      try (CSVWriter writer = new CSVWriter(new FileWriter(OUTFILE))) {
        writer.writeNext(outputOperationHeaders());
        StreamSupport.stream(workspaceOperationDao.findAll().spliterator(), false)
            .map(this::outputOperationRow)
            .limit(10)
            .forEach(writer::writeNext);
        writer.flush();
      }
    };
  }

  public static void main(String[] args) throws Exception {
    CommandLineToolConfig.runCommandLine(ExportWorkspaceOperations.class, args);
  }
}
