package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;

public interface WorkspaceDaoCustom {

  DbWorkspace saveWithLastModified(DbWorkspace workspace, DbUser user);
}
