package org.pmiops.workbench.tools.elastic;

import com.google.cloud.RetryOption;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.ExtractJobConfiguration;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.TableResult;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.BlobListOption;
import com.google.cloud.storage.StorageOptions;
import com.google.common.base.Preconditions;
import com.google.common.io.Resources;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Spliterators;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsRequest;
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Settings;
import org.pmiops.workbench.elasticsearch.ElasticDocument;
import org.pmiops.workbench.elasticsearch.ElasticUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.threeten.bp.Duration;

/**
 * Command-line tool for indexing an OMOP BigQuery dataset into Elasticsearch. Executes the
 * following pipeline:
 *
 * <p>- Create (and optionally replace) an existing index for this CDR. - Query BigQuery to produce
 * JSON documents: the output of the query is the exact format we define for Elasticsearch (using a
 * repeated STRUCT for repeated nested documents). - [Optional] Export these results to a temporary
 * table, and export that to GCS. Slower, but necessary for larger datasets (e.g. the entire 1m
 * participant Synthetic CDR). - Bulk ingest the JSON documents into Elasticsearch (either inserts
 * or updates).
 *
 * <p>This indexer can optionally be run on a subsample of participants in the CDR for faster test
 * iterations and local development. The full index will be 10-100GB, so it is not advisable to run
 * this locally against the entire synthetic CDR.
 */
public final class ElasticSearchIndexer {

  private static Option queryProjectIdOpt =
      Option.builder()
          .longOpt("query-project-id")
          .desc(
              "GCP Cloud Project ID to run the extraction query in. Does not necessarily need "
                  + "to be the same as the CDR dataset's BigQuery project.")
          .required()
          .hasArg()
          .build();
  private static Option esBaseUrlOpt =
      Option.builder()
          .longOpt("es-base-url")
          .desc("Elasticsearch base API URL, for loading data into")
          .required()
          .hasArg()
          .build();
  private static Option esAuthProjectOpt =
      Option.builder()
          .longOpt("es-auth-project")
          .desc(
              "If specified, basic authentication is used for the given GCP project configuration;"
                  + "required for Elastic Cloud access")
          .hasArg()
          .build();
  private static Option cdrVersionOpt =
      Option.builder()
          .longOpt("cdr-version")
          .desc(
              "The CDR version identifier, e.g. 'synth_r_2018q1_1', see "
                  + "https://docs.google.com/document/d/1W8DnEN7FnnPgGW6yrvGsdzLZhQrdOtTjvgdFUL6e4oc/edit")
          .required()
          .hasArg()
          .build();
  private static Option cdrBigQueryDatasetOpt =
      Option.builder()
          .longOpt("cdr-big-query-dataset")
          .desc(
              "CDR BigQuery dataset ID (including the project), e.g. "
                  + "'all-of-us-ehr-dev.synthetic_cdr20180606'")
          .required()
          .hasArg()
          .build();
  private static Option scratchBigQueryDatasetOpt =
      Option.builder()
          .longOpt("scratch-big-query-dataset")
          .desc(
              "BigQuery dataset ID (including the project), for intermediate scratch exports e.g. "
                  + "'all-of-us-ehr-dev.elastic-scratch'")
          .hasArg()
          .build();
  private static Option scratchGcsOpt =
      Option.builder()
          .longOpt("scratch-gcs-bucket")
          .desc(
              "GCS bucket for intermediate JSON exports (without a gs://), e.g. "
                  + "'all-of-us-workbench-test-elastic-exports'")
          .hasArg()
          .build();
  private static Option inverseProbOpt =
      Option.builder()
          .longOpt("participant-inclusion-inverse-prob")
          .desc(
              "The inverse probability to index a participant, used to index a "
                  + "sample of participants. For example, 1000 would index ~1/1000 of participants in the "
                  + "target dataset. Defaults to 1 (all participants).")
          .type(Integer.class)
          .hasArg()
          .build();
  private static Option deleteIndicesOpt =
      Option.builder()
          .longOpt("delete-indices")
          .desc("If specified, deletes existing indices of the same name before re-importing")
          .build();
  private static Options createOptions =
      new Options()
          .addOption(queryProjectIdOpt)
          .addOption(esBaseUrlOpt)
          .addOption(esAuthProjectOpt)
          .addOption(cdrVersionOpt)
          .addOption(cdrBigQueryDatasetOpt)
          .addOption(scratchBigQueryDatasetOpt)
          .addOption(scratchGcsOpt)
          .addOption(inverseProbOpt)
          .addOption(deleteIndicesOpt);

  private static final Logger log = Logger.getLogger(ElasticSearchIndexer.class.getName());

  private RestHighLevelClient client;

  private void createIndex(CommandLine opts) throws IOException, InterruptedException {
    Preconditions.checkArgument(
        !(opts.hasOption(scratchGcsOpt.getLongOpt())
            ^ opts.hasOption(scratchBigQueryDatasetOpt.getLongOpt())),
        "Must provide both or provide neither of scratch BigQuery and scratch GCS locations");
    int inverseProb = 1;
    if (opts.hasOption(inverseProbOpt.getLongOpt())) {
      inverseProb = Integer.parseInt(opts.getOptionValue(inverseProbOpt.getLongOpt()));
    }

    String personIndex = ElasticUtils.personIndexName(opts.getOptionValue("cdr-version"));
    try {
      ElasticUtils.createPersonIndex(
          client, personIndex, opts.hasOption(deleteIndicesOpt.getLongOpt()));
    } catch (ElasticsearchStatusException e) {
      if (e.status().getStatus() != 400) {
        throw e;
      }
      log.warning(
          "400 status on index creation, assuming conflict and ignoring: " + e.getMessage());
    }

    // Temporarily disable index refreshes and replication to speed up the ingestion process, per
    // https://www.elastic.co/guide/en/elasticsearch/reference/master/tune-for-indexing-speed.html
    Settings originalSettings =
        client
            .indices()
            .getSettings(new GetSettingsRequest().indices(personIndex), ElasticUtils.REQ_OPTS)
            .getIndexToSettings()
            .get(personIndex);
    client
        .indices()
        .putSettings(
            new UpdateSettingsRequest(personIndex)
                .settings(
                    Settings.builder()
                        .put("index.refresh_interval", -1)
                        .put("index.number_of_replicas", 0)),
            ElasticUtils.REQ_OPTS);

    // Generate JSON documents from the CDR for Elasticsearch.
    // Note: Here, BigQuery uses default credentials for the given environment.
    BigQuery bq =
        BigQueryOptions.newBuilder()
            .setProjectId(opts.getOptionValue(queryProjectIdOpt.getLongOpt()))
            .build()
            .getService();
    String cdrDataset = opts.getOptionValue(cdrBigQueryDatasetOpt.getLongOpt());
    String personSQL = getPersonBigQuerySQL(cdrDataset, inverseProb);

    int totalSampleSize;
    Iterator<ElasticDocument> docs;
    if (opts.hasOption(scratchGcsOpt.getLongOpt())) {
      // Export to BigQuery -> GCS newline-delimited JSON before ingest. Slower, but more reliable.
      // Required for bigger data, e.g. 100K/1m participant datasets.
      String[] parts = opts.getOptionValue(scratchBigQueryDatasetOpt.getLongOpt()).split("\\.");
      String scratchProject = parts[0];
      String scratchDataset = parts[1];
      String scratchTable = String.format("%s_%d", personIndex, Instant.now().toEpochMilli());
      TableId scratchTableId = TableId.of(scratchProject, scratchDataset, scratchTable);

      log.info(
          String.format(
              "Exporting from CDR '%s' -> intermediate table '%s'", cdrDataset, scratchTableId));
      Job job =
          bq.create(
                  Job.of(
                      QueryJobConfiguration.newBuilder(personSQL)
                          .setDestinationTable(scratchTableId)
                          .build()))
              .waitFor(RetryOption.totalTimeout(Duration.ofHours(1)));
      if (job.getStatus().getError() != null) {
        throw new IOException("BigQuery job failed: " + job.getStatus().getError().getMessage());
      }
      totalSampleSize = (int) job.getQueryResults().getTotalRows();

      // TODO: Ensure expiry and delete after successful ingest.
      String bucket = opts.getOptionValue(scratchGcsOpt.getLongOpt());
      String gcsDir = scratchTable;

      // * is substituted with a numeric shard value; this only matters if the results are large
      // enough to be sharded by BigQuery on export.
      String gcsExportPath = String.format("gs://%s/%s/person-*.json", bucket, gcsDir);
      log.info(
          String.format(
              "Exporting from intermediate table '%s' -> intermediate GCS file(s) '%s'",
              scratchTableId, gcsExportPath));
      job =
          bq.create(
                  Job.of(
                      ExtractJobConfiguration.of(
                          scratchTableId, gcsExportPath, "NEWLINE_DELIMITED_JSON")))
              .waitFor(RetryOption.totalTimeout(Duration.ofHours(1)));
      if (job.getStatus().getError() != null) {
        throw new IOException("BigQuery job failed: " + job.getStatus().getError().getMessage());
      }

      log.info(
          String.format(
              "Converting intermediate GCS JSON file(s) '%s' -> Elasticsearch documents",
              gcsExportPath));
      Storage storage = StorageOptions.getDefaultInstance().getService();
      Iterator<String> lineIter =
          new CloudStorageShardedLineIterator(
              storage.list(bucket, BlobListOption.prefix(gcsDir + "/")).iterateAll());
      docs =
          StreamSupport.stream(Spliterators.spliteratorUnknownSize(lineIter, 0), false)
              .map(
                  line -> {
                    try {
                      return ElasticDocument.fromBigQueryJson(line);
                    } catch (IOException e) {
                      throw new RuntimeException("Error reading JSON line from BigQuery export", e);
                    }
                  })
              .iterator();
    } else {
      log.info(
          String.format("SELECTing from CDR '%s' -> Elasticsearch JSON documents", cdrDataset));
      TableResult res = bq.query(QueryJobConfiguration.newBuilder(personSQL).build());
      totalSampleSize = (int) res.getTotalRows();
      docs =
          StreamSupport.stream(res.iterateAll().spliterator(), false)
              .map(ElasticDocument::fromBigQueryResults)
              .iterator();
    }

    log.info(
        String.format(
            "Starting bulk ingest of Elasticsearch documents to '%s'",
            opts.getOptionValue(esBaseUrlOpt.getLongOpt())));
    ElasticUtils.ingestDocuments(client, personIndex, docs, totalSampleSize);

    // Restore the original index settings.
    client
        .indices()
        .putSettings(
            new UpdateSettingsRequest(personIndex)
                .settings(
                    Settings.builder()
                        .put(
                            "index.refresh_interval",
                            originalSettings.get("index.refresh_interval", null))
                        .put(
                            "index.number_of_replicas",
                            originalSettings.get("index.number_of_replicas", null))),
            ElasticUtils.REQ_OPTS);
  }

  private String getPersonBigQuerySQL(String bqDataset, int inverseProb) throws IOException {
    // Synth dataset doesn't contain sex_at_birth, so we use gender as a fix
    String resourceName =
        bqDataset.contains("synthetic") ? "bigquery/es_synth_person.sql" : "bigquery/es_person.sql";
    String esPersonBQTemplate =
        Resources.toString(Resources.getResource(resourceName), Charset.defaultCharset());
    return esPersonBQTemplate
        .replaceAll("\\{BQ_DATASET\\}", bqDataset)
        .replaceAll("\\{PERSON_ID_MOD\\}", Integer.toString(inverseProb));
  }

  private static RestHighLevelClient newClient(String esBaseUrl, @Nullable String esAuthProject)
      throws MalformedURLException {
    URL url = new URL(esBaseUrl);
    RestClientBuilder b =
        RestClient.builder(new HttpHost(url.getHost(), url.getPort(), url.getProtocol()));
    if (!Strings.isNullOrEmpty(esAuthProject)) {
      Storage gcs = StorageOptions.getDefaultInstance().getService();
      JsonObject creds =
          new JsonParser()
              .parse(
                  new String(
                      gcs.get(esAuthProject + "-credentials", "elastic-cloud.json").getContent()))
              .getAsJsonObject();

      CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
      credentialsProvider.setCredentials(
          AuthScope.ANY,
          new UsernamePasswordCredentials(
              creds.get("username").getAsString(), creds.get("password").getAsString()));
      b.setHttpClientConfigCallback(
          (httpClientBuilder) ->
              httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
    }
    return new RestHighLevelClient(b);
  }

  @Bean
  public CommandLineRunner run() {
    return (args) -> {
      if (args.length < 1) {
        throw new IllegalArgumentException("must specify a command 'create'");
      }
      String cmd = args[0];
      args = Arrays.copyOfRange(args, 1, args.length);
      try {
        switch (cmd) {
          case "create":
            CommandLine opts = new DefaultParser().parse(createOptions, args);
            client =
                newClient(
                    opts.getOptionValue(esBaseUrlOpt.getLongOpt()),
                    opts.getOptionValue(esAuthProjectOpt.getLongOpt()));
            createIndex(opts);
            return;

          default:
            throw new IllegalArgumentException(
                String.format("unrecognized command '%s', want 'create'", cmd));
        }
      } finally {
        if (client != null) {
          client.close();
        }
      }
    };
  }

  public static void main(String[] args) throws Exception {
    new SpringApplicationBuilder(ElasticSearchIndexer.class).web(false).run(args);
  }
}
