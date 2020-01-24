package org.pmiops.workbench.tools;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.pmiops.workbench.notebooks.ApiClient;
import org.pmiops.workbench.notebooks.ApiException;
import org.pmiops.workbench.notebooks.ApiResponse;
import org.pmiops.workbench.notebooks.api.ClusterApi;
import org.pmiops.workbench.notebooks.model.Cluster;
import org.pmiops.workbench.notebooks.model.ClusterStatus;
import org.pmiops.workbench.notebooks.model.ListClusterResponse;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ManageClusters is an operational utility for interacting with the Leonardo Notebook clusters
 * available to the application default user. This should generally be used while authorized as the
 * App Engine default service account for a given environment.
 */
@Configuration
public class ManageClusters {

  private static final Logger log = Logger.getLogger(ManageClusters.class.getName());
  private static final String[] BILLING_SCOPES =
      new String[] {
        "https://www.googleapis.com/auth/userinfo.profile",
        "https://www.googleapis.com/auth/userinfo.email"
      };
  private static final List<String> DESCRIBE_ARG_NAMES =
      ImmutableList.of("api_url", "project", "service_account", "cluster_id");
  private static final List<String> DELETE_ARG_NAMES =
      ImmutableList.of("api_url", "min_age", "ids", "dry_run");
  private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();

  private static Set<String> commaDelimitedStringToSet(String str) {
    return Arrays.asList(str.split(",")).stream()
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toSet());
  }

  private static ClusterApi newApiClient(String apiUrl) throws IOException {
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(apiUrl);
    GoogleCredential credential =
        GoogleCredential.getApplicationDefault().createScoped(Arrays.asList(BILLING_SCOPES));
    credential.refreshToken();
    apiClient.setAccessToken(credential.getAccessToken());
    ClusterApi api = new ClusterApi();
    api.setApiClient(apiClient);
    return api;
  }

  private static String clusterId(ListClusterResponse c) {
    return c.getGoogleProject() + "/" + c.getClusterName();
  }

  private static String formatTabular(ListClusterResponse c) {
    Gson gson = new Gson();
    JsonObject labels = gson.toJsonTree(c.getLabels()).getAsJsonObject();
    String creator = "unknown";
    if (labels.has("created-by")) {
      creator = labels.get("created-by").getAsString();
    }
    ClusterStatus status = ClusterStatus.UNKNOWN;
    if (c.getStatus() != null) {
      status = c.getStatus();
    }
    return String.format(
        "%-40.40s %-50.50s %-10s %-15s", clusterId(c), creator, status, c.getCreatedDate());
  }

  private static void listClusters(String apiUrl) throws IOException, ApiException {
    AtomicInteger count = new AtomicInteger();
    newApiClient(apiUrl).listClusters(null, false).stream()
        .sorted(Comparator.comparing(c -> Instant.parse(c.getCreatedDate())))
        .forEachOrdered(
            (c) -> {
              System.out.println(formatTabular(c));
              count.getAndIncrement();
            });
    System.out.println(String.format("listed %d clusters", count.get()));
  }

  private static void describeCluster(
      String apiUrl, String workbenchProjectId, String workbenchServiceAccount, String clusterId)
      throws IOException, ApiException {
    String[] parts = clusterId.split("/");
    if (parts.length != 2) {
      System.err.println(
          String.format(
              "given cluster ID '%s' is invalid, wanted format 'project/clusterName'", clusterId));
      return;
    }
    String clusterProject = parts[0];
    String clusterName = parts[1];

    // Leo's getCluster API swagger tends to be outdated; issue a raw getCluster request to ensure
    // we get all available information for debugging.
    ClusterApi client = newApiClient(apiUrl);
    com.squareup.okhttp.Call call =
        client.getClusterCall(
            clusterProject,
            clusterName,
            /* progressListener */ null,
            /* progressRequestListener */ null);
    ApiResponse<Object> resp = client.getApiClient().execute(call, Object.class);

    // Parse the response as well so we can log specific structured fields.
    Cluster cluster = PRETTY_GSON.fromJson(PRETTY_GSON.toJson(resp.getData()), Cluster.class);

    System.out.println(PRETTY_GSON.toJson(resp.getData()));
    System.out.printf("\n\nTo inspect logs in cloud storage, run the following:\n\n");

    // TODO(PD-4740): Use impersonation here instead.
    String keyPath = String.format("/tmp/%s-key.json", workbenchProjectId);
    System.out.printf(
        "    gcloud iam service-accounts keys create %s --iam-account %s\n",
        keyPath, workbenchServiceAccount);
    System.out.printf("    gcloud auth activate-service-account --key-file %s\n\n", keyPath);
    System.out.printf("    gsutil ls gs://%s\n\n", cluster.getStagingBucket());
    System.out.printf("    # Delete the key when done\n");
    System.out.printf(
        "    gcloud iam service-accounts keys delete $(jq -r .private_key_id %s) --iam-account %s\n\n",
        keyPath, workbenchServiceAccount);
  }

  private static void deleteClusters(
      String apiUrl, @Nullable Instant oldest, Set<String> ids, boolean dryRun)
      throws IOException, ApiException {
    Set<String> remaining = new HashSet<>(ids);
    String dryMsg = dryRun ? "[DRY RUN]: would have... " : "";

    AtomicInteger deleted = new AtomicInteger();
    ClusterApi api = newApiClient(apiUrl);
    api.listClusters(null, false).stream()
        .sorted(Comparator.comparing(c -> Instant.parse(c.getCreatedDate())))
        .filter(
            (c) -> {
              Instant createdDate = Instant.parse(c.getCreatedDate());
              if (oldest != null && createdDate.isAfter(oldest)) {
                return false;
              }
              if (!ids.isEmpty() && !ids.contains(clusterId(c))) {
                return false;
              }
              return true;
            })
        .forEachOrdered(
            (c) -> {
              String cid = clusterId(c);
              if (!dryRun) {
                try {
                  api.deleteCluster(c.getGoogleProject(), c.getClusterName());
                } catch (ApiException e) {
                  log.log(Level.SEVERE, "failed to deleted cluster " + cid, e);
                  return;
                }
              }
              remaining.remove(cid);
              deleted.getAndIncrement();
              System.out.println(dryMsg + "deleted cluster: " + formatTabular(c));
            });
    if (!remaining.isEmpty()) {
      log.log(
          Level.SEVERE,
          "failed to find/delete clusters: {1}",
          new Object[] {Joiner.on(", ").join(remaining)});
    }
    System.out.println(String.format("%sdeleted %d clusters", dryMsg, deleted.get()));
  }

  @Bean
  public CommandLineRunner run() {
    return (args) -> {
      if (args.length < 1) {
        throw new IllegalArgumentException("must specify a command 'list', 'describe' or 'delete'");
      }
      String cmd = args[0];
      args = Arrays.copyOfRange(args, 1, args.length);
      switch (cmd) {
        case "describe":
          if (args.length != DESCRIBE_ARG_NAMES.size()) {
            throw new IllegalArgumentException(
                String.format(
                    "Expected %d args %s. Got: %s",
                    DESCRIBE_ARG_NAMES.size(), DESCRIBE_ARG_NAMES, Arrays.asList(args)));
          }
          describeCluster(args[0], args[1], args[2], args[3]);
          return;

        case "list":
          if (args.length != 1) {
            throw new IllegalArgumentException("Expected 1 arg. Got " + Arrays.asList(args));
          }
          listClusters(args[0]);
          return;

        case "delete":
          // User-friendly command-line parsing is done in devstart.rb, so we do only simple
          // positional argument parsing here.
          if (args.length != DELETE_ARG_NAMES.size()) {
            throw new IllegalArgumentException(
                String.format(
                    "Expected %d args %s. Got: %s",
                    DELETE_ARG_NAMES.size(), DELETE_ARG_NAMES, Arrays.asList(args)));
          }
          String apiUrl = args[0];
          Instant oldest = null;
          if (!args[1].isEmpty()) {
            Duration age = Duration.ofDays(Long.parseLong(args[1]));
            oldest = Clock.systemUTC().instant().minus(age);
            log.info("only clusters created before " + oldest + " will be considered");
          }

          // Note: IDs are optional, this set may be empty.
          Set<String> ids = commaDelimitedStringToSet(args[2]);
          boolean dryRun = Boolean.valueOf(args[3]);
          deleteClusters(apiUrl, oldest, ids, dryRun);
          return;

        default:
          throw new IllegalArgumentException(
              String.format("unrecognized command '%s', want 'list' or 'delete'", cmd));
      }
    };
  }

  public static void main(String[] args) throws Exception {
    // This tool doesn't currently need database access, so it doesn't extend the
    // CommandLineToolConfig. To add database access, extend from that config and update project.rb
    // to ensure a Cloud SQL proxy is available when this command is run.
    new SpringApplicationBuilder(ManageClusters.class).web(false).run(args);
  }
}
