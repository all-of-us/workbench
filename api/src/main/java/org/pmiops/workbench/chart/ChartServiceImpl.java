package org.pmiops.workbench.chart;

import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.cohortbuilder.ParticipantCriteria;
import org.pmiops.workbench.db.model.DbCohort;
import org.pmiops.workbench.exceptions.NotFoundException;
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

  public ChartServiceImpl(BigQueryService bigQueryService, ChartQueryBuilder chartQueryBuilder) {
    this.bigQueryService = bigQueryService;
    this.chartQueryBuilder = chartQueryBuilder;
  }

  @Override
  public List<CohortChartData> findCohortChartData(
      SearchRequest searchRequest, Domain domain, int limit) {
    TableResult result =
        bigQueryService.executeQuery(
            bigQueryService.filterBigQueryConfig(
                chartQueryBuilder.buildDomainChartInfoCounterQuery(
                    new ParticipantCriteria(searchRequest), domain, limit)));
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

  @Override
  public List<DemoChartInfo> findDemoChartInfo(
      GenderOrSexType genderOrSexType, AgeType ageType, SearchRequest request) {
    QueryJobConfiguration qjc =
        bigQueryService.filterBigQueryConfig(
            chartQueryBuilder.buildDemoChartInfoCounterQuery(
                new ParticipantCriteria(request, genderOrSexType, ageType)));
    TableResult result = bigQueryService.executeQuery(qjc);
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
  public List<CohortChartData> findCohortChartData(DbCohort dbCohort, Domain domain, int limit) {
    SearchRequest searchRequest =
        new Gson().fromJson(getCohortDefinition(dbCohort), SearchRequest.class);

    TableResult result =
        bigQueryService.executeQuery(
            bigQueryService.filterBigQueryConfig(
                chartQueryBuilder.buildDomainChartInfoCounterQuery(
                    new ParticipantCriteria(searchRequest), domain, limit)));
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

  private String getCohortDefinition(DbCohort dbCohort) {
    String definition = dbCohort.getCriteria();
    if (definition == null) {
      throw new NotFoundException(
          String.format(
              "Not Found: No Cohort definition matching cohortId: %s", dbCohort.getCohortId()));
    }
    return definition;
  }
}
