package org.pmiops.workbench.elasticsearch

import com.google.cloud.bigquery.FieldValue
import com.google.cloud.bigquery.FieldValueList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.Maps
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.Objects
import java.util.stream.Collectors
import org.elasticsearch.common.xcontent.XContentBuilder
import org.elasticsearch.common.xcontent.XContentFactory
import org.elasticsearch.common.xcontent.XContentType

/**
 * ElasticDocument is primarily an intermediate model to be used for ingestion. It includes
 * knowledge of the BigQuery participant SELECT query and the corresponding Elasticsearch schema,
 * and therefore must be kept in sync with resources/bigquery/es_person.sql.
 */
class ElasticDocument private constructor(val id: String, val source: XContentBuilder) {
    /** The Elasticsearch primitive index types we utilize.  */
    private enum class ElasticType {
        KEYWORD,
        INTEGER,
        FLOAT,
        DATE,
        NESTED,
        TEXT,
        BOOLEAN;

        internal fun lower(): String {
            return this.name.toLowerCase()
        }
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o == null || javaClass != o.javaClass) {
            return false
        }
        val that = o as ElasticDocument?
        return id == that!!.id && source == that.source
    }

    override fun hashCode(): Int {
        return Objects.hash(id, source)
    }

    companion object {

        // An unfortunate manual mapping of column name to BigQuery index in the nested struct query for
        // domain types. BigQuery does not return a schema for nested types therefore we cannot lookup
        // result columns by name (only index).
        private val BIG_QUERY_COLUMN_INDICES = ImmutableMap.builder<String, Int>()
                .put("concept_id", 0)
                .put("source_concept_id", 1)
                .put("start_date", 2)
                .put("age_at_start", 3)
                .put("visit_concept_id", 4)
                .put("value_as_number", 5)
                .put("value_as_concept_id", 6)
                .put("value_as_source_concept_id", 7)
                .build()

        private val NESTED_FOREIGN_SCHEMA = ImmutableMap.of<String, Any>(
                "type", ElasticType.NESTED.lower(),
                "properties",
                ImmutableMap.builder<Any, Any>()
                        .put("concept_id", esType(ElasticType.KEYWORD))
                        .put("source_concept_id", esType(ElasticType.KEYWORD))
                        // Domain-dependent fields follow (may be unpopulated).
                        .put("start_date", esType(ElasticType.DATE))
                        .put("age_at_start", esType(ElasticType.INTEGER))
                        .put("visit_concept_id", esType(ElasticType.KEYWORD))
                        .put("value_as_number", esType(ElasticType.FLOAT))
                        .put("value_as_concept_id", esType(ElasticType.KEYWORD))
                        .put("value_as_source_concept_id", esType(ElasticType.KEYWORD))
                        .build())

        val PERSON_SCHEMA: Map<String, Any> = ImmutableMap.builder<String, Any>()
                .put("birth_datetime", esType(ElasticType.DATE))
                .put("gender_concept_id", esType(ElasticType.KEYWORD))
                .put("gender_concept_name", esType(ElasticType.KEYWORD))
                .put("race_concept_id", esType(ElasticType.KEYWORD))
                .put("race_concept_name", esType(ElasticType.KEYWORD))
                .put("ethnicity_concept_id", esType(ElasticType.KEYWORD))
                .put("ethnicity_concept_name", esType(ElasticType.KEYWORD))
                .put("condition_concept_ids", esType(ElasticType.KEYWORD))
                .put("condition_source_concept_ids", esType(ElasticType.KEYWORD))
                .put("observation_concept_ids", esType(ElasticType.KEYWORD))
                .put("observation_source_concept_ids", esType(ElasticType.KEYWORD))
                .put("drug_concept_ids", esType(ElasticType.KEYWORD))
                .put("drug_source_concept_ids", esType(ElasticType.KEYWORD))
                .put("procedure_concept_ids", esType(ElasticType.KEYWORD))
                .put("procedure_source_concept_ids", esType(ElasticType.KEYWORD))
                .put("measurement_concept_ids", esType(ElasticType.KEYWORD))
                .put("measurement_source_concept_ids", esType(ElasticType.KEYWORD))
                .put("visit_concept_ids", esType(ElasticType.KEYWORD))
                .put("is_deceased", esType(ElasticType.BOOLEAN))
                .put("events", NESTED_FOREIGN_SCHEMA)
                .build()

        private fun esType(t: ElasticType): Map<String, Any> {
            return ImmutableMap.of<String, Any>("type", t.lower())
        }

        /** Converts a line of BigQuery results JSON to an Elasticsearch document.  */
        @Throws(IOException::class)
        fun fromBigQueryJson(line: String): ElasticDocument {
            val doc = JsonParser().parse(line).asJsonObject
            val id = doc.remove("_id").asString
            val builder = XContentFactory.contentBuilder(XContentType.JSON)
            builder.rawValue(ByteArrayInputStream(doc.toString().toByteArray()), XContentType.JSON)
            return ElasticDocument(id, builder)
        }

        /** Converts a row of BigQuery results to an Elasticsearch document suitable for indexing.  */
        fun fromBigQueryResults(fvl: FieldValueList): ElasticDocument {
            // The id must be ingested separately from the source document, so we first remove it from the
            // BigQuery JSON results.
            val id = fvl.get("_id").stringValue
            val source = bqToElasticSchema(fvl, PERSON_SCHEMA)
            try {
                val builder = XContentFactory.contentBuilder(XContentType.JSON)
                builder.map(source)
                return ElasticDocument(id, builder)
            } catch (e: IOException) {
                // This shouldn't happen since we create the input map in memory, there is no IO here.
                throw RuntimeException(e)
            }

        }

        /**
         * Recursively convert BigQuery results into a format consumable by Elastic. BigQuery result
         * columns must exactly match the target Elasticsearch schema, and the resulting documents are
         * encoded as a nested "JSON" structure of type Map<String></String>, Object>.
         */
        private fun bqToElasticSchema(
                fvl: FieldValueList, elasticSchema: Map<String, Any>): Map<String, Any> {
            val doc = Maps.newHashMap<String, Any>()
            for (k in elasticSchema.keys) {
                val prop = elasticSchema[k] as Map<String, Any>
                val esType = ElasticType.valueOf((prop["type"] as String).toUpperCase())

                val fv: FieldValue
                if (fvl.hasSchema()) {
                    fv = fvl.get(k)
                } else {
                    // BigQuery struct columns don't have defined schemas in the Java client.
                    fv = fvl.get(BIG_QUERY_COLUMN_INDICES[k])
                }
                if (fv.isNull) {
                    continue
                }

                val isRepeated = FieldValue.Attribute.REPEATED == fv.attribute
                val `val`: Any
                when (esType) {
                    ElasticDocument.ElasticType.INTEGER, ElasticDocument.ElasticType.FLOAT -> if (isRepeated) {
                        `val` = fv.repeatedValue.stream()
                                .map<BigDecimal>(Function<FieldValue, BigDecimal> { it.getNumericValue() })
                                .collect<List<BigDecimal>, Any>(Collectors.toList<BigDecimal>())
                    } else {
                        `val` = fv.numericValue
                    }
                    ElasticDocument.ElasticType.DATE, ElasticDocument.ElasticType.KEYWORD, ElasticDocument.ElasticType.TEXT -> if (isRepeated) {
                        `val` = fv.repeatedValue.stream()
                                .map<String>(Function<FieldValue, String> { it.getStringValue() })
                                .collect<List<String>, Any>(Collectors.toList())
                    } else {
                        `val` = fv.stringValue
                    }
                    ElasticDocument.ElasticType.NESTED -> {
                        val nestedSchema = prop["properties"] as Map<String, Any>
                        if (isRepeated) {
                            `val` = fv.repeatedValue.stream()
                                    .map { f -> bqToElasticSchema(f.recordValue, nestedSchema) }
                                    .collect<List<Map<String, Any>>, Any>(Collectors.toList())
                        } else {
                            `val` = bqToElasticSchema(fv.recordValue, nestedSchema)
                        }
                    }
                    else -> throw RuntimeException("Unhandled ES type: $esType")
                }
                doc[k] = `val`
            }
            return doc
        }
    }
}
