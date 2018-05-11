package org.pmiops.workbench;

import org.junit.Test;
import org.mockito.Mock;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.pmiops.workbench.firecloud.FireCloudServiceImpl;
import org.pmiops.workbench.firecloud.FirecloudRetryHandler;
import org.pmiops.workbench.firecloud.api.BillingApi;
import org.pmiops.workbench.firecloud.api.GroupsApi;
import org.pmiops.workbench.firecloud.api.ProfileApi;
import org.pmiops.workbench.firecloud.api.WorkspacesApi;
import org.pmiops.workbench.test.Providers;
import org.springframework.retry.backoff.NoBackOffPolicy;

import static com.google.common.truth.Truth.assertThat;

public class FireCloudIntegrationTest {

    /*
     * Mocked service providers are (currently) not available for functional integration tests.
     * Integration tests with real users/workspaces against production FireCloud is not
     * recommended at this time.
     */
    @Mock
    private ProfileApi profileApi;
    @Mock
    private BillingApi billingApi;
    @Mock
    private WorkspacesApi workspacesApi;
    @Mock
    private GroupsApi groupsApi;

    private final FireCloudServiceImpl fireCloudService = new FireCloudServiceImpl(
        Providers.of(createConfig()),
        Providers.of(profileApi),
        Providers.of(billingApi),
        Providers.of(groupsApi),
        Providers.of(workspacesApi),
        new FirecloudRetryHandler(new NoBackOffPolicy())
    );

    @Test
    public void testStatus() {
        assertThat(fireCloudService.getFirecloudStatus()).isTrue();
    }

    private static WorkbenchConfig createConfig() {
        WorkbenchConfig config = new WorkbenchConfig();
        config.firecloud = new WorkbenchConfig.FireCloudConfig();
        config.firecloud.debugEndpoints = true;
        return config;
    }

}
