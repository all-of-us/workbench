package org.pmiops.workbench.utils;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.CommonStorageEnums;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.utils.mappers.CommonMappers;

@Mapper(
    componentModel = "spring",
    uses = {
        CommonMappers.class,
        WorkspaceDao.class,
        UserDao.class
    },
    unmappedTargetPolicy = ReportingPolicy.IGNORE) // FIXME!!!
public interface WorkspaceMapper {

  @Mapping(source = "id", target = "workspaceId")
  @Mapping(source = "namespace", target = "workspaceNamespace")
  @Mapping(source = "", target = "firecloudName")
    //  "version, firecloudName, dataAccessLevelEnum,
    //  cdrVersion, lastAccessedTime, diseaseFocusedResearch,
    //  diseaseOfFocus, methodsDevelopment, controlSet, ancestry,
    //  commercialPurpose, socialBehavioral, populationHealth,
    //  educational, drugDevelopment, otherPurpose, otherPurposeDetails,
    //  population, populationDetails, specificPopulationsEnum,
    //  otherPopulationDetails, additionalNotes, reasonForAllOfUs,
    //  intendedStudy, anticipatedFindings, reviewRequested, approved,
    //  timeRequested, cohorts, conceptSets, dataSets, firecloudUuid,
    //  workspaceActiveStatusEnum, billingMigrationStatusEnum,
    //  billingStatus, billingAccountType".
    // I think we need an actual decorator to do the split mapping: https://mapstruct.org/documentation/1.0/reference/html/index.html#customizing-mappers-using-decorators
  DbWorkspace clientToDbModel(Workspace clientWorkspace);

  @Mapping(source = "workspaceId", target = "id")
  @Mapping(target = "etag", ignore = true)
  @Mapping(source = "workspaceNamespace", target = "namespace")
  @Mapping(source = "cdrVersion", target = "cdrVersionId")
  @Mapping(target = "googleBucketName", ignore = true)
  @Mapping(target = "researchPurpose", ignore = true) // need to map multiple fields into one...
  Workspace dbModelToClient(DbWorkspace dbWorkspace);

  @Mapping(target = "populationDetails", ignore = true)
  ResearchPurpose dbWorkspaceToResearchPurpose(DbWorkspace workspace);

}
