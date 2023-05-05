package org.pmiops.workbench.apiclients.leonardo;

import static org.pmiops.workbench.leonardo.LeonardoLabelHelper.mapDiskLabelsToDiskAppType;

import java.util.Map;
import org.broadinstitute.dsde.workbench.client.leonardo.model.GetPersistentDiskResponse;
import org.broadinstitute.dsde.workbench.client.leonardo.model.ListPersistentDiskResponse;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.pmiops.workbench.model.Disk;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(config = MapStructConfig.class)
public interface NewLeonardoMapper {
  @Mapping(target = "creator", source = "auditInfo.creator")
  @Mapping(target = "createdDate", source = "auditInfo.createdDate")
  @Mapping(target = "dateAccessed", source = "auditInfo.dateAccessed")
  // applied via getDiskAfterMapper() -> mapDiskLabelsToDiskAppType()
  @Mapping(target = "appType", ignore = true)
  // applied via getDiskAfterMapper() -> mapDiskLabelsToDiskAppType()
  @Mapping(target = "isGceRuntime", ignore = true)
  Disk toApiGetDiskResponse(GetPersistentDiskResponse response);

  @SuppressWarnings("unchecked")
  @AfterMapping
  default void getDiskAfterMapper(
      @MappingTarget Disk disk, GetPersistentDiskResponse leoGetDiskResponse) {
    mapDiskLabelsToDiskAppType(disk, (Map<String, String>) leoGetDiskResponse.getLabels());
  }

  @Mapping(target = "creator", source = "auditInfo.creator")
  @Mapping(target = "createdDate", source = "auditInfo.createdDate")
  @Mapping(target = "dateAccessed", source = "auditInfo.dateAccessed")
  // applied via listDisksAfterMapper() -> mapDiskLabelsToDiskAppType()
  @Mapping(target = "appType", ignore = true)
  // applied via listDisksAfterMapper() -> mapDiskLabelsToDiskAppType()
  @Mapping(target = "isGceRuntime", ignore = true)
  Disk toApiListDisksResponse(ListPersistentDiskResponse listPersistentDiskResponse);

  @SuppressWarnings("unchecked")
  @AfterMapping
  default void listDisksAfterMapper(
      @MappingTarget Disk disk, ListPersistentDiskResponse leoListDisksResponse) {
    mapDiskLabelsToDiskAppType(disk, (Map<String, String>) leoListDisksResponse.getLabels());
  }
}
