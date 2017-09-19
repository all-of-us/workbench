package org.pmiops.workbench.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.api.config.TestAppConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { TestAppConfig.class, CohortBuilderController.class })
@ActiveProfiles("test")
public class CohortBuilderControllerTest {

    @Autowired
    CohortBuilderController controller;

    @Test
    public void getCriteriaByTypeAndParentId() throws Exception {
        controller.getCriteriaByTypeAndParentId("icd9", 0L );
    }

}