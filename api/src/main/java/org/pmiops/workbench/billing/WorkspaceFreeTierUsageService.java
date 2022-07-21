package org.pmiops.workbench.billing;

import org.pmiops.workbench.db.dao.WorkspaceFreeTierUsageDao;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.db.model.DbWorkspaceFreeTierUsage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class WorkspaceFreeTierUsageService {

    private final WorkspaceFreeTierUsageDao workspaceFreeTierUsageDao;

    @Autowired
    public WorkspaceFreeTierUsageService(WorkspaceFreeTierUsageDao workspaceFreeTierUsageDao) {
        this.workspaceFreeTierUsageDao = workspaceFreeTierUsageDao;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateCost(Map<Long, DbWorkspaceFreeTierUsage> workspaceIdToFreeTierUsage, DbWorkspace workspace, Double liveCost) {
        workspaceFreeTierUsageDao.updateCost(workspaceIdToFreeTierUsage, workspace, liveCost);
    }
}
