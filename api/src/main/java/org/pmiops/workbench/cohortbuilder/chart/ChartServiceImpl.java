package org.pmiops.workbench.cohortbuilder.chart;

import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.cohortbuilder.ParticipantCriteria;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.pmiops.workbench.cohortbuilder.mapper.CohortBuilderMapper;
import org.pmiops.workbench.cohortreview.mapper.CohortReviewMapper;
import org.pmiops.workbench.model.AgeType;
import org.pmiops.workbench.model.CohortChartData;
import org.pmiops.workbench.model.DemoChartInfo;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.EthnicityInfo;
import org.pmiops.workbench.model.GenderOrSexType;
import org.pmiops.workbench.model.ParticipantChartData;
import org.pmiops.workbench.model.SearchRequest;
import org.springframework.stereotype.Service;

@Service
public class ChartServiceImpl implements ChartService {
  private static final String BAD_REQUEST_MESSAGE =
      "Bad Request: Please provide a valid %s. %s is not valid.";
  private final BigQueryService bigQueryService;

  private final ChartQueryBuilder chartQueryBuilder;

  private final CohortBuilderMapper cohortBuilderMapper;

  private final CohortReviewMapper cohortReviewMapper;

  public ChartServiceImpl(
      BigQueryService bigQueryService,
      ChartQueryBuilder chartQueryBuilder,
      CohortBuilderMapper cohortBuilderMapper,
      CohortReviewMapper cohortReviewMapper) {
    this.bigQueryService = bigQueryService;
    this.chartQueryBuilder = chartQueryBuilder;
    this.cohortBuilderMapper = cohortBuilderMapper;
    this.cohortReviewMapper = cohortReviewMapper;
  }

  @Override
  public List<CohortChartData> findCohortChartData(
      SearchRequest searchRequest, Domain domain, int limit) {
    QueryJobConfiguration qjc =
        chartQueryBuilder.buildDomainChartInfoCounterQuery(
            new ParticipantCriteria(searchRequest), domain, limit);

    return fetchCohortChartData(qjc);
  }

  @Override
  public List<CohortChartData> findCohortReviewChartData(
      Long cohortReviewId, Domain domain, int limit) {
    List<Long> participantIds = null;
    QueryJobConfiguration qjc =
        chartQueryBuilder.buildDomainChartInfoCounterQuery(participantIds, domain, limit);

    return fetchCohortChartData(qjc);

  }

  @Override
  public List<DemoChartInfo> findDemoChartInfo(
      GenderOrSexType genderOrSexType, AgeType ageType, SearchRequest request) {
    TableResult result =
        bigQueryService.filterBigQueryConfigAndExecuteQuery(
            chartQueryBuilder.buildDemoChartInfoCounterQuery(
                new ParticipantCriteria(request, genderOrSexType, ageType)));

    return cohortBuilderMapper.tableResultToDemoChartInfo(result);
  }

  @Override
  public List<EthnicityInfo> findEthnicityInfo(SearchRequest request) {
    QueryJobConfiguration qjc =
        bigQueryService.filterBigQueryConfig(
            chartQueryBuilder.buildEthnicityInfoCounterQuery(new ParticipantCriteria(request)));
    TableResult result = bigQueryService.executeQuery(qjc);
    Map<String, Integer> rm = bigQueryService.getResultMapper(result);

    List<EthnicityInfo> ethnicityInfos = new ArrayList<>();
    for (List<FieldValue> row : result.iterateAll()) {
      ethnicityInfos.add(
          new EthnicityInfo()
              .ethnicity(bigQueryService.getString(row, rm.get("ethnicity")))
              .count(bigQueryService.getLong(row, rm.get("count"))));
    }
    return ethnicityInfos;
  }

  @Override
  public List<ParticipantChartData> findParticipantChartData(
      Long participantId, Domain domain, int limit) {
    TableResult result =
        bigQueryService.executeQuery(
            bigQueryService.filterBigQueryConfig(
                chartQueryBuilder.buildChartDataQuery(participantId, domain, limit)));
    Map<String, Integer> rm = bigQueryService.getResultMapper(result);
    List<ParticipantChartData> participantChartData = new ArrayList<>();
    for (List<FieldValue> row : result.iterateAll()) {
      participantChartData.add(
          new ParticipantChartData()
              .standardName(bigQueryService.getString(row, rm.get("standardName")))
              .standardVocabulary(bigQueryService.getString(row, rm.get("standardVocabulary")))
              .startDate(bigQueryService.getDate(row, rm.get("startDate")))
              .ageAtEvent(bigQueryService.getLong(row, rm.get("ageAtEvent")).intValue())
              .rank(bigQueryService.getLong(row, rm.get("rank")).intValue()));
    }
    return participantChartData;
  }

  @NotNull
  private List<CohortChartData> fetchCohortChartData(QueryJobConfiguration qjc) {
    TableResult result = bigQueryService.executeQuery(bigQueryService.filterBigQueryConfig(qjc));
    Map<String, Integer> rm = bigQueryService.getResultMapper(result);

    List<CohortChartData> cohortChartData = new ArrayList<>();
    for (List<FieldValue> row : result.iterateAll()) {
      cohortChartData.add(
          new CohortChartData()
              .name(bigQueryService.getString(row, rm.get("name")))
              .conceptId(bigQueryService.getLong(row, rm.get("conceptId")))
              .count(bigQueryService.getLong(row, rm.get("count"))));
    }
    return cohortChartData;
  }

  @NotNull
  private List<DemoChartInfo> fetchDemoChartInfos(QueryJobConfiguration qjc) {
    TableResult result = bigQueryService.executeQuery(bigQueryService.filterBigQueryConfig(qjc));
    Map<String, Integer> rm = bigQueryService.getResultMapper(result);

    List<DemoChartInfo> demoChartInfos = new ArrayList<>();
    for (List<FieldValue> row : result.iterateAll()) {
      demoChartInfos.add(
          new DemoChartInfo()
              .name(bigQueryService.getString(row, rm.get("name")))
              .race(bigQueryService.getString(row, rm.get("race")))
              .ageRange(bigQueryService.getString(row, rm.get("ageRange")))
              .count(bigQueryService.getLong(row, rm.get("count"))));
    }
    return demoChartInfos;
  }

  protected AgeType validateAgeType(String age) {
    return Optional.ofNullable(age)
        .map(AgeType::fromValue)
        .orElseThrow(
            () ->
                new BadRequestException(
                    String.format(BAD_REQUEST_MESSAGE, "age type parameter", age)));
  }

  protected GenderOrSexType validateGenderOrSexType(String genderOrSex) {
    return Optional.ofNullable(genderOrSex)
        .map(GenderOrSexType::fromValue)
        .orElseThrow(
            () ->
                new BadRequestException(
                    String.format(
                        BAD_REQUEST_MESSAGE, "gender or sex at birth parameter", genderOrSex)));
  }
}
