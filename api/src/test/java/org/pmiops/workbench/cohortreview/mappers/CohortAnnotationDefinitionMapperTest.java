package org.pmiops.workbench.cohortreview.mappers;

import static com.google.common.truth.Truth.assertThat;

import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.api.Etags;
import org.pmiops.workbench.db.model.DbCohortAnnotationDefinition;
import org.pmiops.workbench.db.model.DbCohortAnnotationEnumValue;
import org.pmiops.workbench.db.model.DbStorageEnums;
import org.pmiops.workbench.model.AnnotationType;
import org.pmiops.workbench.model.CohortAnnotationDefinition;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class CohortAnnotationDefinitionMapperTest {

  @Autowired private CohortAnnotationDefinitionMapper cohortAnnotationDefinitionMapper;

  @TestConfiguration
  @Import({CohortAnnotationDefinitionMapperImpl.class, CommonMappers.class})
  static class Configuration {}

  @Test
  public void dbModelToClient() {
    SortedSet enumValues = new TreeSet();
    enumValues.add(new DbCohortAnnotationEnumValue().name("name"));
    CohortAnnotationDefinition expectedCohortAnnotationDefinition =
        new CohortAnnotationDefinition()
            .columnName("column_name")
            .cohortId(1L)
            .annotationType(AnnotationType.STRING)
            .etag(Etags.fromVersion(0))
            .enumValues(Arrays.asList("name"))
            .cohortAnnotationDefinitionId(1L);
    assertThat(
            cohortAnnotationDefinitionMapper.dbModelToClient(
                new DbCohortAnnotationDefinition()
                    .cohortAnnotationDefinitionId(1L)
                    .cohortId(1L)
                    .columnName("column_name")
                    .version(0)
                    .annotationType(DbStorageEnums.annotationTypeToStorage(AnnotationType.STRING))
                    .enumValues(enumValues)))
        .isEqualTo(expectedCohortAnnotationDefinition);
  }

  @Test
  public void clientToDbModel() {
    DbCohortAnnotationDefinition expectedDbCohortAnnotationDefinition =
        new DbCohortAnnotationDefinition()
            .cohortAnnotationDefinitionId(1L)
            .cohortId(1L)
            .columnName("column_name")
            .version(0)
            .annotationType(DbStorageEnums.annotationTypeToStorage(AnnotationType.STRING));
    expectedDbCohortAnnotationDefinition
        .getEnumValues()
        .add(
            new DbCohortAnnotationEnumValue()
                .name("name")
                .cohortAnnotationDefinition(expectedDbCohortAnnotationDefinition)
                .order(1)
                .cohortAnnotationEnumValueId(1L));
    assertThat(
            cohortAnnotationDefinitionMapper.clientToDbModel(
                new CohortAnnotationDefinition()
                    .columnName("column_name")
                    .cohortId(1L)
                    .annotationType(AnnotationType.STRING)
                    .etag(Etags.fromVersion(0))
                    .enumValues(Arrays.asList("name"))
                    .cohortAnnotationDefinitionId(1L)))
        .isEqualTo(expectedDbCohortAnnotationDefinition);
  }
}
