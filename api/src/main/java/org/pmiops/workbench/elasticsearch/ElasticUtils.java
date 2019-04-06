package org.pmiops.workbench.elasticsearch;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
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
import org.springframework.retry.backoff.ExponentialRandomBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * Shared utilities for indexing and access All of Us data in Elasticsearch.
 */
public final class ElasticUtils {
  private static final Logger log = Logger.getLogger(ElasticUtils.class.getName());

  // These parameters were determined experimentally, and work fairly well on the default Elastic
  // Cloud cluster configuration, but are likely not optimal.
  // - 4/12/19 run: synthetic 1m dataset took 1h54m @ ~8.3K docs/min
  private static final int BATCH_SIZE = 200;
  private static final int BATCH_POOL_SIZE = 3;

  // Elastic timeout is ~30s, this allows for several attempts.
  private static final int MAX_BACKOFF_INTERVAL_MS = 5 * 60 * 1000;

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
              INDEX_TYPE, ImmutableMap.of(
                  // Do not allow new fields to appear in the mapping, this would indicate a
                  // programming error.
                  "dynamic", "strict",
                  "properties", ElasticDocument.PERSON_SCHEMA)),
          REQ_OPTS);
    log.info("created person index: " + indexName);
  }

  /**
   * Ingests the given Elasticsearch documents using bulk ingestion. If documents of the same ID
   * already exist in the index, they will instead be updated. Uses a threadpool to dispatch
   * multiple batch requests simultaneously.
   */
  public static void ingestDocuments(RestHighLevelClient client, String indexName,
      Iterator<ElasticDocument> docs, int numDocs) throws InterruptedException {
    // Need to define our own blocking queue; the default from Executors.fixedThreadPool() uses
    // an unbounded queue. When the queue is full, we use a CallerRunsPolicy as the simplest
    // fallback (by default, it throws an exception). This shouldn't significantly affect throughput
    // since our queue size is double our thread count.
    BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(2 * BATCH_POOL_SIZE, /* fair */ true);
    ExecutorService pool =
        new ThreadPoolExecutor(BATCH_POOL_SIZE, BATCH_POOL_SIZE, 0L, TimeUnit.MILLISECONDS, queue,
            new ThreadPoolExecutor.CallerRunsPolicy());

    Stopwatch timer = Stopwatch.createStarted();
    final AtomicInteger count = new AtomicInteger();
    final AtomicInteger fails = new AtomicInteger();
    for (int i = 0; docs.hasNext(); i += BATCH_SIZE) {
      BulkRequest bulkReq = new BulkRequest();
      int j;
      for (j = 0; j < BATCH_SIZE && docs.hasNext(); j++) {
        ElasticDocument doc = docs.next();
        bulkReq.add(new IndexRequest(indexName, INDEX_TYPE, doc.id)
            .source(doc.source));
      }

      // Adds work to the pool, or runs this callback on the current thread if the queue is full.
      pool.submit(() -> {
        BulkResponse response;
        try {
          response = retryTemplate().execute((context) -> {
            try {
              return client.bulk(bulkReq, REQ_OPTS);
            } catch (IOException e) {
              log.log(Level.WARNING,
                  "bulk request attempt " + context.getRetryCount() + " failed, retrying...", e);
              throw e;
            }
          });
        } catch (IOException e) {
          log.log(Level.SEVERE, "bulk insertion failed", e);
          fails.addAndGet(bulkReq.numberOfActions());
          return;
        }

        int numFails = 0;
        if (response.hasFailures()) {
          for (BulkItemResponse itemResp : response.getItems()) {
              if (itemResp.isFailed()) {
                log.warning(itemResp.getFailureMessage());
                numFails++;
              }
            }
          }
          count.addAndGet(bulkReq.numberOfActions() - numFails);
          fails.addAndGet(numFails);
        });

      int inserted = count.get();
      int failed = fails.get();
      int submitted = i + j - failed;
      double rate = docsPerMinute(inserted, timer.elapsed().toMillis());
      double hoursRemaining = ((double) (numDocs - inserted - failed)) / rate / 60.0;
      log.info(String.format(
          "Submitted %.2f%% of documents (%d/%d) (%d failed) @ %.2f docs/minute [~%.1fh remaining]",
          ((float) 100 * (submitted)) / numDocs, submitted, numDocs, failed, rate,
          hoursRemaining));
    }

    log.info(String.format("All documents submitted, awaiting remaining threads"));
    pool.shutdown();
    pool.awaitTermination(15, TimeUnit.MINUTES);
    timer.stop();

    log.info(String.format("Indexing complete (%d/%d) (%d failed)",
        numDocs - fails.get(), numDocs, fails.get()));
    Duration t = timer.elapsed();
    log.info(String.format("Indexing took %dh %dm %ds; average ~%.2f documents indexed per minute",
        t.toHours(), t.toMinutes() % 60, t.getSeconds() % 60,
        docsPerMinute(numDocs, t.toMillis())));
  }

  private static RetryTemplate retryTemplate() {
    RetryTemplate tmpl = new RetryTemplate();
    ExponentialRandomBackOffPolicy backoff = new ExponentialRandomBackOffPolicy();
    backoff.setMaxInterval(MAX_BACKOFF_INTERVAL_MS);
    tmpl.setBackOffPolicy(backoff);
    SimpleRetryPolicy retry = new SimpleRetryPolicy();
    retry.setMaxAttempts(20);
    tmpl.setRetryPolicy(retry);
    tmpl.setThrowLastExceptionOnExhausted(true);
    return tmpl;
  }

  private static double docsPerMinute(int processed, long elapsedMillis) {
    return 60 * 1000 * (((double) processed ) / ((double) elapsedMillis));
  }

  public static LocalDate todayMinusYears(int years) {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    return now.minusYears(years).toLocalDate();
  }

}
