package org.pmiops.workbench.rdr;

import java.util.List;

public interface RDRService {
  List<Long> findAllUserIdsToExport();

  List<Long> findAllWorkspacesIdsToExport();

  void sendUser(List<Long> usersToExport);

  void sendWorkspace(List<Long> workspacesToExport);
}
