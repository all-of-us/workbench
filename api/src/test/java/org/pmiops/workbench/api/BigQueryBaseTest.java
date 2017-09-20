package org.pmiops.workbench.api;

import com.google.cloud.bigquery.*;
import org.junit.runner.RunWith;
import org.pmiops.workbench.api.config.TestBigQueryConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {TestBigQueryConfig.class})
@ActiveProfiles("local")
public class BigQueryBaseTest {

    private static final Logger log = Logger.getLogger(BigQueryBaseTest.class.getName());

    @Autowired
    BigQuery bigquery;

    public void createDataSet(String dataSetId) {
        Dataset dataSet = bigquery.create(DatasetInfo.newBuilder(dataSetId).build());
    }

    public void createTable(String dataSetId, String tableId, List<Field> fields) {
        StandardTableDefinition tableDef = StandardTableDefinition.of(Schema.of(fields));
        bigquery.create(TableInfo.of(TableId.of(dataSetId, tableId), tableDef));
    }

    public boolean insertRow(String dataSetId, String tableId, Map<String, Object> row) {
        InsertAllRequest insertRequest = InsertAllRequest
                .newBuilder(TableId.of(dataSetId, tableId))
                .addRow(row).build();
        InsertAllResponse insertResponse = bigquery.insertAll(insertRequest);
        if (insertResponse.hasErrors()) {
            log.info("Errors occurred while inserting rows");
            return false;
        }
        return true;
    }

    public boolean deleteTable(String dataSetId, String tableId) {
        return bigquery.delete(TableId.of(dataSetId, tableId));
    }

    public boolean deleteDataSet(String dataSetId) {
        return bigquery.delete(dataSetId);
    }
}
