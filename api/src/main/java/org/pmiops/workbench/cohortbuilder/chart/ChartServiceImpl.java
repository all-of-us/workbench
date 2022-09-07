package org.pmiops.workbench.cohortbuilder.chart;

import com.google.cloud.bigquery.TableResult;
import java.util.List;
import java.util.Set;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.cohortbuilder.ParticipantCriteria;
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
    TableResult result =
        bigQueryService.filterBigQueryConfigAndExecuteQuery(
            chartQueryBuilder.buildDomainChartInfoCounterQuery(
                new ParticipantCriteria(searchRequest), domain, limit));

    return cohortBuilderMapper.tableResultToCohortChartData(result);
  }

  @Override
  public List<CohortChartData> findCohortReviewChartData(
      Set<Long> participantIds, Domain domain, int limit) {
    TableResult result =
        bigQueryService.filterBigQueryConfigAndExecuteQuery(
            chartQueryBuilder.buildDomainChartInfoCounterQuery(participantIds, domain, limit));

    return cohortBuilderMapper.tableResultToCohortChartData(result);
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
  public List<DemoChartInfo> findCohortReviewDemoChartInfo(Set<Long> participantIds) {
    TableResult result =
        bigQueryService.filterBigQueryConfigAndExecuteQuery(
            chartQueryBuilder.buildDemoChartInfoCounterQuery(participantIds));

    return cohortBuilderMapper.tableResultToDemoChartInfo(result);
  }

  @Override
  public List<EthnicityInfo> findEthnicityInfo(SearchRequest request) {
    TableResult result =
        bigQueryService.filterBigQueryConfigAndExecuteQuery(
            chartQueryBuilder.buildEthnicityInfoCounterQuery(new ParticipantCriteria(request)));

    return cohortBuilderMapper.tableResultToEthnicityInfo(result);
  }

  @Override
  public List<ParticipantChartData> findParticipantChartData(
      Long participantId, Domain domain, int limit) {
    TableResult result =
        bigQueryService.filterBigQueryConfigAndExecuteQuery(
            chartQueryBuilder.buildChartDataQuery(participantId, domain, limit));

    return cohortReviewMapper.tableResultToParticipantChartData(result);
  }
}
