package org.pmiops.workbench.cohortreview.mappers;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.pmiops.workbench.db.model.DbCohortAnnotationDefinition;
import org.pmiops.workbench.db.model.DbCohortAnnotationEnumValue;
import org.pmiops.workbench.model.CohortAnnotationDefinition;
import org.pmiops.workbench.utils.mappers.CommonMappers;

@Mapper(
    componentModel = "spring",
    uses = {CommonMappers.class})
public interface CohortAnnotationDefinitionMapper {

  @Mapping(target = "etag", source = "version", qualifiedByName = "cdrVersionToEtag")
  @Mapping(target = "enumValues", source = "enumValues")
  CohortAnnotationDefinition dbModelToClient(DbCohortAnnotationDefinition source);

  @Mapping(target = "version", source = "etag", qualifiedByName = "etagToCdrVersion")
  @Mapping(target = "enumValues", source = "enumValues")
  @Mapping(target = "annotationTypeEnum", ignore = true)
  DbCohortAnnotationDefinition clientToDbModel(CohortAnnotationDefinition source);

  default List<String> toStringListFromEnumValues(
      SortedSet<DbCohortAnnotationEnumValue> enumValues) {
    return enumValues.stream()
        .map(DbCohortAnnotationEnumValue::getName)
        .collect(Collectors.toList());
  }

  default SortedSet<DbCohortAnnotationEnumValue> toEnumValuesFromStringList(
      List<String> enumValues) {
    return enumValues == null
        ? new TreeSet<>()
        : new TreeSet<>(
            IntStream.range(0, enumValues.size())
                .mapToObj(i -> new DbCohortAnnotationEnumValue().name(enumValues.get(i)).order(i))
                .collect(Collectors.toList()));
  }
}
