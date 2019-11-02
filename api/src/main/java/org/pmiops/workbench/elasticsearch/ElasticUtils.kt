package org.pmiops.workbench.elasticsearch

import com.google.common.base.Stopwatch
import com.google.common.collect.ImmutableMap
import java.io.IOException
import java.time.Duration
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Level
import java.util.logging.Logger
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest
import org.elasticsearch.action.admin.indices.get.GetIndexRequest
import org.elasticsearch.action.bulk.BulkItemResponse
import org.elasticsearch.action.bulk.BulkRequest
import org.elasticsearch.action.bulk.BulkResponse
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.springframework.retry.backoff.ExponentialRandomBackOffPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate

/** Shared utilities for indexing and access All of Us data in Elasticsearch.  */
object ElasticUtils {
    private val log = Logger.getLogger(ElasticUtils::class.java.name)

    // These parameters were determined experimentally, and work fairly well on the default Elastic
    // Cloud cluster configuration, but are likely not optimal.
    // - 4/12/19 run: synthetic 1m dataset took 1h54m @ ~8.3K docs/min
    private val BATCH_SIZE = 200
    private val BATCH_POOL_SIZE = 3

    // Elastic timeout is ~30s, this allows for several attempts.
    private val MAX_BACKOFF_INTERVAL_MS = 5 * 60 * 1000

    // Recommended document type for all indices is "_doc" (types are being deprecated):
    // https://www.elastic.co/guide/en/elasticsearch/reference/current/removal-of-types.html
    val INDEX_TYPE = "_doc"
    val REQ_OPTS = RequestOptions.DEFAULT

    /** Returns the canonical person index name for the given CDR version.  */
    fun personIndexName(cdrVersion: String): String {
        return cdrVersion + "_person"
    }

    /**
     * Creates a new person index and optionally deletes any existing index of the same name. Applies
     * the expected field mapping for the index type.
     */
    @Throws(IOException::class)
    fun createPersonIndex(
            client: RestHighLevelClient, indexName: String, deleteExisting: Boolean) {
        if (deleteExisting && client.indices().exists(GetIndexRequest().indices(indexName), REQ_OPTS)) {
            client.indices().delete(DeleteIndexRequest(indexName), REQ_OPTS)
            log.info("deleted existing index: $indexName")
        }
        client
                .indices()
                .create(
                        CreateIndexRequest(indexName)
                                .mapping(
                                        INDEX_TYPE,
                                        ImmutableMap.of(
                                                // Do not allow new fields to appear in the mapping, this would indicate a
                                                // programming error.
                                                "dynamic", "strict", "properties", ElasticDocument.PERSON_SCHEMA)),
                        REQ_OPTS)
        log.info("created person index: $indexName")
    }

    /**
     * Ingests the given Elasticsearch documents using bulk ingestion. If documents of the same ID
     * already exist in the index, they will instead be updated. Uses a threadpool to dispatch
     * multiple batch requests simultaneously.
     */
    @Throws(InterruptedException::class)
    fun ingestDocuments(
            client: RestHighLevelClient, indexName: String, docs: Iterator<ElasticDocument>, numDocs: Int) {
        // Need to define our own blocking queue; the default from Executors.fixedThreadPool() uses
        // an unbounded queue. When the queue is full, we use a CallerRunsPolicy as the simplest
        // fallback (by default, it throws an exception). This shouldn't significantly affect throughput
        // since our queue size is double our thread count.
        val queue = ArrayBlockingQueue<Runnable>(2 * BATCH_POOL_SIZE, /* fair */ true)
        val pool = ThreadPoolExecutor(
                BATCH_POOL_SIZE,
                BATCH_POOL_SIZE,
                0L,
                TimeUnit.MILLISECONDS,
                queue,
                ThreadPoolExecutor.CallerRunsPolicy())

        val timer = Stopwatch.createStarted()
        val count = AtomicInteger()
        val fails = AtomicInteger()
        var i = 0
        while (docs.hasNext()) {
            val bulkReq = BulkRequest()
            var j: Int
            j = 0
            while (j < BATCH_SIZE && docs.hasNext()) {
                val doc = docs.next()
                bulkReq.add(IndexRequest(indexName, INDEX_TYPE, doc.id).source(doc.source))
                j++
            }

            // Adds work to the pool, or runs this callback on the current thread if the queue is full.
            pool.submit {
                val response: BulkResponse
                try {
                    response = retryTemplate()
                            .execute<BulkResponse, IOException> { context ->
                                try {
                                    return@retryTemplate ()
                                            .execute client . bulk bulkReq, REQ_OPTS)
                                } catch (e: IOException) {
                                    log.log(
                                            Level.WARNING,
                                            "bulk request attempt "
                                                    + context.retryCount
                                                    + " failed, retrying...",
                                            e)
                                    throw e
                                }
                            }
                } catch (e: IOException) {
                    log.log(Level.SEVERE, "bulk insertion failed", e)
                    fails.addAndGet(bulkReq.numberOfActions())
                    return@pool.submit
                }

                var numFails = 0
                if (response.hasFailures()) {
                    for (itemResp in response.items) {
                        if (itemResp.isFailed) {
                            log.warning(itemResp.failureMessage)
                            numFails++
                        }
                    }
                }
                count.addAndGet(bulkReq.numberOfActions() - numFails)
                fails.addAndGet(numFails)
            }

            val inserted = count.get()
            val failed = fails.get()
            val submitted = i + j - failed
            val rate = docsPerMinute(inserted, timer.elapsed().toMillis())
            val hoursRemaining = (numDocs - inserted - failed).toDouble() / rate / 60.0
            log.info(
                    String.format(
                            "Submitted %.2f%% of documents (%d/%d) (%d failed) @ %.2f docs/minute [~%.1fh remaining]",
                            100.toFloat() * submitted / numDocs,
                            submitted,
                            numDocs,
                            failed,
                            rate,
                            hoursRemaining))
            i += BATCH_SIZE
        }

        log.info(String.format("All documents submitted, awaiting remaining threads"))
        pool.shutdown()
        pool.awaitTermination(15, TimeUnit.MINUTES)
        timer.stop()

        log.info(
                String.format(
                        "Indexing complete (%d/%d) (%d failed)", numDocs - fails.get(), numDocs, fails.get()))
        val t = timer.elapsed()
        log.info(
                String.format(
                        "Indexing took %dh %dm %ds; average ~%.2f documents indexed per minute",
                        t.toHours(),
                        t.toMinutes() % 60,
                        t.seconds % 60,
                        docsPerMinute(numDocs, t.toMillis())))
    }

    private fun retryTemplate(): RetryTemplate {
        val tmpl = RetryTemplate()
        val backoff = ExponentialRandomBackOffPolicy()
        backoff.maxInterval = MAX_BACKOFF_INTERVAL_MS.toLong()
        tmpl.setBackOffPolicy(backoff)
        val retry = SimpleRetryPolicy()
        retry.maxAttempts = 20
        tmpl.setRetryPolicy(retry)
        tmpl.setThrowLastExceptionOnExhausted(true)
        return tmpl
    }

    private fun docsPerMinute(processed: Int, elapsedMillis: Long): Double {
        return 60.0 * 1000.0 * (processed.toDouble() / elapsedMillis.toDouble())
    }

    fun todayMinusYears(years: Int): LocalDate {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        return now.minusYears(years.toLong()).toLocalDate()
    }
}
