package org.pmiops.workbench.conceptset.mapper;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.stream.Collectors;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.cdr.model.DbConcept;
import org.pmiops.workbench.concept.ConceptService;
import org.pmiops.workbench.dataset.BigQueryTableInfo;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.ConceptSet;
import org.pmiops.workbench.model.CreateConceptSetRequest;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.utils.mappers.MapStructConfig;

@Mapper(
    config = MapStructConfig.class,
    uses = {CommonMappers.class})
public interface ConceptSetMapper {

  @Mapping(target = "id", source = "conceptSetId")
  @Mapping(target = "domain", source = "domainEnum")
  @Mapping(target = "survey", source = "surveysEnum")
  @Mapping(target = "etag", source = "version", qualifiedByName = "cdrVersionToEtag")
  @Mapping(target = "concepts", ignore = true)
  ConceptSet dbModelToClient(DbConceptSet source);

  @Mapping(target = "conceptSetId", source = "conceptSet.id")
  @Mapping(target = "domainEnum", source = "conceptSet.domain")
  @Mapping(target = "name", source = "conceptSet.name")
  @Mapping(target = "description", source = "conceptSet.description")
  @Mapping(
      target = "creationTime",
      source = "conceptSet.creationTime",
      qualifiedByName = "toTimestampCurrentIfNull")
  @Mapping(
      target = "lastModifiedTime",
      source = "conceptSet.lastModifiedTime",
      qualifiedByName = "toTimestampCurrentIfNull")
  @Mapping(target = "participantCount", source = "conceptSet.participantCount")
  @Mapping(target = "domain", ignore = true)
  @Mapping(target = "survey", ignore = true)
  @Mapping(target = "creator", ignore = true)
  @Mapping(target = "surveysEnum", source = "conceptSet.survey")
  @Mapping(target = "version", source = "conceptSet.etag", qualifiedByName = "etagToCdrVersion")
  @Mapping(target = "workspaceId", ignore = true)
  @Mapping(target = "conceptIds", ignore = true)
  DbConceptSet clientToDbModel(
      CreateConceptSetRequest source,
      @Context Long workspaceId,
      @Context DbUser creator,
      @Context ConceptService conceptService,
      @Context ConceptBigQueryService conceptBigQueryService);

  @AfterMapping
  default void addConceptIdsToSet(
      CreateConceptSetRequest source,
      @Context Long workspaceId,
      @Context DbUser creator,
      @MappingTarget DbConceptSet dbConceptSet,
      @Context ConceptService conceptService,
      @Context ConceptBigQueryService conceptBigQueryService) {
    dbConceptSet.setWorkspaceId(workspaceId);
    dbConceptSet.setCreator(creator);
    Domain domainEnum = dbConceptSet.getDomainEnum();
    if (domainEnum == null) {
      throw new BadRequestException(
          "Domain " + source.getConceptSet().getDomain() + " is not allowed for concept sets");
    }
    Iterable<DbConcept> concepts = conceptService.findAll(source.getAddedIds());
    if (dbConceptSet.getConceptIds().size() > 1000) {
      throw new BadRequestException("Exceeded 1000 in concept set");
    }
    List<DbConcept> mismatchedConcepts =
        ImmutableList.copyOf(concepts).stream()
            .filter(concept -> !concept.getConceptClassId().equals("Question"))
            .filter(
                concept -> {
                  Domain domain =
                      Domain.PHYSICALMEASUREMENT.equals(domainEnum)
                          ? Domain.PHYSICALMEASUREMENT
                          : DbStorageEnums.domainIdToDomain(concept.getDomainId());
                  return !domainEnum.equals(domain);
                })
            .collect(Collectors.toList());
    if (!mismatchedConcepts.isEmpty()) {
      String mismatchedConceptIds =
          Joiner.on(", ")
              .join(
                  mismatchedConcepts.stream()
                      .map(DbConcept::getConceptId)
                      .collect(Collectors.toList()));
      throw new BadRequestException(
          String.format("Concepts [%s] are not in domain %s", mismatchedConceptIds, domainEnum));
    }
    dbConceptSet.getConceptIds().addAll(source.getAddedIds());
    String omopTable = BigQueryTableInfo.getTableName(source.getConceptSet().getDomain());
    dbConceptSet.setParticipantCount(
        conceptBigQueryService.getParticipantCountForConcepts(
            dbConceptSet.getDomainEnum(), omopTable, dbConceptSet.getConceptIds()));
    dbConceptSet.getConceptIds().addAll(source.getAddedIds());
  }
}
