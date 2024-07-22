package org.pmiops.workbench.utils.mappers;

import static org.mapstruct.NullValuePropertyMappingStrategy.SET_TO_DEFAULT;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.mapstruct.AfterMapping;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.featuredworkspace.FeaturedWorkspaceService;
import org.pmiops.workbench.model.CdrVersion;
import org.pmiops.workbench.model.RecentWorkspace;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceResponse;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceAccessLevel;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceDetails;
import org.pmiops.workbench.rawls.model.RawlsWorkspaceListResponse;
import org.pmiops.workbench.workspaces.resources.WorkspaceResourceMapper;

@Mapper(
    config = MapStructConfig.class,
    collectionMappingStrategy = CollectionMappingStrategy.TARGET_IMMUTABLE,
    uses = {
      CommonMappers.class,
      DbStorageEnums.class,
      FirecloudMapper.class,
      WorkspaceResourceMapper.class,
    })
public interface WorkspaceMapper {

  // DEPRECATED and subject to deletion.  Use terraName instead.
  @Mapping(target = "id", source = "fcWorkspace.name")
  // DEPRECATED and subject to deletion.
  // Make an explicit choice to use either displayName for UI or terraName for Terra calls.
  @Mapping(target = "name", source = "dbWorkspace.name")
  @Mapping(target = "researchPurpose", source = "dbWorkspace")
  @Mapping(target = "etag", source = "dbWorkspace.version", qualifiedByName = "versionToEtag")
  @Mapping(target = "namespace", source = "dbWorkspace.workspaceNamespace")
  @Mapping(target = "displayName", source = "dbWorkspace.name")
  @Mapping(target = "terraName", source = "fcWorkspace.name")
  @Mapping(target = "googleBucketName", source = "fcWorkspace.bucketName")
  @Mapping(target = "creator", source = "dbWorkspace.creator.username")
  @Mapping(target = "cdrVersionId", source = "dbWorkspace.cdrVersion")
  @Mapping(target = "accessTierShortName", source = "dbWorkspace.cdrVersion.accessTier.shortName")
  @Mapping(target = "googleProject", source = "dbWorkspace.googleProject")
  @Mapping(target = "featuredCategory", ignore = true) // set by setFeaturedWorkspaceCategory()
  Workspace toApiWorkspace(
      DbWorkspace dbWorkspace,
      RawlsWorkspaceDetails fcWorkspace,
      FeaturedWorkspaceService featuredWorkspaceService);

  @AfterMapping
  default void setFeaturedWorkspaceCategory(
      @MappingTarget Workspace workspace,
      DbWorkspace dbWorkspace,
      FeaturedWorkspaceService featuredWorkspaceService) {
    workspace.setFeaturedCategory(
        featuredWorkspaceService.getFeaturedCategory(dbWorkspace).orElse(null));
  }

  @Mapping(
      target = "accessLevel",
      source = "accessLevel",
      qualifiedByName = "fcToApiWorkspaceAccessLevel")
  WorkspaceResponse toApiWorkspaceResponse(
      Workspace workspace, RawlsWorkspaceAccessLevel accessLevel);

  default List<WorkspaceResponse> toApiWorkspaceResponseList(
      WorkspaceDao workspaceDao,
      List<RawlsWorkspaceListResponse> fcWorkspaces,
      FeaturedWorkspaceService featuredWorkspaceService) {
    // fields must include at least "workspace.workspaceId", otherwise
    // the map creation will fail
    Map<String, RawlsWorkspaceListResponse> fcWorkspacesByUuid =
        fcWorkspaces.stream()
            .collect(
                Collectors.toMap(
                    fcWorkspace -> fcWorkspace.getWorkspace().getWorkspaceId(),
                    fcWorkspace -> fcWorkspace));

    List<DbWorkspace> dbWorkspaces =
        workspaceDao.findActiveByFirecloudUuidIn(fcWorkspacesByUuid.keySet());
    return dbWorkspaces.stream()
        .map(
            dbWorkspace -> {
              var fcResponse = fcWorkspacesByUuid.get(dbWorkspace.getFirecloudUuid());
              return toApiWorkspaceResponse(
                  toApiWorkspace(dbWorkspace, fcResponse.getWorkspace(), featuredWorkspaceService),
                  fcResponse.getAccessLevel());
            })
        .toList();
  }

  @Mapping(target = "timeReviewed", ignore = true)
  @Mapping(target = "populationDetails", source = "specificPopulationsEnum")
  @Mapping(target = "researchOutcomeList", source = "researchOutcomeEnumSet")
  @Mapping(target = "disseminateResearchFindingList", source = "disseminateResearchEnumSet")
  @Mapping(target = "otherDisseminateResearchFindings", source = "disseminateResearchOther")
  ResearchPurpose workspaceToResearchPurpose(DbWorkspace dbWorkspace);

  // DEPRECATED and subject to deletion.  Use terraName instead.
  @Mapping(target = "id", source = "firecloudName")
  @Mapping(target = "cdrVersionId", source = "cdrVersion")
  @Mapping(target = "creator", source = "creator.username")
  @Mapping(target = "etag", source = "version", qualifiedByName = "versionToEtag")
  @Mapping(
      target = "googleBucketName",
      ignore = true) // available via toApiWorkspace(DbWorkspace dbWorkspace, RawlsWorkspaceDetails
  // fcWorkspace)
  @Mapping(target = "displayName", source = "name")
  @Mapping(target = "terraName", source = "firecloudName")
  @Mapping(target = "namespace", source = "workspaceNamespace")
  @Mapping(target = "researchPurpose", source = "dbWorkspace")
  @Mapping(target = "accessTierShortName", source = "dbWorkspace.cdrVersion.accessTier.shortName")
  @Mapping(target = "featuredCategory", ignore = true)
  // provides an incomplete workspace!  Only for use by the RecentWorkspace mapper
  Workspace onlyForMappingRecentWorkspace(DbWorkspace dbWorkspace);

  @Mapping(target = "workspace", source = "dbWorkspace")
  RecentWorkspace toApiRecentWorkspace(DbWorkspace dbWorkspace, WorkspaceAccessLevel accessLevel);

  /**
   * This method was written I think before we realized we could have multiple input arguments.
   *
   * @deprecated
   * @param workspace
   * @param researchPurpose
   */
  @Deprecated
  @Mapping(
      target = "specificPopulationsEnum",
      source = "populationDetails",
      nullValuePropertyMappingStrategy = SET_TO_DEFAULT)
  @Mapping(
      target = "disseminateResearchEnumSet",
      source = "disseminateResearchFindingList",
      nullValuePropertyMappingStrategy = SET_TO_DEFAULT)
  @Mapping(target = "disseminateResearchOther", source = "otherDisseminateResearchFindings")
  @Mapping(
      target = "researchOutcomeEnumSet",
      source = "researchOutcomeList",
      nullValuePropertyMappingStrategy = SET_TO_DEFAULT)

  // This method isn't a full conversion, so we need to mask out the values that don't
  // get set here.
  @Mapping(target = "adminLocked", ignore = true)
  @Mapping(target = "adminLockedReason", ignore = true)
  @Mapping(target = "approved", ignore = true)
  @Mapping(target = "billingAccountName", ignore = true)
  @Mapping(target = "billingStatus", ignore = true)
  @Mapping(target = "cdrVersion", ignore = true)
  @Mapping(target = "cohorts", ignore = true)
  @Mapping(target = "conceptSets", ignore = true)
  @Mapping(target = "creationTime", ignore = true)
  @Mapping(target = "creator", ignore = true)
  @Mapping(target = "dataSets", ignore = true)
  @Mapping(target = "disseminateResearchSet", ignore = true)
  @Mapping(target = "firecloudName", ignore = true)
  @Mapping(target = "firecloudUuid", ignore = true)
  @Mapping(target = "googleProject", ignore = true)
  @Mapping(target = "lastModifiedBy", ignore = true)
  @Mapping(target = "lastModifiedTime", ignore = true)
  @Mapping(target = "name", ignore = true)
  @Mapping(target = "published", ignore = true)
  @Mapping(target = "researchOutcomeSet", ignore = true)
  @Mapping(target = "reviewRequested", ignore = true)
  @Mapping(target = "timeRequested", ignore = true)
  @Mapping(target = "version", ignore = true)
  @Mapping(target = "workspaceActiveStatusEnum", ignore = true)
  @Mapping(target = "workspaceId", ignore = true)
  @Mapping(target = "workspaceNamespace", ignore = true)
  void mergeResearchPurposeIntoWorkspace(
      @MappingTarget DbWorkspace workspace, ResearchPurpose researchPurpose);

  default String cdrVersionId(CdrVersion cdrVersion) {
    return String.valueOf(cdrVersion.getCdrVersionId());
  }
}
