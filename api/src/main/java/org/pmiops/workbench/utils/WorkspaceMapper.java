package org.pmiops.workbench.utils;

import static org.mapstruct.NullValuePropertyMappingStrategy.*;

import org.mapstruct.CollectionMappingStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceAccessEntry;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.pmiops.workbench.model.CdrVersion;
import org.pmiops.workbench.model.RecentWorkspace;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.model.WorkspaceResource;
import org.pmiops.workbench.model.WorkspaceResponse;
import org.pmiops.workbench.utils.mappers.CommonMappers;

@Mapper(
    componentModel = "spring",
    collectionMappingStrategy = CollectionMappingStrategy.TARGET_IMMUTABLE,
    uses = {CommonMappers.class})
public interface WorkspaceMapper {

  @Mapping(target = "researchPurpose", source = "dbWorkspace")
  @Mapping(target = "etag", source = "dbWorkspace.version", qualifiedByName = "cdrVersionToEtag")
  @Mapping(target = "name", source = "dbWorkspace.name")
  @Mapping(target = "id", source = "fcWorkspace.name")
  @Mapping(target = "googleBucketName", source = "fcWorkspace.bucketName")
  @Mapping(target = "creator", source = "fcWorkspace.createdBy")
  @Mapping(target = "cdrVersionId", source = "dbWorkspace.cdrVersion")
  Workspace toApiWorkspace(DbWorkspace dbWorkspace, FirecloudWorkspace fcWorkspace);

  @Mapping(target = "researchPurpose", source = "dbWorkspace")
  @Mapping(target = "etag", source = "version", qualifiedByName = "cdrVersionToEtag")
  @Mapping(target = "id", source = "firecloudName")
  @Mapping(target = "namespace", source = "workspaceNamespace")
  @Mapping(target = "creator", source = "creator.username")
  @Mapping(target = "cdrVersionId", source = "cdrVersion")
  Workspace toApiWorkspace(DbWorkspace dbWorkspace);

  @Mapping(target = "workspace", source = "dbWorkspace")
  @Mapping(target = "accessLevel", source = "firecloudWorkspaceResponse")
  WorkspaceResponse toApiWorkspaceResponse(
      DbWorkspace dbWorkspace, FirecloudWorkspaceResponse firecloudWorkspaceResponse);

  @Mapping(target = "timeReviewed", ignore = true)
  @Mapping(target = "populationDetails", source = "specificPopulationsEnum")
  @Mapping(target = "researchOutcomeList", source = "researchOutcomeEnumSet")
  @Mapping(target = "disseminateResearchFindingList", source = "disseminateResearchEnumSet")
  @Mapping(target = "otherDisseminateResearchFindings", source = "disseminateResearchOther")
  ResearchPurpose workspaceToResearchPurpose(DbWorkspace dbWorkspace);

  @Mapping(target = "workspace", source = "dbWorkspace")
  RecentWorkspace toApiRecentWorkspace(DbWorkspace dbWorkspace, WorkspaceAccessLevel accessLevel);

  // I believe the following fields are ignored because they are only meant to be set once
  // My intent was to keep the same functionality as in the original mapper so I left it in
  // but we should be handling special business case logic like this in our controller/services
  @Mapping(target = "approved", ignore = true)
  @Mapping(target = "reviewRequested", ignore = true)
  @Mapping(target = "timeRequested", ignore = true)
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
  void mergeResearchPurposeIntoWorkspace(
      @MappingTarget DbWorkspace workspace, ResearchPurpose researchPurpose);

  @Mapping(target = "email", source = "user.username")
  @Mapping(target = "role", source = "acl")
  UserRole toApiUserRole(DbUser user, FirecloudWorkspaceAccessEntry acl);

  @Mapping(target = "workspaceFirecloudName", source = "dbWorkspace.firecloudName")
  @Mapping(target = "workspaceBillingStatus", source = "dbWorkspace.billingStatus")
  @Mapping(target = "permission", source = "accessLevel")
  @Mapping(target = "cohort", ignore = true) // All workspaceResources have one object and all others are null. That should be defined by a setter where used
  @Mapping(target = "cohortReview", ignore = true) // All workspaceResources have one object and all others are null. That should be defined by a setter where used
  @Mapping(target = "conceptSet", ignore = true) // All workspaceResources have one object and all others are null. That should be defined by a setter where used
  @Mapping(target = "dataSet", ignore = true) // All workspaceResources have one object and all others are null. That should be defined by a setter where used
  @Mapping(target = "notebook", ignore = true) // All workspaceResources have one object and all others are null. That should be defined by a setter where used
  @Mapping(target = "modifiedTime", ignore = true) // This should be set when the resource is set
  WorkspaceResource workspaceResourceFromDbWorkspace(DbWorkspace dbWorkspace, WorkspaceAccessLevel accessLevel);

  default String cdrVersionId(CdrVersion cdrVersion) {
    return String.valueOf(cdrVersion.getCdrVersionId());
  }
}
