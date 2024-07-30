package org.pmiops.workbench.featuredworkspace;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.pmiops.workbench.db.dao.FeaturedWorkspaceDao;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.FeaturedWorkspaceCategory;
import org.pmiops.workbench.model.WorkspaceResponse;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
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
    return featuredWorkspaceDao
        .findDbFeaturedWorkspacesByCategory(
            featuredWorkspaceMapper.toDbFeaturedCategory(
                FeaturedWorkspaceCategory.fromValue(category)))
        .orElseGet(Collections::emptyList)
        .stream()
        .map(
            dbFeaturedCategory -> {
              DbWorkspace dbWorkspace = dbFeaturedCategory.getWorkspace();
              RawlsWorkspaceDetails fcWorkspace =
                  fireCloudService
                      .getWorkspace(
                          dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName())
                      .getWorkspace();
              return new WorkspaceResponse()
                  .workspace(workspaceMapper.toApiWorkspace(dbWorkspace, fcWorkspace));
            })
        .collect(Collectors.toList());
  }
}
