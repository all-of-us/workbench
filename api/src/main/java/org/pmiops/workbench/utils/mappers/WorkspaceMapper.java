package org.pmiops.workbench.utils.mappers;

import static org.mapstruct.NullValuePropertyMappingStrategy.SET_TO_DEFAULT;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.initialcredits.InitialCreditsExpirationService;
import org.pmiops.workbench.model.CdrVersion;
import org.pmiops.workbench.model.RecentWorkspace;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.TestUserRawlsWorkspace;
import org.pmiops.workbench.model.TestUserWorkspace;
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
  @Mapping(
      target = "initialCredits.expirationEpochMillis",
      source = "dbWorkspace.creator",
      qualifiedByName = "getInitialCreditsExpiration")
  @Mapping(target = "cdrVersionId", source = "dbWorkspace.cdrVersion")
  @Mapping(target = "accessTierShortName", source = "dbWorkspace.cdrVersion.accessTier.shortName")
  @Mapping(target = "googleProject", source = "dbWorkspace.googleProject")
  @Mapping(target = "initialCredits.exhausted", source = "dbWorkspace.initialCreditsExhausted")
  @Mapping(target = "initialCredits.expired", source = "dbWorkspace.initialCreditsExpired")
  @Mapping(target = "usesTanagra", source = "dbWorkspace.usesTanagra")
  Workspace toApiWorkspace(
      DbWorkspace dbWorkspace,
      RawlsWorkspaceDetails fcWorkspace,
      @Context InitialCreditsExpirationService expirationService);

  @Mapping(
      target = "accessLevel",
      source = "accessLevel",
      qualifiedByName = "fcToApiWorkspaceAccessLevel")
  WorkspaceResponse toApiWorkspaceResponse(
      Workspace workspace, RawlsWorkspaceAccessLevel accessLevel);

  default List<WorkspaceResponse> toApiWorkspaceResponseList(
      WorkspaceDao workspaceDao,
      List<RawlsWorkspaceListResponse> fcWorkspaces,
      InitialCreditsExpirationService expirationService) {
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
                  toApiWorkspace(dbWorkspace, fcResponse.getWorkspace(), expirationService),
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

  @Mapping(target = "cdrVersionId", source = "cdrVersion")
  @Mapping(target = "creator", source = "creator.username")
  @Mapping(target = "initialCredits.expired", source = "dbWorkspace.initialCreditsExpired")
  @Mapping(
      target = "initialCredits.expirationEpochMillis",
      source = "creator",
      qualifiedByName = "getInitialCreditsExpiration")
  @Mapping(target = "initialCredits.exhausted", source = "dbWorkspace.initialCreditsExhausted")
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
  // provides an incomplete workspace!  Only for use by the RecentWorkspace mapper
  Workspace onlyForMappingRecentWorkspace(
      DbWorkspace dbWorkspace, @Context InitialCreditsExpirationService expirationService);

  @Mapping(target = "workspace", source = "dbWorkspace")
  RecentWorkspace toApiRecentWorkspace(
      DbWorkspace dbWorkspace,
      WorkspaceAccessLevel accessLevel,
      @Context InitialCreditsExpirationService expirationService);

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
  @Mapping(target = "researchOutcomeSet", ignore = true)
  @Mapping(target = "reviewRequested", ignore = true)
  @Mapping(target = "timeRequested", ignore = true)
  @Mapping(target = "version", ignore = true)
  @Mapping(target = "workspaceActiveStatusEnum", ignore = true)
  @Mapping(target = "workspaceId", ignore = true)
  @Mapping(target = "workspaceNamespace", ignore = true)
  @Mapping(target = "featuredCategory", ignore = true)
  @Mapping(target = "initialCreditsExpired", ignore = true)
  @Mapping(target = "initialCreditsExhausted", ignore = true)
  @Mapping(target = "usesTanagra", ignore = true)
  void mergeResearchPurposeIntoWorkspace(
      @MappingTarget DbWorkspace workspace, ResearchPurpose researchPurpose);

  default String cdrVersionId(CdrVersion cdrVersion) {
    return String.valueOf(cdrVersion.getCdrVersionId());
  }

  TestUserWorkspace toTestUserWorkspace(Workspace workspace, String username);

  @Mapping(target = "terraName", source = "workspace.name")
  TestUserRawlsWorkspace toTestUserRawlsWorkspace(RawlsWorkspaceDetails workspace, String username);
}
