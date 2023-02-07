package org.pmiops.workbench.cohortbuilder.chart;

import java.util.List;
import java.util.Set;
import org.pmiops.workbench.model.AgeType;
import org.pmiops.workbench.model.CohortChartData;
import org.pmiops.workbench.model.CohortDefinition;
import org.pmiops.workbench.model.DemoChartInfo;
import org.pmiops.workbench.model.Domain;
import org.pmiops.workbench.model.EthnicityInfo;
import org.pmiops.workbench.model.GenderSexRaceOrEthType;
import org.pmiops.workbench.model.ParticipantChartData;

public interface ChartService {

  List<CohortChartData> findCohortChartData(
      CohortDefinition cohortDefinition, Domain domain, int limit);

  List<CohortChartData> findCohortReviewChartData(
      Set<Long> participantIds, Domain domain, int limit);

  List<DemoChartInfo> findDemoChartInfo(
      GenderSexRaceOrEthType genderSexRaceOrEthType,
      AgeType ageType,
      CohortDefinition cohortDefinition);

  List<DemoChartInfo> findCohortReviewDemoChartInfo(Set<Long> participantIds);

  List<EthnicityInfo> findEthnicityInfo(CohortDefinition cohortDefinition);

  List<ParticipantChartData> findParticipantChartData(Long participantId, Domain domain, int limit);
}
