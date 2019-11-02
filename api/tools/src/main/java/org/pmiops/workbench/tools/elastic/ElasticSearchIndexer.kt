package org.pmiops.workbench.tools.elastic

import com.google.cloud.RetryOption
import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.BigQueryOptions
import com.google.cloud.bigquery.ExtractJobConfiguration
import com.google.cloud.bigquery.Job
import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.TableId
import com.google.cloud.bigquery.TableResult
import com.google.cloud.storage.Storage
import com.google.cloud.storage.Storage.BlobListOption
import com.google.cloud.storage.StorageOptions
import com.google.common.base.Preconditions
import com.google.common.io.Resources
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.nio.charset.Charset
import java.time.Instant
import java.util.Arrays
import java.util.Spliterators
import java.util.logging.Logger
import java.util.stream.StreamSupport
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options
import org.apache.http.HttpHost
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.CredentialsProvider
import org.apache.http.impl.client.BasicCredentialsProvider
import org.elasticsearch.ElasticsearchStatusException
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsRequest
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestClientBuilder
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.common.Strings
import org.elasticsearch.common.settings.Settings
import org.pmiops.workbench.elasticsearch.ElasticDocument
import org.pmiops.workbench.elasticsearch.ElasticUtils
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.annotation.Bean
import org.threeten.bp.Duration

/**
 * Command-line tool for indexing an OMOP BigQuery dataset into Elasticsearch. Executes the
 * following pipeline:
 *
 *
 * - Create (and optionally replace) an existing index for this CDR. - Query BigQuery to produce
 * JSON documents: the output of the query is the exact format we define for Elasticsearch (using a
 * repeated STRUCT for repeated nested documents). - [Optional] Export these results to a temporary
 * table, and export that to GCS. Slower, but necessary for larger datasets (e.g. the entire 1m
 * participant Synthetic CDR). - Bulk ingest the JSON documents into Elasticsearch (either inserts
 * or updates).
 *
 *
 * This indexer can optionally be run on a subsample of participants in the CDR for faster test
 * iterations and local development. The full index will be 10-100GB, so it is not advisable to run
 * this locally against the entire synthetic CDR.
 */
class ElasticSearchIndexer {

    private var client: RestHighLevelClient? = null

    @Throws(IOException::class, InterruptedException::class)
    private fun createIndex(opts: CommandLine) {
        Preconditions.checkArgument(
                !(opts.hasOption(scratchGcsOpt.longOpt) xor opts.hasOption(scratchBigQueryDatasetOpt.longOpt)),
                "Must provide both or provide neither of scratch BigQuery and scratch GCS locations")
        var inverseProb = 1
        if (opts.hasOption(inverseProbOpt.longOpt)) {
            inverseProb = Integer.parseInt(opts.getOptionValue(inverseProbOpt.longOpt))
        }

        val personIndex = ElasticUtils.personIndexName(opts.getOptionValue("cdr-version"))
        try {
            ElasticUtils.createPersonIndex(
                    client, personIndex, opts.hasOption(deleteIndicesOpt.longOpt))
        } catch (e: ElasticsearchStatusException) {
            if (e.status().status != 400) {
                throw e
            }
            log.warning(
                    "400 status on index creation, assuming conflict and ignoring: " + e.message)
        }

        // Temporarily disable index refreshes and replication to speed up the ingestion process, per
        // https://www.elastic.co/guide/en/elasticsearch/reference/master/tune-for-indexing-speed.html
        val originalSettings = client!!
                .indices()
                .getSettings(GetSettingsRequest().indices(personIndex), ElasticUtils.REQ_OPTS)
                .indexToSettings
                .get(personIndex)
        client!!
                .indices()
                .putSettings(
                        UpdateSettingsRequest(personIndex)
                                .settings(
                                        Settings.builder()
                                                .put("index.refresh_interval", -1)
                                                .put("index.number_of_replicas", 0)),
                        ElasticUtils.REQ_OPTS)

        // Generate JSON documents from the CDR for Elasticsearch.
        // Note: Here, BigQuery uses default credentials for the given environment.
        val bq = BigQueryOptions.newBuilder()
                .setProjectId(opts.getOptionValue(queryProjectIdOpt.longOpt))
                .build()
                .service
        val cdrDataset = opts.getOptionValue(cdrBigQueryDatasetOpt.longOpt)
        val personSQL = getPersonBigQuerySQL(cdrDataset, inverseProb)

        val totalSampleSize: Int
        val docs: Iterator<ElasticDocument>
        if (opts.hasOption(scratchGcsOpt.longOpt)) {
            // Export to BigQuery -> GCS newline-delimited JSON before ingest. Slower, but more reliable.
            // Required for bigger data, e.g. 100K/1m participant datasets.
            val parts = opts.getOptionValue(scratchBigQueryDatasetOpt.longOpt).split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val scratchProject = parts[0]
            val scratchDataset = parts[1]
            val scratchTable = String.format("%s_%d", personIndex, Instant.now().toEpochMilli())
            val scratchTableId = TableId.of(scratchProject, scratchDataset, scratchTable)

            log.info(
                    String.format(
                            "Exporting from CDR '%s' -> intermediate table '%s'", cdrDataset, scratchTableId))
            var job = bq.create(
                    Job.of(
                            QueryJobConfiguration.newBuilder(personSQL)
                                    .setDestinationTable(scratchTableId)
                                    .build()))
                    .waitFor(RetryOption.totalTimeout(Duration.ofHours(1)))
            if (job.status.error != null) {
                throw IOException("BigQuery job failed: " + job.status.error!!.message)
            }
            totalSampleSize = job.getQueryResults().totalRows.toInt()

            // TODO: Ensure expiry and delete after successful ingest.
            val bucket = opts.getOptionValue(scratchGcsOpt.longOpt)

            // * is substituted with a numeric shard value; this only matters if the results are large
            // enough to be sharded by BigQuery on export.
            val gcsExportPath = String.format("gs://%s/%s/person-*.json", bucket, scratchTable)
            log.info(
                    String.format(
                            "Exporting from intermediate table '%s' -> intermediate GCS file(s) '%s'",
                            scratchTableId, gcsExportPath))
            job = bq.create(
                    Job.of(
                            ExtractJobConfiguration.of(
                                    scratchTableId, gcsExportPath, "NEWLINE_DELIMITED_JSON")))
                    .waitFor(RetryOption.totalTimeout(Duration.ofHours(1)))
            if (job.status.error != null) {
                throw IOException("BigQuery job failed: " + job.status.error!!.message)
            }

            log.info(
                    String.format(
                            "Converting intermediate GCS JSON file(s) '%s' -> Elasticsearch documents",
                            gcsExportPath))
            val storage = StorageOptions.getDefaultInstance().service
            val lineIter = CloudStorageShardedLineIterator(
                    storage.list(bucket, BlobListOption.prefix("$scratchTable/")).iterateAll())
            docs = StreamSupport.stream(Spliterators.spliteratorUnknownSize(lineIter, 0), false)
                    .map<ElasticDocument> { line ->
                        try {
                            return@StreamSupport.stream(Spliterators.spliteratorUnknownSize(lineIter, 0), false)
                                    .map ElasticDocument . fromBigQueryJson line
                        } catch (e: IOException) {
                            throw RuntimeException("Error reading JSON line from BigQuery export", e)
                        }
                    }
                    .iterator()
        } else {
            log.info(
                    String.format("SELECTing from CDR '%s' -> Elasticsearch JSON documents", cdrDataset))
            val res = bq.query(QueryJobConfiguration.newBuilder(personSQL).build())
            totalSampleSize = res.totalRows.toInt()
            docs = StreamSupport.stream<FieldValueList>(res.iterateAll().spliterator(), false)
                    .map<ElasticDocument>(Function<FieldValueList, ElasticDocument> { ElasticDocument.fromBigQueryResults(it) })
                    .iterator()
        }

        log.info(
                String.format(
                        "Starting bulk ingest of Elasticsearch documents to '%s'",
                        opts.getOptionValue(esBaseUrlOpt.longOpt)))
        ElasticUtils.ingestDocuments(client, personIndex, docs, totalSampleSize)

        // Restore the original index settings.
        client!!
                .indices()
                .putSettings(
                        UpdateSettingsRequest(personIndex)
                                .settings(
                                        Settings.builder()
                                                .put(
                                                        "index.refresh_interval",
                                                        originalSettings.get("index.refresh_interval", null))
                                                .put(
                                                        "index.number_of_replicas",
                                                        originalSettings.get("index.number_of_replicas", null))),
                        ElasticUtils.REQ_OPTS)
    }

    @Throws(IOException::class)
    private fun getPersonBigQuerySQL(bqDataset: String, inverseProb: Int): String {
        val esPersonBQTemplate = Resources.toString(
                Resources.getResource("bigquery/es_person.sql"), Charset.defaultCharset())
        return esPersonBQTemplate
                .replace("\\{BQ_DATASET\\}".toRegex(), bqDataset)
                .replace("\\{PERSON_ID_MOD\\}".toRegex(), Integer.toString(inverseProb))
    }

    @Bean
    fun run(): CommandLineRunner {
        return { args ->
            require(args.size >= 1) { "must specify a command 'create'" }
            val cmd = args[0]
            args = Arrays.copyOfRange<String>(args, 1, args.size)
            try {
                when (cmd) {
                    "create" -> {
                        val opts = DefaultParser().parse(createOptions, args)
                        client = newClient(
                                opts.getOptionValue(esBaseUrlOpt.longOpt),
                                opts.getOptionValue(esAuthProjectOpt.longOpt))
                        createIndex(opts)
                        return
                    }

                    else -> throw IllegalArgumentException(
                            String.format("unrecognized command '%s', want 'create'", cmd))
                }
            } finally {
                if (client != null) {
                    client!!.close()
                }
            }
        }
    }

    companion object {

        private val queryProjectIdOpt = Option.builder()
                .longOpt("query-project-id")
                .desc(
                        "GCP Cloud Project ID to run the extraction query in. Does not necessarily need " + "to be the same as the CDR dataset's BigQuery project.")
                .required()
                .hasArg()
                .build()
        private val esBaseUrlOpt = Option.builder()
                .longOpt("es-base-url")
                .desc("Elasticsearch base API URL, for loading data into")
                .required()
                .hasArg()
                .build()
        private val esAuthProjectOpt = Option.builder()
                .longOpt("es-auth-project")
                .desc(
                        "If specified, basic authentication is used for the given GCP project configuration;" + "required for Elastic Cloud access")
                .hasArg()
                .build()
        private val cdrVersionOpt = Option.builder()
                .longOpt("cdr-version")
                .desc(
                        "The CDR version identifier, e.g. 'synth_r_2018q1_1', see " + "https://docs.google.com/document/d/1W8DnEN7FnnPgGW6yrvGsdzLZhQrdOtTjvgdFUL6e4oc/edit")
                .required()
                .hasArg()
                .build()
        private val cdrBigQueryDatasetOpt = Option.builder()
                .longOpt("cdr-big-query-dataset")
                .desc(
                        "CDR BigQuery dataset ID (including the project), e.g. " + "'all-of-us-ehr-dev.synthetic_cdr20180606'")
                .required()
                .hasArg()
                .build()
        private val scratchBigQueryDatasetOpt = Option.builder()
                .longOpt("scratch-big-query-dataset")
                .desc(
                        "BigQuery dataset ID (including the project), for intermediate scratch exports e.g. " + "'all-of-us-ehr-dev.elastic-scratch'")
                .hasArg()
                .build()
        private val scratchGcsOpt = Option.builder()
                .longOpt("scratch-gcs-bucket")
                .desc(
                        "GCS bucket for intermediate JSON exports (without a gs://), e.g. " + "'all-of-us-workbench-test-elastic-exports'")
                .hasArg()
                .build()
        private val inverseProbOpt = Option.builder()
                .longOpt("participant-inclusion-inverse-prob")
                .desc(
                        "The inverse probability to index a participant, used to index a "
                                + "sample of participants. For example, 1000 would index ~1/1000 of participants in the "
                                + "target dataset. Defaults to 1 (all participants).")
                .type(Int::class.java)
                .hasArg()
                .build()
        private val deleteIndicesOpt = Option.builder()
                .longOpt("delete-indices")
                .desc("If specified, deletes existing indices of the same name before re-importing")
                .build()
        private val createOptions = Options()
                .addOption(queryProjectIdOpt)
                .addOption(esBaseUrlOpt)
                .addOption(esAuthProjectOpt)
                .addOption(cdrVersionOpt)
                .addOption(cdrBigQueryDatasetOpt)
                .addOption(scratchBigQueryDatasetOpt)
                .addOption(scratchGcsOpt)
                .addOption(inverseProbOpt)
                .addOption(deleteIndicesOpt)

        private val log = Logger.getLogger(ElasticSearchIndexer::class.java.name)

        @Throws(MalformedURLException::class)
        private fun newClient(esBaseUrl: String, esAuthProject: String?): RestHighLevelClient {
            val url = URL(esBaseUrl)
            val b = RestClient.builder(HttpHost(url.host, url.port, url.protocol))
            if (!Strings.isNullOrEmpty(esAuthProject)) {
                val gcs = StorageOptions.getDefaultInstance().service
                val creds = JsonParser()
                        .parse(
                                String(
                                        gcs.get(esAuthProject!! + "-credentials", "elastic-cloud.json").getContent()))
                        .asJsonObject

                val credentialsProvider = BasicCredentialsProvider()
                credentialsProvider.setCredentials(
                        AuthScope.ANY,
                        UsernamePasswordCredentials(
                                creds.get("username").asString, creds.get("password").asString))
                b.setHttpClientConfigCallback { httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider) }
            }
            return RestHighLevelClient(b)
        }

        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplicationBuilder(ElasticSearchIndexer::class.java).web(false).run(*args)
        }
    }
}
