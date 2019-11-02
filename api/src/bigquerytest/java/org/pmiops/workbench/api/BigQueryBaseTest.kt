package org.pmiops.workbench.api

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.DatasetInfo
import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.InsertAllRequest
import com.google.cloud.bigquery.InsertAllResponse
import com.google.cloud.bigquery.LegacySQLTypeName
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardTableDefinition
import com.google.cloud.bigquery.TableId
import com.google.cloud.bigquery.TableInfo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.util.ArrayList
import org.bitbucket.radistao.test.annotation.AfterAllMethods
import org.bitbucket.radistao.test.annotation.BeforeAllMethods
import org.pmiops.workbench.testconfig.TestBigQueryConfig
import org.pmiops.workbench.testconfig.TestWorkbenchConfig
import org.pmiops.workbench.testconfig.WorkbenchConfigConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [TestBigQueryConfig::class, WorkbenchConfigConfig::class])
abstract class BigQueryBaseTest {

    @Autowired
    internal var bigquery: BigQuery? = null

    @Autowired
    internal var workbenchConfig: TestWorkbenchConfig? = null

    abstract val tableNames: List<String>

    abstract val testDataDirectory: String

    @BeforeAllMethods
    @Throws(Exception::class)
    fun beforeAllMethodsSetUp() {
        createDataSet(workbenchConfig!!.bigquery!!.dataSetId)
        for (tableName in tableNames) {
            createTable(workbenchConfig!!.bigquery!!.dataSetId, tableName)
            insertData(workbenchConfig!!.bigquery!!.dataSetId, tableName)
        }
    }

    @AfterAllMethods
    @Throws(Exception::class)
    fun beforeAllMethodsTearDown() {
        for (tableName in tableNames) {
            deleteTable(workbenchConfig!!.bigquery!!.dataSetId, tableName)
        }
        deleteDataSet(workbenchConfig!!.bigquery!!.dataSetId)
    }

    private fun createDataSet(dataSetId: String?) {
        bigquery!!.create(DatasetInfo.newBuilder(dataSetId!!).build())
    }

    @Throws(Exception::class)
    private fun createTable(dataSetId: String?, tableId: String) {
        val jackson = ObjectMapper()
        val rawJson = String(
                Files.readAllBytes(Paths.get(BASE_PATH + "schema/" + tableId + ".json")),
                Charset.defaultCharset())
        val newJson = jackson.readTree(rawJson)

        val gson = Gson()
        val schema = parseSchema(gson.fromJson(newJson.toString(), Array<Column>::class.java))
        val tableDef = StandardTableDefinition.of(schema)
        bigquery!!.create(TableInfo.of(TableId.of(dataSetId!!, tableId), tableDef))
    }

    private fun parseSchema(columns: Array<Column>): Schema {
        val schemaFields = ArrayList<Field>()
        for (column in columns) {
            val typeString = column.type
            val fieldType: LegacySQLTypeName
            when (column.type!!.toLowerCase()) {
                "string" -> fieldType = LegacySQLTypeName.STRING
                "integer" -> fieldType = LegacySQLTypeName.INTEGER
                "timestamp" -> fieldType = LegacySQLTypeName.TIMESTAMP
                "float" -> fieldType = LegacySQLTypeName.FLOAT
                "boolean" -> fieldType = LegacySQLTypeName.BOOLEAN
                "date" -> fieldType = LegacySQLTypeName.DATE
                else -> throw IllegalArgumentException("Unrecognized field type '$typeString'.")
            }
            schemaFields.add(Field.of(column.name, fieldType))
        }
        return Schema.of(schemaFields)
    }

    @Throws(Exception::class)
    private fun insertData(dataSetId: String?, tableId: String) {
        val jackson = ObjectMapper()
        val rawJson = String(
                Files.readAllBytes(
                        Paths.get(
                                BASE_PATH
                                        + testDataDirectory
                                        + "/"
                                        + tableId.toLowerCase()
                                        + "_data.json")),
                Charset.defaultCharset())
        val newJson = jackson.readTree(rawJson)

        val gson = Gson()
        val type = object : TypeToken<List<Map<String, Any>>>() {

        }.type
        val rows = gson.fromJson<List<Map<String, Any>>>(newJson.toString(), type)

        val allRows = ArrayList<InsertAllRequest.RowToInsert>()
        var i = 0
        for (row in rows) {
            allRows.add(InsertAllRequest.RowToInsert.of("key" + i++, row))
        }

        val insertRequest = InsertAllRequest.newBuilder(TableId.of(dataSetId!!, tableId)).setRows(allRows).build()

        val insertResponse = bigquery!!.insertAll(insertRequest)
        if (insertResponse.hasErrors()) {
            throw RuntimeException(
                    "Errors occurred while inserting rows: " + insertResponse.insertErrors.toString())
        }
    }

    @Throws(Exception::class)
    private fun deleteTable(dataSetId: String?, tableId: String) {
        if (!bigquery!!.delete(TableId.of(dataSetId!!, tableId))) {
            throw RuntimeException(
                    "Errors occurred while deleting table: $dataSetId:$tableId")
        }
    }

    @Throws(Exception::class)
    private fun deleteDataSet(dataSetId: String?) {
        if (!bigquery!!.delete(dataSetId)) {
            throw RuntimeException("Errors occurred while deleting dataset: " + dataSetId!!)
        }
    }

    private inner class Column {

        // Start stepping through the array from the beginning
        var type: String? = null
        var name: String? = null
        var mode: String? = null
    }

    companion object {

        val CB_DATA = "cbdata"
        val MATERIALIZED_DATA = "materializeddata"

        val BASE_PATH = "src/bigquerytest/resources/bigquery/"
    }
}
