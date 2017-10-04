package org.pmiops.workbench.cohortbuilder.querybuilder;

import org.pmiops.workbench.api.config.TestBigQueryConfig;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {TestBigQueryConfig.class})
public class BaseQueryBuilderTest {

    @Autowired
    WorkbenchConfig workbenchConfig;

    protected String getTablePrefix() {
        return workbenchConfig.bigquery.projectId + "." + workbenchConfig.bigquery.dataSetId;
    }
}
