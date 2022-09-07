package org.pmiops.workbench.cohortbuilder.chart;

import java.util.List;
import java.util.Set;
import org.pmiops.workbench.model.CohortChartData;
import org.pmiops.workbench.model.DemoChartInfo;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.EthnicityInfo;
import org.pmiops.workbench.model.ParticipantChartData;
import org.pmiops.workbench.model.SearchRequest;

public interface ChartService {

  List<CohortChartData> findCohortChartData(SearchRequest searchRequest, Domain domain, int limit);

  List<CohortChartData> findCohortReviewChartData(
      Set<Long> participantIds, Domain domain, int limit);

  List<DemoChartInfo> findDemoChartInfo(
      String genderOrSexType, String ageType, SearchRequest request);

  List<DemoChartInfo> findCohortReviewDemoChartInfo(Set<Long> participantIds);

  List<EthnicityInfo> findEthnicityInfo(SearchRequest request);

  List<ParticipantChartData> findParticipantChartData(Long participantId, Domain domain, int limit);
}
