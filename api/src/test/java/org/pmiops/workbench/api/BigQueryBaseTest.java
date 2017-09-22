package org.pmiops.workbench.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.bigquery.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.runner.RunWith;
import org.pmiops.workbench.api.config.TestBigQueryConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.logging.Logger;

//@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {TestBigQueryConfig.class})
public class BigQueryBaseTest {

    private static final Logger log = Logger.getLogger(BigQueryBaseTest.class.getName());

    @Autowired
    BigQuery bigquery;

    public void createDataSet(String dataSetId) {
        Dataset dataSet = bigquery.create(DatasetInfo.newBuilder(dataSetId).build());
    }

    public void createTable(String dataSetId, String tableId) throws Exception {
        String basePath = "src/test/resources/bigquery/schema/";
        ObjectMapper jackson = new ObjectMapper();
        String rawJson =
                new String(Files.readAllBytes(Paths.get(basePath + tableId + ".json")), Charset.defaultCharset());
        JsonNode newJson = jackson.readTree(rawJson);

        Gson gson = new Gson();
        Schema schema = gson.fromJson(newJson.toString(), Schema.class);
        StandardTableDefinition tableDef = StandardTableDefinition.of(schema);
        bigquery.create(TableInfo.of(TableId.of(dataSetId, tableId), tableDef));
    }

    public boolean insertRow(String dataSetId, String tableId) throws Exception {
        String basePath = "src/test/resources/bigquery/data/";
        ObjectMapper jackson = new ObjectMapper();
        String rawJson =
                new String(Files.readAllBytes(Paths.get(basePath + tableId + "_data.json")), Charset.defaultCharset());
        JsonNode newJson = jackson.readTree(rawJson);

        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, Object>>(){}.getType();
        Map<String, Object> row = gson.fromJson(newJson.toString(), type);

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
