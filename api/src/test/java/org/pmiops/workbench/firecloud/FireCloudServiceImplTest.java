package org.pmiops.workbench.firecloud;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.exceptions.ConflictException;
import org.pmiops.workbench.exceptions.ForbiddenException;
import org.pmiops.workbench.exceptions.GatewayTimeoutException;
import org.pmiops.workbench.exceptions.NotFoundException;
import org.pmiops.workbench.exceptions.ServerErrorException;
import org.pmiops.workbench.exceptions.ServerUnavailableException;
import org.pmiops.workbench.exceptions.UnauthorizedException;
import org.pmiops.workbench.firecloud.api.BillingApi;
import org.pmiops.workbench.firecloud.api.GroupsApi;
import org.pmiops.workbench.firecloud.api.ProfileApi;
import org.pmiops.workbench.firecloud.api.WorkspacesApi;
import org.pmiops.workbench.firecloud.model.Enabled;
import org.pmiops.workbench.firecloud.model.ManagedGroupAccessResponse;
import org.pmiops.workbench.firecloud.model.Me;
import org.pmiops.workbench.test.Providers;
import org.springframework.retry.backoff.NoBackOffPolicy;

public class FireCloudServiceImplTest {


  private FireCloudServiceImpl service;

  @Mock
  private WorkbenchConfig workbenchConfig;
  @Mock
  private ProfileApi profileApi;
  @Mock
  private BillingApi billingApi;
  @Mock
  private WorkspacesApi workspacesApi;
  @Mock
  private GroupsApi groupsApi;
  @Mock
  private GroupsApi endUserGroupsApi;

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Before
  public void setUp() {
    service = new FireCloudServiceImpl(Providers.of(workbenchConfig),
        Providers.of(profileApi), Providers.of(billingApi), Providers.of(groupsApi),
        Providers.of(endUserGroupsApi), Providers.of(workspacesApi),
        new FirecloudRetryHandler(new NoBackOffPolicy()));
  }

  @Test(expected = NotFoundException.class)
  public void testGetMe_throwsNotFound() throws ApiException {
    when(profileApi.me()).thenThrow(new ApiException(404, "blah"));
    service.getMe();
  }

  @Test(expected = ForbiddenException.class)
  public void testGetMe_throwsForbidden() throws ApiException {
    when(profileApi.me()).thenThrow(new ApiException(403, "blah"));
    service.getMe();
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetMe_throwsUnauthorized() throws ApiException {
    when(profileApi.me()).thenThrow(new ApiException(401, "blah"));
    service.getMe();
  }

  @Test
  public void testIsUserMemberOfGroup_none() throws Exception {
    when(endUserGroupsApi.getGroups()).thenReturn(new ArrayList<ManagedGroupAccessResponse>());
    assertThat(service.isUserMemberOfGroup("group")).isFalse();
  }

  @Test
  public void testIsUserMemberOfGroup_noNameMatch() throws Exception {
    when(endUserGroupsApi.getGroups()).thenReturn(
        Lists.newArrayList(new ManagedGroupAccessResponse().groupName("blah").role("Member")));
    assertThat(service.isUserMemberOfGroup("group")).isFalse();
  }

  @Test
  public void testIsUserMemberOfGroup_noRoleMatch() throws Exception {
    when(endUserGroupsApi.getGroups()).thenReturn(
        Lists.newArrayList(new ManagedGroupAccessResponse().groupName("group").role("notmember")));
    assertThat(service.isUserMemberOfGroup("group")).isFalse();
  }

  @Test
  public void testIsUserMemberOfGroup_match() throws Exception {
    when(endUserGroupsApi.getGroups()).thenReturn(
        Lists.newArrayList(new ManagedGroupAccessResponse().groupName("group").role("member")));
    assertThat(service.isUserMemberOfGroup("group")).isTrue();
  }
}
