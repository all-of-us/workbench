package org.pmiops.workbench.rdr;

import java.util.List;
import org.pmiops.workbench.model.RdrEntity;

public interface RdrExportService {
  List<Long> findUnchangedEntitiesForBackfill(RdrEntity entityType);

  List<Long> findAllUserIdsToExport();

  List<Long> findAllWorkspacesIdsToExport();

  void exportUsers(List<Long> usersToExport, boolean backfill);

  void exportWorkspaces(List<Long> workspacesToExport, boolean backfill);

  void updateDbRdrExport(RdrEntity entity, List<Long> idList);

  void deleteRdrExportEntries(RdrEntity entity, List<Long> workspaceIds);
}
