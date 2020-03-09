package org.pmiops.workbench.utils;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.model.CdrVersion;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.SpecificPopulationEnum;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;
import org.pmiops.workbench.utils.mappers.CommonMappers;

@Mapper(
    componentModel = "spring",
    uses = {CommonMappers.class})
public interface WorkspaceMapper {

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

  @Mapping(target = "timeReviewed", ignore = true)
  @Mapping(target = "populationDetails", source = "specificPopulationsEnum")
  @Mapping(target = "researchOutcomeList", source = "researchOutcomeEnumSet")
  @Mapping(target = "disseminateResearchFindingList", source = "disseminateResearchEnumSet")
  ResearchPurpose workspaceToResearchPurpose(DbWorkspace dbWorkspace);

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
}
