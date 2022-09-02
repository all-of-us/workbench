package org.pmiops.workbench.cohortbuilder.chart;

import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.cohortbuilder.ParticipantCriteria;
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDao;
import org.pmiops.workbench.model.AgeType;
import org.pmiops.workbench.model.CohortChartData;
import org.pmiops.workbench.model.CohortChartDataListResponse;
import org.pmiops.workbench.model.DemoChartInfo;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.EthnicityInfo;
import org.pmiops.workbench.model.GenderOrSexType;
import org.pmiops.workbench.model.ParticipantChartData;
import org.pmiops.workbench.model.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ChartServiceImpl implements ChartService {
  private final BigQueryService bigQueryService;
  private final ChartQueryBuilder chartQueryBuilder;
  private final ParticipantCohortStatusDao participantCohortStatusDao;

  @Autowired
  public ChartServiceImpl(
      BigQueryService bigQueryService,
      ChartQueryBuilder chartQueryBuilder,
      ParticipantCohortStatusDao participantCohortStatusDao) {
    this.bigQueryService = bigQueryService;
    this.chartQueryBuilder = chartQueryBuilder;
    this.participantCohortStatusDao = participantCohortStatusDao;
  }

  @Override
  public List<CohortChartData> findCohortChartData(
      SearchRequest searchRequest, Domain domain, int limit) {

    final QueryJobConfiguration queryJobConfiguration =
        chartQueryBuilder.buildDomainChartInfoCounterQuery(
            new ParticipantCriteria(searchRequest), domain, limit);

    return bigQueryForCohortChartData(queryJobConfiguration);
  }

  @Override
  public List<DemoChartInfo> findDemoChartInfo(
      GenderOrSexType genderOrSexType, AgeType ageType, SearchRequest request) {

    final QueryJobConfiguration queryJobConfiguration =
        chartQueryBuilder.buildDemoChartInfoCounterQuery(
            new ParticipantCriteria(request, genderOrSexType, ageType));

    return bigQueryForDemoChartInfo(queryJobConfiguration);
  }

  @Override
  public List<EthnicityInfo> findEthnicityInfo(SearchRequest request) {

    final QueryJobConfiguration queryJobConfiguration =
        chartQueryBuilder.buildEthnicityInfoCounterQuery(new ParticipantCriteria(request));

    return bigQueryForEthnicityInfo(queryJobConfiguration);
  }

  private Set<Long> findParticipantIdsByCohortReview(Long cohortReviewId) {

    return participantCohortStatusDao.findByParticipantKey_CohortReviewId(cohortReviewId).stream()
        .map(pcs -> pcs.getParticipantKey().getParticipantId())
        .collect(Collectors.toSet());
  }

  @Override
  public CohortChartDataListResponse findCohortReviewChartData(
      SearchRequest searchRequest, Long cohortReviewId, Domain domain, int limit) {

    Set<Long> participantIds = findParticipantIdsByCohortReview(cohortReviewId);

    final QueryJobConfiguration queryJobConfiguration =
        chartQueryBuilder.buildDomainChartInfoCounterQuery(
            new ParticipantCriteria(searchRequest).participantIdsToInclude(participantIds),
            domain,
            limit);

    return new CohortChartDataListResponse()
        .count((long) participantIds.size())
        .items(bigQueryForCohortChartData(queryJobConfiguration));
  }

  @Override
  public List<DemoChartInfo> findCohortReviewDemoChartInfo(
      GenderOrSexType genderOrSexType,
      AgeType ageType,
      SearchRequest request,
      Long cohortReviewId) {

    Set<Long> participantIds = findParticipantIdsByCohortReview(cohortReviewId);

    final QueryJobConfiguration queryJobConfiguration =
        chartQueryBuilder.buildDemoChartInfoCounterQuery(
            new ParticipantCriteria(request, genderOrSexType, ageType)
                .participantIdsToInclude(participantIds));

    return bigQueryForDemoChartInfo(queryJobConfiguration);
  }

  @Override
  public List<EthnicityInfo> findCohortReviewEthnicityInfo(
      SearchRequest request, Long cohortReviewId) {
    Set<Long> participantIds = findParticipantIdsByCohortReview(cohortReviewId);
    final QueryJobConfiguration queryJobConfiguration =
        chartQueryBuilder.buildEthnicityInfoCounterQuery(
            new ParticipantCriteria(request).participantIdsToInclude(participantIds));

    return bigQueryForEthnicityInfo(queryJobConfiguration);
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
  private List<CohortChartData> bigQueryForCohortChartData(
      QueryJobConfiguration queryJobConfiguration) {
    TableResult result =
        bigQueryService.executeQuery(bigQueryService.filterBigQueryConfig(queryJobConfiguration));
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
  private List<DemoChartInfo> bigQueryForDemoChartInfo(
      QueryJobConfiguration queryJobConfiguration) {
    TableResult result =
        bigQueryService.executeQuery(bigQueryService.filterBigQueryConfig(queryJobConfiguration));
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

  @NotNull
  private List<EthnicityInfo> bigQueryForEthnicityInfo(QueryJobConfiguration qjc) {
    TableResult result = bigQueryService.executeQuery(bigQueryService.filterBigQueryConfig(qjc));
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
}
