package org.pmiops.workbench.featuredworkspace;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.pmiops.workbench.db.dao.FeaturedWorkspaceDao;
import org.pmiops.workbench.db.model.DbFeaturedWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.FeaturedWorkspaceCategory;
import org.pmiops.workbench.model.WorkspaceResponse;
import org.pmiops.workbench.utils.mappers.FeaturedWorkspaceMapper;
import org.pmiops.workbench.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class FeaturedWorkspaceServiceImpl implements FeaturedWorkspaceService {
  private final FeaturedWorkspaceDao featuredWorkspaceDao;
  private final FeaturedWorkspaceMapper featuredWorkspaceMapper;
  private final WorkspaceService workspaceService;

  @Autowired
  public FeaturedWorkspaceServiceImpl(
      FeaturedWorkspaceDao featuredWorkspaceDao,
      FeaturedWorkspaceMapper featuredWorkspaceMapper,
      @Lazy WorkspaceService workspaceService) {
    this.featuredWorkspaceDao = featuredWorkspaceDao;
    this.featuredWorkspaceMapper = featuredWorkspaceMapper;
    this.workspaceService = workspaceService;
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

  @Override
  public List<WorkspaceResponse> getByFeaturedCategory(String category) {
    Optional<List<DbFeaturedWorkspace>> featureWorkspacelistByCateogry =
        featuredWorkspaceDao.findDbFeaturedWorkspacesByCategory(
            featuredWorkspaceMapper.toDbFeaturedCategory(
                FeaturedWorkspaceCategory.fromValue(category)));
    List<WorkspaceResponse> workspaceResponsesByCategory =
        featureWorkspacelistByCateogry
            .map(
                dbFeaturedWorkspaces ->
                    dbFeaturedWorkspaces.stream()
                        .map(
                            dbFeaturedWorkspace ->
                                workspaceService.getWorkspace(
                                    dbFeaturedWorkspace.getWorkspace().getWorkspaceNamespace(),
                                    dbFeaturedWorkspace.getWorkspace().getFirecloudName()))
                        .collect(Collectors.toList()))
            .orElse(new ArrayList<>());
    return workspaceResponsesByCategory;
  }
}
