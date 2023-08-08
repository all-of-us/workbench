package org.pmiops.workbench.tools;

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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.pmiops.workbench.auth.ServiceAccounts;
import org.broadinstitute.dsde.workbench.client.leonardo.ApiClient;
import org.pmiops.workbench.leonardo.ApiException;
import org.pmiops.workbench.leonardo.ApiResponse;
import org.broadinstitute.dsde.workbench.client.leonardo.api.RuntimesApi;
import org.broadinstitute.dsde.workbench.client.leonardo.model.GetRuntimeResponse;
import org.broadinstitute.dsde.workbench.client.leonardo.model.ListRuntimeResponse;
import org.broadinstitute.dsde.workbench.client.leonardo.model.ClusterStatus;
import org.pmiops.workbench.utils.mappers.LeonardoMapper;
import org.pmiops.workbench.utils.mappers.LeonardoMapperImpl;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * ManageLeonardoRuntimes is an operational utility for interacting with the Leonardo Notebook
 * runtimes available to the application default user. This should generally be used while
 * authorized as the App Engine default service account for a given environment.
 */
@Configuration
@Import(LeonardoMapperImpl.class)
public class ManageLeonardoRuntimes {

  enum OutputFormat {
    TABULAR,
    JSON
  }

  private static final Logger log = Logger.getLogger(ManageLeonardoRuntimes.class.getName());

  private LeonardoMapper leonardoMapper;

  public ManageLeonardoRuntimes(LeonardoMapper leonardoMapper) {
    this.leonardoMapper = leonardoMapper;
  }

  private static final String[] LEO_SCOPES =
      new String[] {
        "https://www.googleapis.com/auth/userinfo.profile",
        "https://www.googleapis.com/auth/userinfo.email"
      };
  private static final List<String> DESCRIBE_ARG_NAMES =
      ImmutableList.of("api_url", "service_account", "runtime_id");
  private static final List<String> DELETE_ARG_NAMES =
      ImmutableList.of("api_url", "min_age", "ids", "dry_run");
  private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();

  private static Set<String> commaDelimitedStringToSet(String str) {
    return Arrays.stream(str.split(",")).filter(s -> !s.isEmpty()).collect(Collectors.toSet());
  }

  private static RuntimesApi newApiClient(String apiUrl) throws IOException {
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(apiUrl);
    apiClient.setAccessToken(
        ServiceAccounts.getScopedServiceAccessToken(Arrays.asList(LEO_SCOPES)));
    apiClient.setReadTimeout(60 * 1000);
    RuntimesApi api = new RuntimesApi();
    api.setApiClient(apiClient);
    return api;
  }

  private String runtimeId(ListRuntimeResponse r) {
    return leonardoMapper.toGoogleProject(r.getCloudContext()) + "/" + r.getRuntimeName();
  }

  private String formatTabular(ListRuntimeResponse r) {
    Gson gson = new Gson();
    JsonObject labels = gson.toJsonTree(r.getLabels()).getAsJsonObject();
    String creator = "unknown";
    if (labels.has("created-by")) {
      creator = labels.get("created-by").getAsString();
    }
    ClusterStatus status = ClusterStatus.UNKNOWN;
    if (r.getStatus() != null) {
      status = r.getStatus();
    }
    return String.format(
        "%-40.40s %-50.50s %-10s %-15s",
        runtimeId(r), creator, status, r.getAuditInfo().getCreatedDate());
  }

  private void printFormatted(List<ListRuntimeResponse> runtimes, OutputFormat fmt) {
    Function<ListRuntimeResponse, String> toGoogle =
        r -> leonardoMapper.toGoogleProject(r.getCloudContext());
    Stream<ListRuntimeResponse> stream =
        runtimes.stream()
            .sorted(
                Comparator.comparing(toGoogle)
                    .thenComparing(r -> r.getAuditInfo().getCreatedDate()));

    switch (fmt) {
      case JSON:
        System.out.println(PRETTY_GSON.toJson(stream.collect(Collectors.toList())));
        break;

      case TABULAR:
      default:
        stream.forEachOrdered(
            (c) -> {
              System.out.println(formatTabular(c));
            });
        System.out.println(String.format("listed %d runtimes", runtimes.size()));
        break;
    }
  }

  private void listRuntimes(
      String apiUrl, boolean includeDeleted, Optional<String> googleProjectId, OutputFormat fmt)
      throws IOException, ApiException {
    RuntimesApi api = newApiClient(apiUrl);

    List<ListRuntimeResponse> runtimes;
    if (googleProjectId.isPresent()) {
      runtimes = api.listRuntimesByProject(googleProjectId.get(), null, includeDeleted);
    } else {
      runtimes = api.listRuntimes(null, includeDeleted);
    }
    printFormatted(runtimes, fmt);
  }

  private static void describeRuntime(
      String apiUrl, String workbenchServiceAccount, String runtimeId)
      throws IOException, ApiException {
    String[] parts = runtimeId.split("/");
    if (parts.length != 2) {
      System.err.println(
          String.format(
              "given runtime ID '%s' is invalid, wanted format 'googleProject/runtimeName'",
              runtimeId));
      return;
    }
    String googleProject = parts[0];
    String runtimeName = parts[1];

    // Leo's getRuntime API swagger tends to be outdated; issue a raw getRuntime request to ensure
    // we get all available information for debugging.
    RuntimesApi client = newApiClient(apiUrl);
    com.squareup.okhttp.Call call =
        client.getRuntimeCall(
            googleProject,
            runtimeName,
            /* progressListener */ null,
            /* progressRequestListener */ null);
    ApiResponse<Object> resp = client.getApiClient().execute(call, Object.class);

    // Parse the response as well so we can log specific structured fields.
    GetRuntimeResponse runtime =
        PRETTY_GSON.fromJson(PRETTY_GSON.toJson(resp.getData()), GetRuntimeResponse.class);

    System.out.println(PRETTY_GSON.toJson(resp.getData()));
    System.out.printf("\n\nTo inspect logs in cloud storage, run the following:\n\n");

    System.out.printf(
        "    gsutil -i %s ls gs://%s/**\n",
        workbenchServiceAccount, runtime.getAsyncRuntimeFields().getStagingBucket());
    System.out.printf(
        "    gsutil -i %s cat ... # inspect or copy logs\n\n", workbenchServiceAccount);
  }

  private void deleteRuntimes(
      String apiUrl, @Nullable Instant oldest, Set<String> ids, boolean dryRun)
      throws IOException, ApiException {
    Set<String> remaining = new HashSet<>(ids);
    String dryMsg = dryRun ? "[DRY RUN]: would have... " : "";

    AtomicInteger deleted = new AtomicInteger();
    RuntimesApi api = newApiClient(apiUrl);
    api.listRuntimes(null, false).stream()
        .sorted(Comparator.comparing(ListRuntimeResponse::getRuntimeName))
        .filter(
            (r) -> {
              Instant createdDate = Instant.parse(r.getAuditInfo().getCreatedDate());
              if (oldest != null && createdDate.isAfter(oldest)) {
                return false;
              }
              if (!ids.isEmpty() && !ids.contains(runtimeId(r))) {
                return false;
              }
              return true;
            })
        .forEachOrdered(
            (r) -> {
              String cid = runtimeId(r);
              if (!dryRun) {
                try {
                  api.deleteRuntime(
                      leonardoMapper.toGoogleProject(r.getCloudContext()),
                      r.getRuntimeName(), /* deleteDisk */
                      false);
                } catch (org.broadinstitute.dsde.workbench.client.leonardo.ApiException e) {
                  log.log(Level.SEVERE, "failed to deleted runtime " + cid, e);
                  return;
                }
              }
              remaining.remove(cid);
              deleted.getAndIncrement();
              System.out.println(dryMsg + "deleted runtime: " + formatTabular(r));
            });
    if (!remaining.isEmpty()) {
      log.log(
          Level.SEVERE,
          "failed to find/delete runtimes: {1}",
          new Object[] {Joiner.on(", ").join(remaining)});
    }
    System.out.println(String.format("%sdeleted %d runtimes", dryMsg, deleted.get()));
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
          describeRuntime(args[0], args[1], args[2]);
          return;

        case "list":
          if (args.length != 4) {
            throw new IllegalArgumentException("Expected 4 args. Got " + Arrays.asList(args));
          }
          listRuntimes(
              args[0],
              Boolean.parseBoolean(args[1]),
              Optional.of(args[2]).filter(p -> !p.isEmpty()),
              OutputFormat.valueOf(args[3]));
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
            log.info("only runtimes created before " + oldest + " will be considered");
          }

          // Note: IDs are optional, this set may be empty.
          Set<String> ids = commaDelimitedStringToSet(args[2]);
          boolean dryRun = Boolean.parseBoolean(args[3]);

          if (oldest == null && ids.isEmpty()) {
            throw new IllegalArgumentException(
                "must provide either a maximum age, or a list of ids for filtering of runtimes");
          }
          deleteRuntimes(apiUrl, oldest, ids, dryRun);
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
    new SpringApplicationBuilder(ManageLeonardoRuntimes.class)
        .web(WebApplicationType.NONE)
        .run(args)
        .close();
  }
}
