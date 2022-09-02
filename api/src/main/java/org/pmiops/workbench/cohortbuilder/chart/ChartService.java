package org.pmiops.workbench.cohortbuilder.chart;

import java.util.List;
import org.pmiops.workbench.model.AgeType;
import org.pmiops.workbench.model.CohortChartData;
import org.pmiops.workbench.model.CohortChartDataListResponse;
import org.pmiops.workbench.model.DemoChartInfo;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.EthnicityInfo;
import org.pmiops.workbench.model.GenderOrSexType;
import org.pmiops.workbench.model.ParticipantChartData;
import org.pmiops.workbench.model.SearchRequest;

public interface ChartService {

  List<CohortChartData> findCohortChartData(SearchRequest searchRequest, Domain domain, int limit);

  List<DemoChartInfo> findDemoChartInfo(
      GenderOrSexType genderOrSexType, AgeType ageType, SearchRequest request);

  List<EthnicityInfo> findEthnicityInfo(SearchRequest request);

  CohortChartDataListResponse findCohortReviewChartData(
      SearchRequest searchRequest, Long cohortReviewId, Domain domain, int limit);

  List<DemoChartInfo> findCohortReviewDemoChartInfo(
      GenderOrSexType genderOrSexType, AgeType ageType, SearchRequest request, Long cohortReviewId);

  List<EthnicityInfo> findCohortReviewEthnicityInfo(SearchRequest request, Long cohortReviewId);

  List<ParticipantChartData> findParticipantChartData(Long participantId, Domain domain, int limit);
}
