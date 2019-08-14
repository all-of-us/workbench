package org.pmiops.workbench.billing;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import java.util.logging.Logger;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.db.dao.UserDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BillingAlertsService {

  private static final Logger log = Logger.getLogger(BillingController.class.getName());

  private final BigQueryService bigQueryService;
  private final NotificationService notificationService;

  @Autowired
  public BillingAlertsService(BigQueryService bigQueryService,
      NotificationService notificationService) {
    this.bigQueryService = bigQueryService;
    this.notificationService = notificationService;
  }

  public void alertUsersExceedingFreeTierBilling() {
    log.info("testing cron job");
    QueryJobConfiguration queryConfig = QueryJobConfiguration
        .newBuilder("SELECT project.id, SUM(cost) cost FROM `all-of-us-workbench-test-bd.billing_data.gcp_billing_export_v1_014D91_FCB792_33D2C0` GROUP BY project.id ORDER BY cost desc;")
        .build();

    TableResult result = bigQueryService.executeQuery(queryConfig);
  }

}
