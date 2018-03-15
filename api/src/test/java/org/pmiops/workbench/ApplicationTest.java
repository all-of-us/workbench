package org.pmiops.workbench;

import com.google.apphosting.api.ApiProxy;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.pmiops.workbench.api.CohortBuilderController;
import org.pmiops.workbench.api.ProfileController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class})
@Import({TestJpaConfig.class})
@ActiveProfiles("test")
public class ApplicationTest {

    @Autowired
    private CohortBuilderController controller;

    @Autowired
    private ProfileController profileController;

    @Mock
    private ApiProxy apiProxy;

    @Test
    public void contextLoads() throws Exception {
        assertThat(controller).isNotNull();
        assertThat(profileController).isNotNull();
    }
}
