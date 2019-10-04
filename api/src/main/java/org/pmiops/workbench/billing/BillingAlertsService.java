package org.pmiops.workbench.billing;

import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import com.google.common.collect.Streams;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sun.nio.cs.CharsetMapping;

@Service
public class BillingAlertsService {

  private static final Logger logger = LoggerFactory.getLogger(BillingAlertsService.class);
  public static final String MAX_FREE_CREDITS_EXCEEDED = "You have exceeded your free tier credits.";

  private final BigQueryService bigQueryService;
  private final NotificationService notificationService;
  private final WorkspaceDao workspaceDao;
  private final Provider<WorkbenchConfig> workbenchConfigProvider;

  @Autowired
  public BillingAlertsService(
      BigQueryService bigQueryService,
      NotificationService notificationService,
      WorkspaceDao workspaceDao,
      Provider<WorkbenchConfig> workbenchConfigProvider) {
    this.bigQueryService = bigQueryService;
    this.notificationService = notificationService;
    this.workspaceDao = workspaceDao;
    this.workbenchConfigProvider = workbenchConfigProvider;
  }

  public void checkFreeTierBillingUsage() {
    QueryJobConfiguration queryConfig =
        QueryJobConfiguration.newBuilder(
                "SELECT project.id, SUM(cost) cost FROM `"
                    + workbenchConfigProvider.get().billing.exportBigQueryTable
                    + "` GROUP BY project.id ORDER BY cost desc;")
            .build();

    Map<String, User> namespaceToCreator = workspaceDao.namespaceToCreator();
    final int numWorkspaces = namespaceToCreator.keySet().size();
    final long numUniqueUsers = namespaceToCreator.values().stream()
        .distinct()
        .count();
    logger.info(String.format("found %d workspaces with %d unique users", numWorkspaces, numUniqueUsers));

    final TableResult queryResults = bigQueryService.executeQuery(queryConfig);
    logger.info("Query retrieved %d results", queryResults.getTotalRows());

    if (queryResults.getTotalRows() < numWorkspaces) {
      logger.warn("Expected %d rows but found %d", numWorkspaces, queryResults.getTotalRows());
    }

    Streams.stream(queryResults.getValues())
        .filter(fv -> fv.get("id").isNull())
        .forEach(fv -> logger.error(String.format("Query result has no project ID: %s", fv.toString())));

    Map<User, Double> perUserSpend =
        Streams.stream(queryResults.getValues())
            .filter(fv -> !fv.get("id").isNull() && !fv.get("cost").isNull())
            .map(flv -> buildMapEntry(namespaceToCreator, flv))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(
                Collectors.groupingBy(
                    SimpleImmutableEntry::getKey, Collectors.summingDouble(Entry::getValue)));

    perUserSpend.entrySet().stream()
        .filter(entry -> entry.getValue() > getUserFreeTierLimit(entry.getKey()))
        .map(Entry::getKey)
        .forEach(this::alertUser);
  }

  private Optional<AbstractMap.SimpleImmutableEntry<User, Double>> buildMapEntry(Map<String, User> namespaceToCreator, FieldValueList fieldValueList) {
    Optional<User> creatorMaybe = getCreatorUser(namespaceToCreator, fieldValueList.get("id").getStringValue());
    return creatorMaybe.map(creator -> new AbstractMap.SimpleImmutableEntry<User, Double>(creator, fieldValueList.get("cost").getDoubleValue()));
  }

  private Optional<User> getCreatorUser(Map<String, User> namespaceToCreator, String id) {
    final Optional<User> creatorUser = Optional.ofNullable(namespaceToCreator.get(id));
    if (!creatorUser.isPresent()) {
      logger.error(String.format("Could not locate creator for workspace id %s", id));
    }
    return creatorUser;
  }

  private void alertUser(User user) {
    logger.info(String.format("Alerting user ID %d contact email %s", user.getUserId(), user.getContactEmail());
    notificationService.alertUser(user, MAX_FREE_CREDITS_EXCEEDED);
  }

  private Double getUserFreeTierLimit(User user) {
    if (user.getFreeTierCreditsLimitOverride() != null) {
      return user.getFreeTierCreditsLimitOverride();
    }

    return workbenchConfigProvider.get().billing.defaultFreeCreditsLimit;
  }
}
