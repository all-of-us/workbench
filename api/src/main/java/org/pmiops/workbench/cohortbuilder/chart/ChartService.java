package org.pmiops.workbench.cohortbuilder.chart;

import java.util.List;
import java.util.Set;

import org.pmiops.workbench.model.*;

public interface ChartService {

  List<CohortChartData> findCohortChartData(CohortDefinition cohortDefinition, Domain domain, int limit);

  List<CohortChartData> findCohortReviewChartData(
      Set<Long> participantIds, Domain domain, int limit);

  List<DemoChartInfo> findDemoChartInfo(
          GenderOrSexType genderOrSexType, AgeType ageType, CohortDefinition cohortDefinition);

  List<DemoChartInfo> findCohortReviewDemoChartInfo(Set<Long> participantIds);

  List<EthnicityInfo> findEthnicityInfo(CohortDefinition cohortDefinition);

  List<ParticipantChartData> findParticipantChartData(Long participantId, Domain domain, int limit);
}
