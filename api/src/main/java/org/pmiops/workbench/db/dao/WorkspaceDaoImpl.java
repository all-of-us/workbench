package org.pmiops.workbench.db.dao;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.ConflictException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

public class WorkspaceDaoImpl implements WorkspaceDaoCustom {

  private static final Logger log = Logger.getLogger(WorkspaceDaoImpl.class.getName());

  @Autowired private Clock clock;

  @Lazy @Autowired private WorkspaceDao workspaceDao;

  private DbWorkspace saveWithLastModified(DbWorkspace workspace, Timestamp ts) {
    workspace.setLastModifiedTime(ts);
    try {
      return workspaceDao.save(workspace);
    } catch (ObjectOptimisticLockingFailureException e) {
      log.log(Level.WARNING, "version conflict for workspace update", e);
      throw new ConflictException("Failed due to concurrent workspace modification");
    }
  }

  @Override
  public DbWorkspace saveWithLastModified(DbWorkspace workspace) {
    return saveWithLastModified(workspace, new Timestamp(clock.instant().toEpochMilli()));
  }
}
