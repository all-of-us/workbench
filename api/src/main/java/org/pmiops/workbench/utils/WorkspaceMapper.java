package org.pmiops.workbench.utils;

import org.mapstruct.Mapper;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.Workspace;

@Mapper(
    componentModel = "spring",
    uses = {CommonMappers.class, WorkspaceDao.class})
public interface WorkspaceMapper {

  DbWorkspace clientToDbModel(Workspace clientWorkspace);

  Workspace dbModelToClient(DbWorkspace dbWorkspace);

}
