package org.pmiops.workbench.elasticsearch;

import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;

/**
 * ElasticDocument is primarily an intermediate model to be used for ingestion. It includes
 * knowledge of the BigQuery participant SELECT query and the corresponding Elasticsearch schema,
 * and therefore must be kept in sync with resources/bigquery/es_person.sql.
 */
public class ElasticDocument {
  /** The Elasticsearch primitive index types we utilize. */
  private enum ElasticType {
    KEYWORD,
    INTEGER,
    FLOAT,
    DATE,
    NESTED,
    TEXT,
    BOOLEAN;

    String lower() {
      return this.name().toLowerCase();
    }
  }

  // An unfortunate manual mapping of column name to BigQuery index in the nested struct query for
  // domain types. BigQuery does not return a schema for nested types therefore we cannot lookup
  // result columns by name (only index).
  private static final Map<String, Integer> BIG_QUERY_COLUMN_INDICES =
      ImmutableMap.<String, Integer>builder()
          .put("concept_id", 0)
          .put("source_concept_id", 1)
          .put("start_date", 2)
          .put("age_at_start", 3)
          .put("visit_concept_id", 4)
          .put("value_as_number", 5)
          .put("value_as_concept_id", 6)
          .put("value_as_source_concept_id", 7)
          .build();

  private static final Map<String, Object> NESTED_FOREIGN_SCHEMA =
      ImmutableMap.of(
          "type", ElasticType.NESTED.lower(),
          "properties",
              ImmutableMap.builder()
                  .put("concept_id", esType(ElasticType.KEYWORD))
                  .put("source_concept_id", esType(ElasticType.KEYWORD))
                  // Domain-dependent fields follow (may be unpopulated).
                  .put("start_date", esType(ElasticType.DATE))
                  .put("age_at_start", esType(ElasticType.INTEGER))
                  .put("visit_concept_id", esType(ElasticType.KEYWORD))
                  .put("value_as_number", esType(ElasticType.FLOAT))
                  .put("value_as_concept_id", esType(ElasticType.KEYWORD))
                  .put("value_as_source_concept_id", esType(ElasticType.KEYWORD))
                  .build());

  public static final Map<String, Object> PERSON_SCHEMA =
      ImmutableMap.<String, Object>builder()
          .put("birth_datetime", esType(ElasticType.DATE))
          .put("age_at_consent", esType(ElasticType.INTEGER))
          .put("age_at_cdr", esType(ElasticType.INTEGER))
          .put("gender_concept_id", esType(ElasticType.KEYWORD))
          .put("gender_concept_name", esType(ElasticType.KEYWORD))
          .put("race_concept_id", esType(ElasticType.KEYWORD))
          .put("race_concept_name", esType(ElasticType.KEYWORD))
          .put("ethnicity_concept_id", esType(ElasticType.KEYWORD))
          .put("ethnicity_concept_name", esType(ElasticType.KEYWORD))
          .put("sex_at_birth_concept_id", esType(ElasticType.KEYWORD))
          .put("sex_at_birth_concept_name", esType(ElasticType.KEYWORD))
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
          .put("has_ehr_data", esType(ElasticType.BOOLEAN))
          .put("has_physical_measurement_data", esType(ElasticType.BOOLEAN))
          .put("events", NESTED_FOREIGN_SCHEMA)
          .build();

  private static Map<String, Object> esType(ElasticType t) {
    return ImmutableMap.of("type", t.lower());
  }

  public final String id;
  public final XContentBuilder source;

  private ElasticDocument(String id, XContentBuilder source) {
    this.id = id;
    this.source = source;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ElasticDocument that = (ElasticDocument) o;
    return id.equals(that.id) && source.equals(that.source);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, source);
  }

  /** Converts a line of BigQuery results JSON to an Elasticsearch document. */
  public static ElasticDocument fromBigQueryJson(String line) throws IOException {
    JsonObject doc = new JsonParser().parse(line).getAsJsonObject();
    String id = doc.remove("_id").getAsString();
    XContentBuilder builder = XContentFactory.contentBuilder(XContentType.JSON);
    builder.rawValue(new ByteArrayInputStream(doc.toString().getBytes()), XContentType.JSON);
    return new ElasticDocument(id, builder);
  }

  /** Converts a row of BigQuery results to an Elasticsearch document suitable for indexing. */
  public static ElasticDocument fromBigQueryResults(FieldValueList fvl) {
    // The id must be ingested separately from the source document, so we first remove it from the
    // BigQuery JSON results.
    String id = fvl.get("_id").getStringValue();
    Map<String, Object> source = bqToElasticSchema(fvl, PERSON_SCHEMA);
    try {
      XContentBuilder builder = XContentFactory.contentBuilder(XContentType.JSON);
      builder.map(source);
      return new ElasticDocument(id, builder);
    } catch (IOException e) {
      // This shouldn't happen since we create the input map in memory, there is no IO here.
      throw new RuntimeException(e);
    }
  }

  /**
   * Recursively convert BigQuery results into a format consumable by Elastic. BigQuery result
   * columns must exactly match the target Elasticsearch schema, and the resulting documents are
   * encoded as a nested "JSON" structure of type Map<String, Object>.
   */
  @SuppressWarnings("unchecked")
  private static Map<String, Object> bqToElasticSchema(
      FieldValueList fvl, Map<String, Object> elasticSchema) {
    Map<String, Object> doc = Maps.newHashMap();
    for (String k : elasticSchema.keySet()) {
      Map<String, Object> prop = (Map<String, Object>) elasticSchema.get(k);
      ElasticType esType = ElasticType.valueOf(((String) prop.get("type")).toUpperCase());

      FieldValue fv;
      if (fvl.hasSchema()) {
        fv = fvl.get(k);
      } else {
        // BigQuery struct columns don't have defined schemas in the Java client.
        fv = fvl.get(BIG_QUERY_COLUMN_INDICES.get(k));
      }
      if (fv.isNull()) {
        continue;
      }

      boolean isRepeated = FieldValue.Attribute.REPEATED.equals(fv.getAttribute());
      Object val;
      switch (esType) {
        case INTEGER:
        case FLOAT:
          if (isRepeated) {
            val =
                fv.getRepeatedValue().stream()
                    .map(FieldValue::getNumericValue)
                    .collect(Collectors.toList());
          } else {
            val = fv.getNumericValue();
          }
          break;
        case DATE:
        case KEYWORD:
        case TEXT:
          if (isRepeated) {
            val =
                fv.getRepeatedValue().stream()
                    .map(FieldValue::getStringValue)
                    .collect(Collectors.toList());
          } else {
            val = fv.getStringValue();
          }
          break;
        case NESTED:
          Map<String, Object> nestedSchema = (Map<String, Object>) prop.get("properties");
          if (isRepeated) {
            val =
                fv.getRepeatedValue().stream()
                    .map(f -> bqToElasticSchema(f.getRecordValue(), nestedSchema))
                    .collect(Collectors.toList());
          } else {
            val = bqToElasticSchema(fv.getRecordValue(), nestedSchema);
          }
          break;
        default:
          throw new RuntimeException("Unhandled ES type: " + esType.toString());
      }
      doc.put(k, val);
    }
    return doc;
  }
}
