package org.pmiops.workbench.featuredworkspace;

import jakarta.inject.Provider;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.pmiops.workbench.config.FeaturedWorkspacesConfig;
import org.pmiops.workbench.db.dao.FeaturedWorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbFeaturedWorkspace.DbFeaturedCategory;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.initialcredits.InitialCreditsExpirationService;
import org.pmiops.workbench.model.FeaturedWorkspaceCategory;
import org.pmiops.workbench.model.WorkspaceResponse;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceResponse;
import org.pmiops.workbench.utils.mappers.FeaturedWorkspaceMapper;
import org.pmiops.workbench.utils.mappers.WorkspaceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FeaturedWorkspaceServiceImpl implements FeaturedWorkspaceService {
  private final FeaturedWorkspaceDao featuredWorkspaceDao;
  private final FeaturedWorkspaceMapper featuredWorkspaceMapper;
  private final Provider<FeaturedWorkspacesConfig> featuredWorkspacesConfigProvider;
  private final FireCloudService fireCloudService;
  private final InitialCreditsExpirationService initialCreditsExpirationService;
  private final WorkspaceDao workspaceDao;
  private final WorkspaceMapper workspaceMapper;

  @Autowired
  public FeaturedWorkspaceServiceImpl(
      FeaturedWorkspaceDao featuredWorkspaceDao,
      FeaturedWorkspaceMapper featuredWorkspaceMapper,
      Provider<FeaturedWorkspacesConfig> featuredWorkspacesConfigProvider,
      FireCloudService fireCloudService,
      InitialCreditsExpirationService initialCreditsExpirationService,
      WorkspaceDao workspaceDao,
      WorkspaceMapper workspaceMapper) {
    this.featuredWorkspaceDao = featuredWorkspaceDao;
    this.featuredWorkspaceMapper = featuredWorkspaceMapper;
    this.featuredWorkspacesConfigProvider = featuredWorkspacesConfigProvider;
    this.fireCloudService = fireCloudService;
    this.initialCreditsExpirationService = initialCreditsExpirationService;
    this.workspaceDao = workspaceDao;
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
    DbFeaturedCategory requestedDbCategory =
        featuredWorkspaceMapper.toDbFeaturedCategory(featuredWorkspaceCategory);

    return featuredWorkspaceDao.findDbFeaturedWorkspacesByCategory(requestedDbCategory).stream()
        .map(
            dbFeaturedCategory -> {
              DbWorkspace dbWorkspace = dbFeaturedCategory.getWorkspace();
              RawlsWorkspaceResponse rawlsWorkspaceResponse =
                  fireCloudService.getWorkspace(
                      dbWorkspace.getWorkspaceNamespace(), dbWorkspace.getFirecloudName());
              return workspaceMapper.toApiWorkspaceResponse(
                  workspaceMapper.toApiWorkspace(
                      dbWorkspace,
                      rawlsWorkspaceResponse.getWorkspace(),
                      initialCreditsExpirationService),
                  rawlsWorkspaceResponse.getAccessLevel());
            })
        .collect(Collectors.toList());
  }
}
