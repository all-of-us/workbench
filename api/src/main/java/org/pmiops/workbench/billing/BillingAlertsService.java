package org.pmiops.workbench.billing;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.common.collect.Streams;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BillingAlertsService {

  private final BigQueryService bigQueryService;
  private final NotificationService notificationService;
  private final UserDao userDao;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  @Autowired
  public BillingAlertsService(
      BigQueryService bigQueryService,
      NotificationService notificationService,
      UserDao userDao,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.bigQueryService = bigQueryService;
    this.notificationService = notificationService;
    this.userDao = userDao;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  public void alertUsersExceedingFreeTierBilling() {
    QueryJobConfiguration queryConfig =
        QueryJobConfiguration.newBuilder(
                "SELECT project.id, SUM(cost) cost FROM `"
                    + workbenchConfigProvider.get().billing.billingExportBigQueryTable
                    + "` GROUP BY project.id ORDER BY cost desc;")
            .build();

    Map<User, Double> perUserSpend =
        Streams.stream(bigQueryService.executeQuery(queryConfig).getValues())
            .filter(fv -> !fv.get("id").isNull() && !fv.get("id").isNull())
            .map(
                fv ->
                    new AbstractMap.SimpleImmutableEntry<>(
                        userDao.findCreatorByWorkspaceNamespace(fv.get("id").getStringValue()),
                        fv.get("cost").getDoubleValue()))
            .filter(spend -> spend.getKey() != null)
            .filter(
                spend ->
                    !workbenchConfigProvider
                        .get()
                        .billing
                        .whitelistedUsers
                        .contains(spend.getKey().getEmail()))
            .collect(
                Collectors.groupingBy(
                    spend -> spend.getKey(), Collectors.summingDouble(Entry::getValue)));

    perUserSpend.entrySet().stream()
        .filter(entry -> entry.getValue() > getUserFreeTierLimit(entry.getKey()))
        .forEach(
            entry ->
                notificationService.alertUser(
                    entry.getKey(), "You have exceeded your free tier credits."));
  }

  private Double getUserFreeTierLimit(User user) {
    if (user.getFreeTierCreditsLimitOverride() != null) {
      return user.getFreeTierCreditsLimitOverride();
    }

    return workbenchConfigProvider.get().billing.defaultFreeCreditsLimit;
  }
}
