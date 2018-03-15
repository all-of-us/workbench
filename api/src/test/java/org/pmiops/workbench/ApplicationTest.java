package org.pmiops.workbench;

import com.google.apphosting.api.ApiProxy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.api.CohortBuilderController;
import org.pmiops.workbench.api.ProfileController;
import org.pmiops.workbench.config.WebMvcConfig;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(SpringRunner.class)
@Import({TestJpaConfig.class})
@SpringBootTest(classes = {Application.class})
@PrepareForTest(ApiProxy.class)
public class ApplicationTest {

    @Autowired
    private CohortBuilderController controller;

    @Autowired
    private ProfileController profileController;

    @Autowired
    private WebMvcConfig webMvcConfig;

    @Test
    public void contextLoads() throws Exception {
        mockStatic(ApiProxy.class);
        when(ApiProxy.getCurrentEnvironment()).thenReturn(new TestEnvironment());

        assertThat(controller).isNotNull();
        assertThat(profileController).isNotNull();

//        verifyStatic();
    }

    public class TestEnvironment implements ApiProxy.Environment {

        @Override
        public String getAppId() {
            return null;
        }

        @Override
        public String getModuleId() {
            return null;
        }

        @Override
        public String getVersionId() {
            return null;
        }

        @Override
        public String getEmail() {
            return null;
        }

        @Override
        public boolean isLoggedIn() {
            return false;
        }

        @Override
        public boolean isAdmin() {
            return false;
        }

        @Override
        public String getAuthDomain() {
            return null;
        }

        @Override
        public String getRequestNamespace() {
            return null;
        }

        @Override
        public Map<String, Object> getAttributes() {
            return new HashMap<>();
        }

        @Override
        public long getRemainingMillis() {
            return 0;
        }
    }
}
