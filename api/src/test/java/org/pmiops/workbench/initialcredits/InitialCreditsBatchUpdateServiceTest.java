package org.pmiops.workbench.initialcredits;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValue.Attribute;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.LegacySQLTypeName;
import com.google.cloud.bigquery.Schema;
import com.google.common.base.Stopwatch;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.pmiops.workbench.FakeClockConfiguration;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.db.dao.UserDao;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.utils.BigQueryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

@DataJpaTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class InitialCreditsBatchUpdateServiceTest {

  @Autowired private InitialCreditsBatchUpdateService initialCreditsBatchUpdateService;

  @MockBean private WorkspaceDao mockWorkspaceDao;
  @MockBean private InitialCreditsService mockInitialCreditsService;
  @MockBean private UserDao mockUserDao;
  @MockBean private BigQueryService mockBigQueryService;

  private static WorkbenchConfig config;

  @TestConfiguration
  @Import({
    FakeClockConfiguration.class,
    InitialCreditsBatchUpdateService.class,
    Stopwatch.class,
  })
  static class Configuration {
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public WorkbenchConfig workbenchConfig() {
      return config;
    }
  }

  Set<DbUser> mockDbUserSet = new HashSet<>();

  Set<String> googleProjectIdsSet = new HashSet<>(List.of("12", "22", "23", "32", "33"));

  @BeforeEach
  public void init() {
    config = WorkbenchConfig.createEmptyConfig();

    mockDbUsers();
    mockGoogleProjectsForUser();
    mockGoogleProjectCost();
  }

  @Test
  public void testFreeTierBillingBatchUpdateService() {
    initialCreditsBatchUpdateService.checkInitialCreditsUsage(List.of(1L, 2L, 3L));

    verify(mockWorkspaceDao, times(1)).getWorkspaceGoogleProjectsForCreators(List.of(1L, 2L, 3L));

    verify(mockInitialCreditsService)
        .checkInitialCreditsUsageForUsers(mockDbUserSet, getUserCostMap());
  }

  private void mockDbUsers() {
    DbUser dbUserForId1 = new DbUser().setUserId(1L);
    DbUser dbUserForId2 = new DbUser().setUserId(2L);
    DbUser dbUserForId3 = new DbUser().setUserId(3L);
    when(mockUserDao.findUserByUserId(1L)).thenReturn(dbUserForId1);
    when(mockUserDao.findUserByUserId(2L)).thenReturn(dbUserForId2);
    when(mockUserDao.findUserByUserId(3L)).thenReturn(dbUserForId3);
    mockDbUserSet.add(dbUserForId1);
    mockDbUserSet.add(dbUserForId2);
    mockDbUserSet.add(dbUserForId3);
  }

  private void mockGoogleProjectsForUser() {
    when(mockWorkspaceDao.getWorkspaceGoogleProjectsForCreators(List.of(1L, 2L, 3L)))
        .thenReturn(googleProjectIdsSet);
  }

  private void mockGoogleProjectCost() {
    Schema s =
        Schema.of(
            Field.of("id", LegacySQLTypeName.STRING), Field.of("cost", LegacySQLTypeName.NUMERIC));

    List<FieldValueList> tableRows =
        List.of(
            bqRow("12", "0.013"),
            bqRow("22", "1.123"),
            bqRow("23", "6.5"),
            bqRow("32", "0.34"),
            bqRow("33", "0.9"));

    when(mockBigQueryService.executeQuery(any()))
        .thenReturn(BigQueryUtils.newTableResult(s, tableRows));
  }

  private FieldValueList bqRow(String id, String cost) {
    return FieldValueList.of(
        List.of(FieldValue.of(Attribute.PRIMITIVE, id), FieldValue.of(Attribute.PRIMITIVE, cost)));
  }

  private Map<String, Double> getUserCostMap() {
    Map<String, Double> userCostMap = new HashMap<String, Double>();
    userCostMap.put("12", 0.013);
    userCostMap.put("22", 1.123);
    userCostMap.put("23", 6.5);
    userCostMap.put("32", 0.34);
    userCostMap.put("33", 0.9);
    return userCostMap;
  }
}
