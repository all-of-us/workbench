package org.pmiops.workbench.profile;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.db.model.DbPageVisit;
import org.pmiops.workbench.model.PageVisit;
import org.pmiops.workbench.utils.mappers.CommonMappers;

@Mapper(
    componentModel = "spring",
    uses = {CommonMappers.class})
public interface PageVisitMapper {
  @Mapping(target = "page", source = "pageId")
  @Mapping(target = "userId", ignore = true) // set by ProfileService.getProfile
  PageVisit dbPageVisitToPageVisit(DbPageVisit dbPageVisit);

  @Mapping(target = "pageId", source = "page")
  @Mapping(target = "pageVisitId", ignore = true)
  @Mapping(target = "user", ignore = true) // set by ProfileController.updatePageVisits
  DbPageVisit pageVisitToDbPageVisit(PageVisit pageVisit);
}
