package org.pmiops.workbench.conceptset.mapper;

import java.sql.Timestamp;
import java.util.stream.Collectors;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.pmiops.workbench.cdr.ConceptBigQueryService;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.db.model.DbConceptSetConceptId;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.ConceptSet;
import org.pmiops.workbench.model.CreateConceptSetRequest;
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
  @Mapping(target = "criteriums", ignore = true)
  @Mapping(target = "participantCount", ignore = true)
  ConceptSet dbModelToClient(DbConceptSet source);

  @Mapping(target = "id", source = "conceptSetId")
  @Mapping(target = "domain", source = "domainEnum")
  @Mapping(target = "survey", source = "surveysEnum")
  @Mapping(target = "etag", source = "version", qualifiedByName = "cdrVersionToEtag")
  @Mapping(target = "criteriums", ignore = true)
  @Mapping(target = "participantCount", ignore = true)
  ConceptSet dbModelToClient(
      DbConceptSet source, @Context ConceptBigQueryService conceptBigQueryService);

  @AfterMapping
  default void afterMappingDbModelToClient(
      DbConceptSet source,
      @MappingTarget ConceptSet conceptSet,
      @Context ConceptBigQueryService conceptBigQueryService) {
    conceptSet.setParticipantCount(
        conceptBigQueryService.getParticipantCountForConcepts(
            source.getDomainEnum(), source.getConceptSetConceptIds()));
  }

  @Mapping(target = "conceptSetId", ignore = true)
  @Mapping(target = "name", ignore = true)
  @Mapping(target = "creator", ignore = true)
  @Mapping(target = "workspaceId", ignore = true)
  @Mapping(target = "creationTime", ignore = true)
  @Mapping(target = "lastModifiedTime", ignore = true)
  @Mapping(target = "version", ignore = true)
  DbConceptSet dbModelToDbModel(
      DbConceptSet dbConceptSet, @Context ConceptSetContext conceptSetContext);

  @AfterMapping
  default void afterMappingDbModelToDbModel(
      @MappingTarget DbConceptSet dbConceptSet, @Context ConceptSetContext conceptSetContext) {
    dbConceptSet.setName(conceptSetContext.getName());
    dbConceptSet.setCreator(conceptSetContext.getCreator());
    dbConceptSet.setWorkspaceId(conceptSetContext.getWorkspaceId());
    dbConceptSet.setCreationTime(conceptSetContext.getCreationTime());
    dbConceptSet.setLastModifiedTime(conceptSetContext.getLastModifiedTime());
    dbConceptSet.setVersion(conceptSetContext.getVersion());
  }

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
  @Mapping(target = "domain", ignore = true)
  @Mapping(target = "survey", ignore = true)
  @Mapping(target = "creator", ignore = true)
  @Mapping(target = "surveysEnum", source = "conceptSet.survey")
  @Mapping(target = "version", source = "conceptSet.etag", qualifiedByName = "etagToCdrVersion")
  @Mapping(target = "workspaceId", ignore = true)
  @Mapping(target = "conceptSetConceptIds", ignore = true)
  DbConceptSet clientToDbModel(
      CreateConceptSetRequest source, @Context Long workspaceId, @Context DbUser creator);

  @AfterMapping
  default void afterMappingClientToDbModel(
      CreateConceptSetRequest source,
      @Context Long workspaceId,
      @Context DbUser creator,
      @MappingTarget DbConceptSet dbConceptSet) {
    dbConceptSet.setWorkspaceId(workspaceId);
    dbConceptSet.setCreator(creator);
    dbConceptSet
        .getConceptSetConceptIds()
        .addAll(
            source.getAddedConceptSetConceptIds().stream()
                .map(
                    c ->
                        DbConceptSetConceptId.builder()
                            .addConceptId(c.getConceptId())
                            .addStandard(c.getStandard())
                            .build())
                .collect(Collectors.toList()));
  }

  /**
   * Mapstruct throws an error(The types of @Context parameters must be unique) when you have 2 or
   * more of the same types using the @Context annotation. The 2 Timestamp objects for creationTime
   * and lastModifiedTime are the offending properties. This is an attempt to work around this
   * issue.
   */
  class ConceptSetContext {
    private String name;
    private DbUser creator;
    private Long workspaceId;
    private Timestamp creationTime;
    private Timestamp lastModifiedTime;
    private int version;

    public ConceptSetContext(Builder builder) {
      this.name = builder.name;
      this.creator = builder.creator;
      this.workspaceId = builder.workspaceId;
      this.creationTime = builder.creationTime;
      this.lastModifiedTime = builder.lastModifiedTime;
      this.version = builder.version;
    }

    public String getName() {
      return name;
    }

    public DbUser getCreator() {
      return creator;
    }

    public Long getWorkspaceId() {
      return workspaceId;
    }

    public Timestamp getCreationTime() {
      return creationTime;
    }

    public Timestamp getLastModifiedTime() {
      return lastModifiedTime;
    }

    public int getVersion() {
      return version;
    }

    public static class Builder {
      private String name;
      private DbUser creator;
      private Long workspaceId;
      private Timestamp creationTime;
      private Timestamp lastModifiedTime;
      private int version;

      public Builder name(String name) {
        this.name = name;
        return this;
      }

      public Builder creator(DbUser creator) {
        this.creator = creator;
        return this;
      }

      public Builder workspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
        return this;
      }

      public Builder creationTime(Timestamp creationTime) {
        this.creationTime = creationTime;
        return this;
      }

      public Builder lastModifiedTime(Timestamp lastModifiedTime) {
        this.lastModifiedTime = lastModifiedTime;
        return this;
      }

      public Builder version(int version) {
        this.version = version;
        return this;
      }

      public ConceptSetContext build() {
        return new ConceptSetContext(this);
      }
    }
  }
}
