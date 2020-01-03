package org.pmiops.workbench.rdr;

import java.util.List;

public interface RdrExportService {
  List<Long> findAllUserIdsToExport();

  List<Long> findAllWorkspacesIdsToExport();

  void exportUsers(List<Long> usersToExport);

  void exportWorkspaces(List<Long> workspacesToExport);
}
