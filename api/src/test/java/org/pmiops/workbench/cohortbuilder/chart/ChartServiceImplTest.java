package org.pmiops.workbench.cohortbuilder.chart;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.cohortbuilder.mapper.CohortBuilderMapperImpl;
import org.pmiops.workbench.cohortreview.mapper.CohortReviewMapperImpl;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.model.AgeType;
import org.pmiops.workbench.model.GenderOrSexType;
import org.pmiops.workbench.utils.mappers.CommonMappers;
import org.pmiops.workbench.workspaces.WorkspaceAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ChartServiceImplTest {

  private ChartServiceImpl chartService;
  @Mock private BigQueryService bigQueryService;
  @Mock private ChartQueryBuilder chartQueryBuilder;
  @Autowired private CohortBuilderMapperImpl cohortBuilderMapper;
  @Autowired private CohortReviewMapperImpl cohortReviewMapper;

  @TestConfiguration
  @Import({
    FakeClockConfiguration.class,
    CommonMappers.class,
    CohortBuilderMapperImpl.class,
    CohortReviewMapperImpl.class
  })
  @MockBean({WorkspaceAuthService.class})
  static class Configuration {}

  @BeforeEach
  public void setUp() {

    chartService =
        new ChartServiceImpl(
            bigQueryService, chartQueryBuilder, cohortBuilderMapper, cohortReviewMapper);
  }

  @Test
  public void validateAgeTypeCaseSensitive() {
    List<String> values =
        Stream.of(AgeType.values()).map(AgeType::toString).collect(Collectors.toList());
    for (String value : values) {
      assertDoesNotThrow(
          () -> chartService.validateAgeType(value),
          "BadRequestException is not expected to be thrown for [" + value + "]");
    }
    // expect exception for lowercase
    values.replaceAll(String::toLowerCase);
    for (String value : values) {
      Throwable exception =
          assertThrows(
              BadRequestException.class,
              () -> chartService.validateAgeType(value),
              "Expected BadRequestException is not thrown.");
      assertThat(exception).hasMessageThat().contains("Please provide a valid age type parameter");
    }
  }

  @Test
  public void validateGenderOrSexTypeCaseSensitive() {
    List<String> values =
        Stream.of(GenderOrSexType.values())
            .map(GenderOrSexType::toString)
            .collect(Collectors.toList());
    for (String value : values) {
      assertDoesNotThrow(
          () -> chartService.validateGenderOrSexType(value),
          "BadRequestException is not expected to be thrown for [" + value + "]");
    }
    // expect exception for lowercase
    values.replaceAll(String::toLowerCase);
    for (String value : values) {
      Throwable exception =
          assertThrows(
              BadRequestException.class,
              () -> chartService.validateGenderOrSexType(value),
              "Expected BadRequestException is not thrown.");
      assertThat(exception)
          .hasMessageThat()
          .contains("Please provide a valid gender or sex at birth parameter");
    }
  }
}
