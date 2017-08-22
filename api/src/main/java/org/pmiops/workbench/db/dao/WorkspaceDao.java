package org.pmiops.workbench.db.dao;

import java.util.List;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.Workspace;
import org.springframework.data.repository.CrudRepository;

public interface WorkspaceDao extends CrudRepository<Workspace, Long> {
  List<Workspace> findByWorkspaceNamespace(String workspaceNamespace);
  Workspace findByWorkspaceNamespaceAndFirecloudName(String workspaceNamespace,
      String firecloudName);
  List<Workspace> findByCreatorOrderByNameAsc(User creator);
}
