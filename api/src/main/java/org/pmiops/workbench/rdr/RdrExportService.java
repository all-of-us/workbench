package org.pmiops.workbench.rdr;

import java.util.List;
import org.pmiops.workbench.model.RdrEntity;

public interface RdrExportService {
  List<Long> findAllUserIdsToExport();

  List<Long> findAllWorkspacesIdsToExport();

  void exportUsers(List<Long> usersToExport);

  void exportWorkspaces(List<Long> workspacesToExport);

  void updateDbRdrExport(RdrEntity entity, List<Long> idList);
}
