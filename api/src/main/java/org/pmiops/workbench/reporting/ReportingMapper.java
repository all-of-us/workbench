package org.pmiops.workbench.reporting;

import org.mapstruct.Mapper;
import org.pmiops.workbench.db.model.DbPdrResearcher;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(config = MapStructConfig.class)
public interface ReportingMapper {
  PdrResearcherRow toModel(DbPdrResearcher dbPdrResearcher);
}
