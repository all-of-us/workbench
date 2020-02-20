package org.pmiops.workbench.conceptset;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.db.model.DbConceptSet;
import org.pmiops.workbench.model.ConceptSet;
import org.pmiops.workbench.utils.mappers.CommonMappers;

@Mapper(
    componentModel = "spring",
    uses = {CommonMappers.class})
public interface ConceptSetMapper {

  @Mapping(target = "id", source = "conceptSetId")
  @Mapping(target = "domain", source = "domainEnum")
  @Mapping(target = "survey", source = "surveysEnum")
  @Mapping(target = "etag", source = "version", qualifiedByName = "cdrVersionToEtag")
  @Mapping(target = "concepts", ignore = true)
  ConceptSet dbModelToClient(DbConceptSet source);
}
