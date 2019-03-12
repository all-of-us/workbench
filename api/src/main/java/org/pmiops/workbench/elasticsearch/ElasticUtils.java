package org.pmiops.workbench.elasticsearch;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Logger;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;

/**
 * Shared utilities for indexing and access All of Us data in Elasticsearch.
 */
public final class ElasticUtils {
  private static final Logger log = Logger.getLogger(ElasticUtils.class.getName());
  private static final int BATCH_SIZE = 200;

  // Recommended document type for all indices is "_doc" (types are being deprecated):
  // https://www.elastic.co/guide/en/elasticsearch/reference/current/removal-of-types.html
  public static final String INDEX_TYPE = "_doc";
  public static final RequestOptions REQ_OPTS = RequestOptions.DEFAULT;

  private ElasticUtils() {}

  /**
   * Returns the canonical person index name for the given CDR version.
   */
  public static String personIndexName(String cdrVersion) {
    return cdrVersion + "_person";
  }

  /**
   * Creates a new person index and optionally deletes any existing index of the same name. Applies
   * the expected field mapping for the index type.
   */
  public static void createPersonIndex(RestHighLevelClient client, String indexName,
      boolean deleteExisting) throws IOException {
    if (deleteExisting &&
        client.indices().exists(new GetIndexRequest().indices(indexName), REQ_OPTS)) {
      client.indices().delete(new DeleteIndexRequest(indexName), REQ_OPTS);
      log.info("deleted existing index: " + indexName);
    }
    client.indices().create(
          new CreateIndexRequest(indexName).mapping(
              INDEX_TYPE, ImmutableMap.of("properties", ElasticDocument.PERSON_SCHEMA)),
          REQ_OPTS);
    log.info("created person index: " + indexName);
  }

  /**
   * Ingests the given Elasticsearch documents using bulk ingestion. If documents of the same ID
   * already exist in the index, they will instead be updated.
   */
  public static void ingestDocuments(RestHighLevelClient client, String indexName,
      Iterator<ElasticDocument> docs, int numDocs) throws IOException {
    int fails = 0;
    for (int i = 0; docs.hasNext(); i += BATCH_SIZE) {
      BulkRequest bulkReq = new BulkRequest();
      int j;
      for (j = 0; j < BATCH_SIZE && docs.hasNext(); j++) {
        ElasticDocument doc = docs.next();
        bulkReq.add(new IndexRequest(indexName, INDEX_TYPE, doc.id)
            .source(doc.source));
      }
      BulkResponse response = client.bulk(bulkReq, REQ_OPTS);
      if (response.hasFailures()) {
        for (BulkItemResponse itemResp : response.getItems()) {
          if (itemResp.isFailed()) {
            // TODO: Investigate retry handling or aggregate failures.
            log.warning(itemResp.getFailureMessage());
            fails++;
          }
        }
      }

      int inserted = i + j - fails;
      log.info(String.format("Inserted %.2f%% of documents (%d/%d) (%d failed)",
          ((float) 100 * (inserted)) / numDocs, inserted, numDocs, fails));
    }
  }

}
