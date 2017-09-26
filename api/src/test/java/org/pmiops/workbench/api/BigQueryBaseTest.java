package org.pmiops.workbench.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.Dataset;
import com.google.cloud.bigquery.DatasetInfo;
import com.google.cloud.bigquery.InsertAllRequest;
import com.google.cloud.bigquery.InsertAllResponse;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardTableDefinition;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.TableInfo;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.bitbucket.radistao.test.annotation.AfterAllMethods;
import org.bitbucket.radistao.test.annotation.BeforeAllMethods;
import org.pmiops.workbench.api.config.TestBigQueryConfig;
import org.pmiops.workbench.config.WorkbenchConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@SpringBootTest(classes = {TestBigQueryConfig.class})
public abstract class BigQueryBaseTest {

    private static final Logger log = Logger.getLogger(BigQueryBaseTest.class.getName());

    @Autowired
    BigQuery bigquery;

    @Autowired
    WorkbenchConfig workbenchConfig;

    public static final String BASE_PATH = "src/test/resources/bigquery/";

    @BeforeAllMethods
    public void setUp() throws Exception {
        createDataSet(workbenchConfig.bigquery.dataSetId);
        for (String tableName: getTableNames()) {
            createTable(workbenchConfig.bigquery.dataSetId, tableName);
            insertData(workbenchConfig.bigquery.dataSetId, tableName);
        }
    }

    @AfterAllMethods
    public void tearDown() throws Exception {
        for (String tableName: getTableNames()) {
            deleteTable(workbenchConfig.bigquery.dataSetId, tableName);
        }
        deleteDataSet(workbenchConfig.bigquery.dataSetId);
    }

    public abstract List<String> getTableNames();

    private void createDataSet(String dataSetId) {
        Dataset dataSet = bigquery.create(DatasetInfo.newBuilder(dataSetId).build());
    }

    private void createTable(String dataSetId, String tableId) throws Exception {
        ObjectMapper jackson = new ObjectMapper();
        String rawJson =
                new String(Files.readAllBytes(Paths.get(BASE_PATH + "schema/" + tableId.toLowerCase() + ".json")), Charset.defaultCharset());
        JsonNode newJson = jackson.readTree(rawJson);

        Gson gson = new Gson();
        Schema schema = gson.fromJson(newJson.toString(), Schema.class);
        StandardTableDefinition tableDef = StandardTableDefinition.of(schema);
        bigquery.create(TableInfo.of(TableId.of(dataSetId, tableId), tableDef));
    }

    private void insertData(String dataSetId, String tableId) throws Exception {
        ObjectMapper jackson = new ObjectMapper();
        String rawJson =
                new String(Files.readAllBytes(Paths.get(BASE_PATH + "data/" + tableId.toLowerCase() + "_data.json")), Charset.defaultCharset());
        JsonNode newJson = jackson.readTree(rawJson);

        Gson gson = new Gson();
        Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
        List<Map<String, Object>> rows = gson.fromJson(newJson.toString(), type);

        List<InsertAllRequest.RowToInsert> allRows = new ArrayList<>();
        int i = 0;
        for (Map<String, Object> row: rows) {
            allRows.add(InsertAllRequest.RowToInsert.of("key" + i++, row));
        }

        InsertAllRequest insertRequest = InsertAllRequest
                .newBuilder(TableId.of(dataSetId, tableId))
                .setRows(allRows).build();

        InsertAllResponse insertResponse = bigquery.insertAll(insertRequest);
        if (insertResponse.hasErrors()) {
            throw new RuntimeException("Errors occurred while inserting rows: " + insertResponse.getInsertErrors().toString());
        }
    }

    private void deleteTable(String dataSetId, String tableId) throws Exception {
        if (!bigquery.delete(TableId.of(dataSetId, tableId))) {
            throw new RuntimeException("Errors occurred while deleting table: " + dataSetId + ":" + tableId);
        }
    }

    private void deleteDataSet(String dataSetId) throws Exception {
        if (!bigquery.delete(dataSetId)) {
            throw new RuntimeException("Errors occurred while deleting dataset: " + dataSetId);
        }
    }
}
