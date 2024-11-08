package org.pmiops.workbench.featuredworkspace;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.pmiops.workbench.initialcredits.InitialCreditsService;
import org.pmiops.workbench.db.dao.FeaturedWorkspaceDao;
import org.pmiops.workbench.db.model.DbFeaturedWorkspace;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.FeaturedWorkspaceCategory;
import org.pmiops.workbench.model.WorkspaceResponse;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceListResponse;
import org.pmiops.workbench.utils.mappers.FeaturedWorkspaceMapper;
import org.pmiops.workbench.utils.mappers.WorkspaceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FeaturedWorkspaceServiceImpl implements FeaturedWorkspaceService {
  private final FeaturedWorkspaceDao featuredWorkspaceDao;
  private final FeaturedWorkspaceMapper featuredWorkspaceMapper;
  private final FireCloudService fireCloudService;
  private final InitialCreditsService initialCreditsService;
  private final WorkspaceMapper workspaceMapper;

  @Autowired
  public FeaturedWorkspaceServiceImpl(
      FeaturedWorkspaceDao featuredWorkspaceDao,
      FeaturedWorkspaceMapper featuredWorkspaceMapper,
      FireCloudService fireCloudService,
      InitialCreditsService initialCreditsService,
      WorkspaceMapper workspaceMapper) {
    this.featuredWorkspaceDao = featuredWorkspaceDao;
    this.featuredWorkspaceMapper = featuredWorkspaceMapper;
    this.fireCloudService = fireCloudService;
    this.initialCreditsService = initialCreditsService;
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

  public List<WorkspaceResponse> getWorkspaceResponseByFeaturedCategory(
      FeaturedWorkspaceCategory featuredWorkspaceCategory) {

    List<DbWorkspace> dbWorkspaces =
        featuredWorkspaceDao
            .findDbFeaturedWorkspacesByCategory(
                featuredWorkspaceMapper.toDbFeaturedCategory(featuredWorkspaceCategory))
            .stream()
            .map(DbFeaturedWorkspace::getWorkspace)
            .toList();

    Map<String, RawlsWorkspaceListResponse> fcWorkspacesByUuid =
        fireCloudService.getWorkspaces().stream()
            .collect(
                Collectors.toMap(
                    fcWorkspace -> fcWorkspace.getWorkspace().getWorkspaceId(),
                    fcWorkspace -> fcWorkspace));

    return workspaceMapper.toApiWorkspaceResponseList(
        dbWorkspaces, fcWorkspacesByUuid, initialCreditsService);
  }
}
