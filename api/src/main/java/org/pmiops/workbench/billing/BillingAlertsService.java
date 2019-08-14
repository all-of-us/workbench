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
  private final UserDao userDao;

  @Autowired
  public BillingAlertsService(BigQueryService bigQueryService,
      UserDao userDao) {
    this.bigQueryService = bigQueryService;
    this.userDao = userDao;
  }

  public void alertUsersExceedingFreeTierBilling() {
    log.info("testing cron job");
    QueryJobConfiguration queryConfig = QueryJobConfiguration
        .newBuilder("SELECT project.id, SUM(cost) cost, MIN(DATE(usage_start_time)) start_time, MAX(DATE(usage_end_time)) end_time FROM `all-of-us-workbench-test-bd.billing_data.gcp_billing_export_v1_014D91_FCB792_33D2C0` GROUP BY project.id ORDER BY cost desc;")
        .build();

    TableResult result = bigQueryService.executeQuery(queryConfig);
  }

}
