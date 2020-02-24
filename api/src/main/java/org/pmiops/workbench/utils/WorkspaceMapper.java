package org.pmiops.workbench.utils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.model.CdrVersion;
import org.pmiops.workbench.model.DisseminateResearchEnum;
import org.pmiops.workbench.model.ResearchOutcomeEnum;
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
  @Mapping(target = "email", source = "username")
  UserRole userToUserRole(DbUser user);

  default WorkspaceAccessLevel roleToWorkspaceAccessLevel(Short dataAccessLevel) {
    return DbStorageEnums.workspaceAccessLevelFromStorage(dataAccessLevel);
  }

  @Mapping(target = "researchPurpose", source = "dbWorkspace")
  @Mapping(target = "etag", source = "dbWorkspace.version", qualifiedByName = "cdrVersionToEtag")
  @Mapping(target = "dataAccessLevel", source = "dbWorkspace.dataAccessLevelEnum")
  @Mapping(target = "name", source = "dbWorkspace.name")
  @Mapping(target = "id", source = "fcWorkspace.name")
  @Mapping(target = "googleBucketName", source = "fcWorkspace.bucketName")
  @Mapping(target = "creator", source = "fcWorkspace.createdBy")
  @Mapping(target = "cdrVersionId", source = "dbWorkspace.cdrVersion")
  Workspace toApiWorkspace(DbWorkspace dbWorkspace, FirecloudWorkspace fcWorkspace);

  // This method is simply merging the research purpose, which covers only a subset of the fields
  // in the DbWorkspace target
  @BeanMapping(ignoreByDefault = true)
  @Mapping(target = "specificPopulationsEnum", source = "populationDetails")
  void mergeResearchPurposeIntoWorkspace(
      @MappingTarget DbWorkspace dbWorkspace, ResearchPurpose researchPurpose);

  @Mapping(target = "timeReviewed", ignore = true)
  @Mapping(target = "disseminateResearchFindingList", ignore =  true)
  @Mapping(target = "otherDisseminateResearchFindings", ignore = true)
  @Mapping(target = "researchOutcomeList", ignore = true)
  ResearchPurpose workspaceToResearchPurpose(DbWorkspace dbWorkspace);

  // This method (defined by AfterMapping annotation) handles the scenario in which the name of the
  // parameters are different
  @AfterMapping
  default void afterWorkspaceIntoResearchPurpose(
      @MappingTarget ResearchPurpose researchPurpose, DbWorkspace dbWorkspace) {
    if (dbWorkspace.getPopulation()) {
      researchPurpose.setPopulationDetails(
          ImmutableList.copyOf(dbWorkspace.getSpecificPopulationsEnum()));
    }
    researchPurpose.setResearchOutcomeList(
        Optional.ofNullable(dbWorkspace.getResearchOutcomeEnumSet())
            .map(researchOutcome -> researchOutcome.stream().collect(Collectors.toList()))
            .orElse(new ArrayList<ResearchOutcomeEnum>()));

    researchPurpose.setDisseminateResearchFindingList(
        Optional.ofNullable(dbWorkspace.getDisseminateResearchEnumSet())
            .map(disseminateResearch -> disseminateResearch.stream().collect(Collectors.toList()))
            .orElse(new ArrayList<DisseminateResearchEnum>()));
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
    final Stream<Short> ordinalsStream;
    if (ordinals == null) {
      ordinalsStream = Stream.of();
    } else {
      ordinalsStream = ordinals.stream();
    }
    return ordinalsStream
        .map(DbStorageEnums::specificPopulationFromStorage)
        .collect(Collectors.toList());
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
