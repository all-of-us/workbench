package org.pmiops.workbench.featuredworkspace;

import java.util.List;
import java.util.stream.StreamSupport;
import org.pmiops.workbench.db.dao.FeaturedWorkspaceDao;
import org.pmiops.workbench.db.model.DbFeaturedWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.FeaturedWorkspaceCategory;
import org.pmiops.workbench.model.WorkspaceResponse;
import org.pmiops.workbench.utils.mappers.FeaturedWorkspaceMapper;
import org.pmiops.workbench.utils.mappers.WorkspaceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FeaturedWorkspaceServiceImpl implements FeaturedWorkspaceService {
  private final FeaturedWorkspaceDao featuredWorkspaceDao;
  private final FeaturedWorkspaceMapper featuredWorkspaceMapper;
  private final FireCloudService fireCloudService;
  private final WorkspaceMapper workspaceMapper;

  @Autowired
  public FeaturedWorkspaceServiceImpl(
      FeaturedWorkspaceDao featuredWorkspaceDao,
      FeaturedWorkspaceMapper featuredWorkspaceMapper,
      FireCloudService fireCloudService,
      WorkspaceMapper workspaceMapper) {
    this.featuredWorkspaceDao = featuredWorkspaceDao;
    this.featuredWorkspaceMapper = featuredWorkspaceMapper;
    this.fireCloudService = fireCloudService;
    this.workspaceMapper = workspaceMapper;
  }

  @Override
  public boolean isFeaturedWorkspace(DbWorkspace dbWorkspace) {
    return featuredWorkspaceDao.existsByWorkspace(dbWorkspace);
  }

  @Override
  public FeaturedWorkspaceCategory getFeaturedCategory(DbWorkspace dbWorkspace) {
    DbFeaturedWorkspace dbFeaturedWorkspace =
        featuredWorkspaceDao.findByWorkspace(dbWorkspace).orElseThrow();
    return featuredWorkspaceMapper.toFeaturedWorkspaceCategory(dbFeaturedWorkspace.getCategory());
  }

  @Override
  public List<DbWorkspace> getDbWorkspaces() {
    return StreamSupport.stream(featuredWorkspaceDao.findAll().spliterator(), false)
        .map(DbFeaturedWorkspace::getWorkspace)
        .toList();
  }

  @Override
  public List<WorkspaceResponse> getFeaturedWorkspaces() {
    return workspaceMapper.toFeaturedWorkspaceResponseList(fireCloudService, this);
  }
}
