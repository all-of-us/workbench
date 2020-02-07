package org.pmiops.workbench.workspaceadmin;

import java.util.Optional;
import org.pmiops.workbench.db.dao.CohortDao;
import org.pmiops.workbench.db.dao.ConceptSetDao;
import org.pmiops.workbench.db.dao.DataSetDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.AdminWorkspaceObjectsCounts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceAdminServiceImpl implements WorkspaceAdminService {
  private final CohortDao cohortDao;
  private final ConceptSetDao conceptSetDao;
  private final DataSetDao dataSetDao;
  private final WorkspaceDao workspaceDao;

  @Autowired
  public WorkspaceAdminServiceImpl(
      CohortDao cohortDao,
      ConceptSetDao conceptSetDao,
      DataSetDao dataSetDao,
      WorkspaceDao workspaceDao
  ) {
    this.cohortDao = cohortDao;
    this.conceptSetDao = conceptSetDao;
    this.dataSetDao = dataSetDao;
    this.workspaceDao = workspaceDao;
  }

  @Override
  /**
   * Returns the first workspace found for any given namespace.
   */
  public Optional<DbWorkspace> getFirstWorkspaceByNamespace(String workspaceNamespace) {
    return workspaceDao.findFirstByWorkspaceNamespaceOrderByFirecloudNameAsc(workspaceNamespace);
  }

  @Override
  public AdminWorkspaceObjectsCounts getAdminWorkspaceObjects(long workspaceId) {
    int cohortCount = cohortDao.countByWorkspaceId(workspaceId);
    int conceptSetCount = conceptSetDao.countByWorkspaceId(workspaceId);
    int dataSetCount = dataSetDao.countByWorkspaceId(workspaceId);
    return new AdminWorkspaceObjectsCounts()
        .cohortCount(cohortCount)
        .conceptSetCount(conceptSetCount)
        .datasetCount(dataSetCount);
  }
}
