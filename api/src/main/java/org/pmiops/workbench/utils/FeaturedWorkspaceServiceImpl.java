package org.pmiops.workbench.utils;

import java.util.List;
import java.util.stream.Collectors;
import org.pmiops.workbench.db.dao.FeaturedWorkspaceDao;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.FeaturedWorkspace;
import org.pmiops.workbench.utils.mappers.FeaturedWorkspaceMapper;
import org.pmiops.workbench.utils.mappers.WorkspaceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FeaturedWorkspaceServiceImpl implements FeaturedWorkspaceService {

  private final FeaturedWorkspaceDao featuredWorkspaceDao;
  private final FeaturedWorkspaceMapper featuredWorkspaceMapper;
  private final WorkspaceMapper workspaceMapper;
  private final FireCloudService fireCloudService;

  @Autowired
  public FeaturedWorkspaceServiceImpl(
      FeaturedWorkspaceDao featuredWorkspaceDao,
      FireCloudService fireCloudService,
      FeaturedWorkspaceMapper featuredWorkspaceMapper,
      WorkspaceMapper workspaceMapper) {
    this.featuredWorkspaceDao = featuredWorkspaceDao;
    this.fireCloudService = fireCloudService;
    this.featuredWorkspaceMapper = featuredWorkspaceMapper;
    this.workspaceMapper = workspaceMapper;
  }

  @Override
  public List<FeaturedWorkspace> getAllFeaturedWorkspaces() {
    return featuredWorkspaceDao.findAll().stream()
        .map(featuredWorkspaceMapper::toFeaturedWorkspace)
        .collect(Collectors.toList());
  }
}
