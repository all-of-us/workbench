package org.pmiops.workbench.billing;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.common.collect.Streams;
import java.util.Map;
import java.util.stream.Collectors;
import javafx.util.Pair;
import javax.inject.Provider;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BillingAlertsService {

  // TODO eric : Add WorkbenchConfig values
  // TODO eric : Add liquibase change

  private final BigQueryService bigQueryService;
  private final NotificationService notificationService;
  private final UserDao userDao;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  @Autowired
  public BillingAlertsService(
      BigQueryService bigQueryService,
      NotificationService notificationService,
      UserDao userDao,
      Provider<WorkbenchConfig> workbenchConfigProvider
  ) {
    this.bigQueryService = bigQueryService;
    this.notificationService = notificationService;
    this.userDao = userDao;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  public void alertUsersExceedingFreeTierBilling() {
    QueryJobConfiguration queryConfig = QueryJobConfiguration
        .newBuilder("SELECT project.id, SUM(cost) cost FROM `all-of-us-workbench-test-bd.billing_data.gcp_billing_export_v1_014D91_FCB792_33D2C0` "
            + "GROUP BY project.id ORDER BY cost desc;")
        .build();

    Map<User, Double> userCosts = Streams
        .stream(bigQueryService.executeQuery(queryConfig).getValues())
        .map(fv -> new Pair<>(
            userDao.findCreatorByWorkspaceNamespace(fv.get("id").getStringValue()),
            fv.get("cost").getDoubleValue()))
        .filter(pair -> pair.getKey() != null)
        .filter(pair -> !workbenchConfigProvider.get().freeCredits.whitelistedUsers
            .contains(pair.getKey().getEmail()) )
        .collect(Collectors.groupingBy(
            pair -> pair.getKey(),
            Collectors.summingDouble(Pair::getValue)
        ));

    userCosts.entrySet().stream()
        .filter(entry -> entry.getValue() > getUserFreeTierLimit(entry.getKey()))
        .forEach(entry -> notificationService.alertUser(entry.getKey(), "You have exceeded your free tier credits."));
  }

  private Double getUserFreeTierLimit(User user) {
    if (user.getFreeTierCreditsLimitOverride() != null) {
      return user.getFreeTierCreditsLimitOverride();
    }

    return workbenchConfigProvider.get().freeCredits.defaultLimit;
  }
}
