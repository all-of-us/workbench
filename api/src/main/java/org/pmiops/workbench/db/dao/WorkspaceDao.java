package org.pmiops.workbench.db.dao;

import java.util.List;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.Workspace;
import org.springframework.data.repository.CrudRepository;

public interface WorkspaceDao extends CrudRepository<Workspace, Long> {
  Workspace findByFirecloudName(String firecloudName);
  List<Workspace> findByCreatorOrderByNameAsc(User creator);
}
