package org.pmiops.workbench.utils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Set;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.CdrVersion;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.SpecificPopulationEnum;
import org.pmiops.workbench.model.UserRole;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.workspaces.WorkspaceService;

@Mapper(
    componentModel = "spring",
    uses = {CommonMappers.class})
public interface WorkspaceMapper {

  @Mapping(target = "role", source = "dataAccessLevel")
  UserRole userToUserRole(DbUser user);

  default WorkspaceAccessLevel roleToWorkspaceAccessLevel(Short dataAccessLevel) {
    return DbStorageEnums.workspaceAccessLevelFromStorage(dataAccessLevel);
  }

  @Mapping(target = "researchPurpose", source = "dbWorkspace")
  @Mapping(target = "etag", source = "dbWorkspace.version", qualifiedByName = "etag")
  @Mapping(target = "dataAccessLevel", source = "dbWorkspace.dataAccessLevelEnum")
  @Mapping(target = "name", source = "dbWorkspace.name")
  @Mapping(target = "id", source = "fcWorkspace.name")
  @Mapping(target = "googleBucketName", source = "fcWorkspace.bucketName")
  @Mapping(target = "creator", source = "fcWorkspace.createdBy")
  @Mapping(target = "cdrVersionId", source = "dbWorkspace.cdrVersion")
  Workspace toApiWorkspace(
      DbWorkspace dbWorkspace, org.pmiops.workbench.firecloud.model.Workspace fcWorkspace);

  // This method is simply merging the research purpose, which covers only a subset of the fields
  // in the DbWorkspace target
  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "specificPopulationsEnum", source = "populationDetails")
  void mergeResearchPurposeIntoWorkspace(
      @MappingTarget DbWorkspace dbWorkspace, ResearchPurpose researchPurpose);

  @Mapping(target = "timeReviewed", ignore = true)
  ResearchPurpose workspaceToResearchPurpose(DbWorkspace dbWorkspace);

  @AfterMapping
  default void afterWorkspaceIntoResearchPurpose(
      @MappingTarget ResearchPurpose researchPurpose, DbWorkspace dbWorkspace) {
    if (dbWorkspace.getPopulation()) {
      researchPurpose.setPopulationDetails(
          ImmutableList.copyOf(dbWorkspace.getSpecificPopulationsEnum()));
    }
  }

  @AfterMapping
  default void afterResearchPurposeIntoWorkspace(
      @MappingTarget DbWorkspace dbWorkspace, ResearchPurpose researchPurpose) {
    if (researchPurpose.getPopulation()) {
      dbWorkspace.setSpecificPopulationsEnum(
          ImmutableSet.copyOf(researchPurpose.getPopulationDetails()));
    }
  }

  default Set<Short> map(List<SpecificPopulationEnum> value) {
    return value.stream()
        .map(DbStorageEnums::specificPopulationToStorage)
        .collect(ImmutableSet.toImmutableSet());
  }

  default List<SpecificPopulationEnum> ordinalsToSpecificPopulationEnumList(Set<Short> ordinals) {
    return ordinals.stream()
        .map(DbStorageEnums::specificPopulationFromStorage)
        .collect(ImmutableList.toImmutableList());
  }

  default String cdrVersionId(CdrVersion cdrVersion) {
    return String.valueOf(cdrVersion.getCdrVersionId());
  }

  default WorkspaceAccessLevel fromFcAccessLevel(String firecloudAccessLevel) {
    if (firecloudAccessLevel.equals(WorkspaceService.PROJECT_OWNER_ACCESS_LEVEL)) {
      return WorkspaceAccessLevel.OWNER;
    } else {
      return WorkspaceAccessLevel.fromValue(firecloudAccessLevel);
    }
  }
}
