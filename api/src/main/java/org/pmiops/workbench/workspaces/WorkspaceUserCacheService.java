package org.pmiops.workbench.workspaces;

import java.util.List;
import java.util.Map;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceAccessEntry;

public interface WorkspaceUserCacheService {
  List<DbWorkspace> findAllActiveWorkspaceNamespacesNeedingCacheUpdate();

  void updateWorkspaceUserCache(
      Map<Long, Map<String, RawlsWorkspaceAccessEntry>> newEntriesByWorkspaceId);

  void removeInactiveWorkspaces();
}
