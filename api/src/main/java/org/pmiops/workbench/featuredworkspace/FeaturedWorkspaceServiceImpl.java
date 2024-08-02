package org.pmiops.workbench.featuredworkspace;

import jakarta.inject.Provider;
import java.util.Optional;
import org.pmiops.workbench.config.FeaturedWorkspacesConfig;
import org.pmiops.workbench.db.dao.FeaturedWorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.firecloud.FireCloudService;
import org.pmiops.workbench.model.FeaturedWorkspaceCategory;
import org.pmiops.workbench.model.FeaturedWorkspacesConfigResponse;
import org.pmiops.workbench.utils.mappers.FeaturedWorkspaceMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FeaturedWorkspaceServiceImpl implements FeaturedWorkspaceService {
  private final FeaturedWorkspaceDao featuredWorkspaceDao;
  private final FeaturedWorkspaceMapper featuredWorkspaceMapper;
  private final Provider<FeaturedWorkspacesConfig> featuredWorkspacesConfigProvider;
  private final FireCloudService fireCloudService;
  private final WorkspaceDao workspaceDao;

  @Autowired
  public FeaturedWorkspaceServiceImpl(
      FeaturedWorkspaceDao featuredWorkspaceDao,
      FeaturedWorkspaceMapper featuredWorkspaceMapper,
      Provider<FeaturedWorkspacesConfig> featuredWorkspacesConfigProvider,
      FireCloudService fireCloudService,
      WorkspaceDao workspaceDao) {
    this.featuredWorkspaceDao = featuredWorkspaceDao;
    this.featuredWorkspaceMapper = featuredWorkspaceMapper;
    this.featuredWorkspacesConfigProvider = featuredWorkspacesConfigProvider;
    this.fireCloudService = fireCloudService;
    this.workspaceDao = workspaceDao;
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
  public void backFillFeaturedWorkspaces() {
    FeaturedWorkspacesConfig fwConfig = featuredWorkspacesConfigProvider.get();
    // Get featured workspaces from the config
    FeaturedWorkspacesConfigResponse configResponse =
        new FeaturedWorkspacesConfigResponse().featuredWorkspacesList(fwConfig.featuredWorkspaces);
    configResponse
        .getFeaturedWorkspacesList()
        .forEach(
            fw -> {
              String workspaceNamespace = fw.getNamespace();
              String firecloudName = fw.getId();
              try {
                // Get Dbworkspace from workspaceNamesapce and firecloudname
                DbWorkspace dbWorkspace =
                    workspaceDao.getRequired(workspaceNamespace, firecloudName);

                if (dbWorkspace.getPublished()) {
                  // If workspace was marked published, update the workspace acl to save to table
                  // featured_workspace
                  System.out.println(
                      "workspace " + dbWorkspace.getFirecloudName() + " will be published");
                  fireCloudService.updateWorkspaceAclForPublishing(
                      workspaceNamespace, firecloudName, true);
                  featuredWorkspaceDao.save(
                      featuredWorkspaceMapper.toDbFeaturedWorkspace(fw.getCategory(), dbWorkspace));
                }
              } catch (NotFoundException e) {
                System.out.println("workspace  " + fw.getNamespace() + " could not be found");
              }
            });
  }
}
