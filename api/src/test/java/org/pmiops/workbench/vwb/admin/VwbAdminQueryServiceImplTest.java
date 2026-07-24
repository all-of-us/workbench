package org.pmiops.workbench.vwb.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import jakarta.inject.Provider;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.model.VwbWorkspace;
import org.pmiops.workbench.model.WorkspaceAccessLevel;

public class VwbAdminQueryServiceImplTest {

  private static final String TEST_GSUITE_DOMAIN = "example.com";

  @Mock private Provider<WorkbenchConfig> mockConfigProvider;
  @Mock private BigQueryService mockBigQueryService;
  @Mock private TableResult mockTableResult;

  private VwbAdminQueryServiceImpl service;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);

    // Set up config
    WorkbenchConfig config = new WorkbenchConfig();
    config.googleDirectoryService = new WorkbenchConfig.GoogleDirectoryServiceConfig();
    config.googleDirectoryService.gSuiteDomain = TEST_GSUITE_DOMAIN;
    config.vwb = new WorkbenchConfig.VwbConfig();
    config.vwb.organizationUfid = "test-org";
    config.vwb.adminBigQuery = new WorkbenchConfig.VwbConfig.AdminBigQuery();
    config.vwb.adminBigQuery.logProjectId = "test-project";
    config.vwb.adminBigQuery.bigQueryDataset = "test-dataset";

    when(mockConfigProvider.get()).thenReturn(config);

    // Set up mock table result to return empty iterable
    when(mockTableResult.iterateAll()).thenReturn(Collections.emptyList());
    when(mockBigQueryService.executeQuery(any(QueryJobConfiguration.class)))
        .thenReturn(mockTableResult);

    service = new VwbAdminQueryServiceImpl(mockConfigProvider, mockBigQueryService);
  }

  @Test
  public void testNormalizeEmail_usernameOnly_appendsDomain() {
    // When querying by creator with just username
    service.queryVwbWorkspacesByCreator("testuser");

    // Verify that the email was normalized with domain
    ArgumentCaptor<QueryJobConfiguration> configCaptor =
        ArgumentCaptor.forClass(QueryJobConfiguration.class);
    verify(mockBigQueryService).executeQuery(configCaptor.capture());

    QueryJobConfiguration capturedConfig = configCaptor.getValue();
    String normalizedEmail = capturedConfig.getNamedParameters().get("SEARCH_PARAM").getValue();
    assertEquals("testuser@example.com", normalizedEmail);
  }

  @Test
  public void testNormalizeEmail_fullEmailWithConfiguredDomain_unchanged() {
    // When querying with full email that matches GSuite domain
    service.queryVwbWorkspacesByCreator("user@example.com");

    // Verify that the email was not modified
    ArgumentCaptor<QueryJobConfiguration> configCaptor =
        ArgumentCaptor.forClass(QueryJobConfiguration.class);
    verify(mockBigQueryService).executeQuery(configCaptor.capture());

    QueryJobConfiguration capturedConfig = configCaptor.getValue();
    String normalizedEmail = capturedConfig.getNamedParameters().get("SEARCH_PARAM").getValue();
    assertEquals("user@example.com", normalizedEmail);
  }

  @Test
  public void testNormalizeEmail_fullEmailWithDifferentDomain_allowedAndUnchanged() {
    // When querying with full email from different domain (e.g., user@otherdomain.com when
    // config is fake.researchallofus.org) - this is allowed and passed through unchanged
    service.queryVwbWorkspacesByCreator("user@otherdomain.com");

    // Verify that the email was not modified - different domains are allowed
    ArgumentCaptor<QueryJobConfiguration> configCaptor =
        ArgumentCaptor.forClass(QueryJobConfiguration.class);
    verify(mockBigQueryService).executeQuery(configCaptor.capture());

    QueryJobConfiguration capturedConfig = configCaptor.getValue();
    String normalizedEmail = capturedConfig.getNamedParameters().get("SEARCH_PARAM").getValue();
    assertEquals("user@otherdomain.com", normalizedEmail);
  }

  @Test
  public void testNormalizeEmail_usernameWithWhitespace_trimsAndAppendsDomain() {
    // When querying with username that has whitespace
    service.queryVwbWorkspacesByCreator("  testuser  ");

    // Verify that whitespace was trimmed and domain was appended
    ArgumentCaptor<QueryJobConfiguration> configCaptor =
        ArgumentCaptor.forClass(QueryJobConfiguration.class);
    verify(mockBigQueryService).executeQuery(configCaptor.capture());

    QueryJobConfiguration capturedConfig = configCaptor.getValue();
    String normalizedEmail = capturedConfig.getNamedParameters().get("SEARCH_PARAM").getValue();
    assertEquals("testuser@example.com", normalizedEmail);
  }

  @Test
  public void testNormalizeEmail_emptyString_unchanged() {
    // When querying with empty string
    service.queryVwbWorkspacesByCreator("");

    // Verify that empty string was passed as is
    ArgumentCaptor<QueryJobConfiguration> configCaptor =
        ArgumentCaptor.forClass(QueryJobConfiguration.class);
    verify(mockBigQueryService).executeQuery(configCaptor.capture());

    QueryJobConfiguration capturedConfig = configCaptor.getValue();
    String normalizedEmail = capturedConfig.getNamedParameters().get("SEARCH_PARAM").getValue();
    assertEquals("", normalizedEmail);
  }

  @Test
  public void testNormalizeEmail_shareActivity_usernameOnly_appendsDomain() {
    // When querying share activity by username
    service.queryVwbWorkspacesByShareActivity("testuser");

    // Verify that the email was normalized with domain
    ArgumentCaptor<QueryJobConfiguration> configCaptor =
        ArgumentCaptor.forClass(QueryJobConfiguration.class);
    verify(mockBigQueryService).executeQuery(configCaptor.capture());

    QueryJobConfiguration capturedConfig = configCaptor.getValue();
    String normalizedEmail = capturedConfig.getNamedParameters().get("SEARCH_PARAM").getValue();
    assertEquals("testuser@example.com", normalizedEmail);
  }

  @Test
  public void testNormalizeEmail_shareActivity_fullEmail_unchanged() {
    // When querying share activity with full email
    service.queryVwbWorkspacesByShareActivity("testuser@example.com");

    // Verify that the email was not modified
    ArgumentCaptor<QueryJobConfiguration> configCaptor =
        ArgumentCaptor.forClass(QueryJobConfiguration.class);
    verify(mockBigQueryService).executeQuery(configCaptor.capture());

    QueryJobConfiguration capturedConfig = configCaptor.getValue();
    String normalizedEmail = capturedConfig.getNamedParameters().get("SEARCH_PARAM").getValue();
    assertEquals("testuser@example.com", normalizedEmail);
  }

  @Test
  public void testShareActivityQuery_usesLatestAccessEventFilter() {
    service.queryVwbWorkspacesByShareActivity("testuser@example.com");

    ArgumentCaptor<QueryJobConfiguration> configCaptor =
        ArgumentCaptor.forClass(QueryJobConfiguration.class);
    verify(mockBigQueryService).executeQuery(configCaptor.capture());

    QueryJobConfiguration capturedConfig = configCaptor.getValue();
    String query = capturedConfig.getQuery();

    // Current access is determined from latest grant/remove event for this user.
    org.junit.jupiter.api.Assertions.assertTrue(query.contains("ROW_NUMBER() OVER"));
    org.junit.jupiter.api.Assertions.assertTrue(query.contains("@GRANT_EVENT"));
    org.junit.jupiter.api.Assertions.assertTrue(query.contains("@REMOVE_EVENT"));
    org.junit.jupiter.api.Assertions.assertTrue(
        query.contains("LOWER(wal.change_subject_id)=LOWER(@SEARCH_PARAM)"));
    assertEquals(
        "GRANT_WORKSPACE_ROLE", capturedConfig.getNamedParameters().get("GRANT_EVENT").getValue());
    assertEquals(
        "REMOVE_WORKSPACE_ROLE",
        capturedConfig.getNamedParameters().get("REMOVE_EVENT").getValue());
  }

  @Test
  public void testDetermineEffectiveRole_creatorGetsOwnerWhenRoleMissing() {
    VwbWorkspace creatorWorkspace = new VwbWorkspace();
    creatorWorkspace.setUserFacingId("ws-ufid-1");
    creatorWorkspace.setCreatedBy("creator@example.com");

    WorkspaceAccessLevel effectiveRole =
        service.determineEffectiveRole(
            creatorWorkspace, "creator@example.com", Collections.emptyMap());

    assertEquals(WorkspaceAccessLevel.OWNER, effectiveRole);
  }

  @Test
  public void testDetermineEffectiveRole_creatorOverridesNonOwnerRole() {
    VwbWorkspace creatorWorkspace = new VwbWorkspace();
    creatorWorkspace.setUserFacingId("ws-ufid-2");
    creatorWorkspace.setCreatedBy("creator@example.com");

    Map<String, WorkspaceAccessLevel> roles = new HashMap<>();
    roles.put("ws-ufid-2", WorkspaceAccessLevel.WRITER);

    WorkspaceAccessLevel effectiveRole =
        service.determineEffectiveRole(creatorWorkspace, "creator@example.com", roles);

    assertEquals(WorkspaceAccessLevel.OWNER, effectiveRole);
  }

  @Test
  public void testMergeCreatorAndSharedWorkspaces_deduplicatesByWorkspaceId() {
    VwbWorkspace sharedWorkspace = new VwbWorkspace();
    sharedWorkspace.setId("ws-id-3");
    sharedWorkspace.setUserFacingId("ws-ufid-3");

    VwbWorkspace sameWorkspaceFromCreator = new VwbWorkspace();
    sameWorkspaceFromCreator.setId("ws-id-3");
    sameWorkspaceFromCreator.setUserFacingId("ws-ufid-3");
    sameWorkspaceFromCreator.setCreatedBy("creator@example.com");

    Map<String, VwbWorkspace> merged =
        service.mergeCreatorAndSharedWorkspaces(
            List.of(sameWorkspaceFromCreator), List.of(sharedWorkspace));

    assertEquals(1, merged.size());
  }

  @Test
  public void testNormalizeEmail_usernameWithDot_appendsDomain() {
    // When querying with username containing dot
    service.queryVwbWorkspacesByCreator("first.last");

    // Verify that domain was appended
    ArgumentCaptor<QueryJobConfiguration> configCaptor =
        ArgumentCaptor.forClass(QueryJobConfiguration.class);
    verify(mockBigQueryService).executeQuery(configCaptor.capture());

    QueryJobConfiguration capturedConfig = configCaptor.getValue();
    String normalizedEmail = capturedConfig.getNamedParameters().get("SEARCH_PARAM").getValue();
    assertEquals("first.last@example.com", normalizedEmail);
  }
}
