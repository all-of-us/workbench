package org.pmiops.workbench.featuredworkspace;

import org.pmiops.workbench.db.dao.FeaturedWorkspaceDao;
import org.pmiops.workbench.db.model.DbFeaturedWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.FeaturedWorkspaceCategory;
import org.pmiops.workbench.utils.mappers.FeaturedWorkspaceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FeaturedWorkspaceServiceImpl implements FeaturedWorkspaceService {
  private final FeaturedWorkspaceDao featuredWorkspaceDao;
  private final FeaturedWorkspaceMapper featuredWorkspaceMapper;

  @Autowired
  public FeaturedWorkspaceServiceImpl(
      FeaturedWorkspaceDao featuredWorkspaceDao, FeaturedWorkspaceMapper featuredWorkspaceMapper) {
    this.featuredWorkspaceDao = featuredWorkspaceDao;
    this.featuredWorkspaceMapper = featuredWorkspaceMapper;
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
}
