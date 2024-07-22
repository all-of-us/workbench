package org.pmiops.workbench.featuredworkspace;

import java.util.Optional;
import org.pmiops.workbench.db.dao.FeaturedWorkspaceDao;
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
  public Optional<FeaturedWorkspaceCategory> getFeaturedCategory(DbWorkspace dbWorkspace) {
    return featuredWorkspaceDao
        .findByWorkspace(dbWorkspace)
        .map(
            dbFeaturedWorkspace ->
                featuredWorkspaceMapper.toFeaturedWorkspaceCategory(
                    dbFeaturedWorkspace.getCategory()));
  }
}
