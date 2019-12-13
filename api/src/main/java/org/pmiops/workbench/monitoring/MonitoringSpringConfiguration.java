package org.pmiops.workbench.monitoring;

import com.google.common.collect.ImmutableList;
import io.opencensus.stats.Stats;
import io.opencensus.stats.StatsRecorder;
import io.opencensus.stats.ViewManager;
import java.util.List;
import org.pmiops.workbench.billing.BillingProjectBufferService;
import org.pmiops.workbench.cohortreview.CohortReviewServiceImpl;
import org.pmiops.workbench.db.dao.DataSetServiceImpl;
import org.pmiops.workbench.workspaces.WorkspaceServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MonitoringSpringConfiguration {

  @Bean
  public ViewManager getViewManager() {
    return Stats.getViewManager();
  }

  @Bean
  public StatsRecorder getStatsRecorder() {
    return Stats.getStatsRecorder();
  }

//  /**
//   * Collect all the beans that implement GaugeDataCollector. For use
//   * with Monitoring Gauge signals
//   */
//  @Bean
//  public List<GaugeDataCollector> getGaugeDataCollectors(
//      BillingProjectBufferService billingProjectBufferService,
//      WorkspaceServiceImpl workspaceServiceImpl,
//      DataSetServiceImpl dataSetServiceImpl,
//      CohortReviewServiceImpl cohortReviewServiceImpl) {
//    return ImmutableList.of(
//        billingProjectBufferService,
//        workspaceServiceImpl,
//        dataSetServiceImpl,
//        cohortReviewServiceImpl);
//  }
}
