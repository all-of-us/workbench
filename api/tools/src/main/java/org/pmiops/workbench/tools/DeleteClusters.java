package org.pmiops.workbench.tools;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.pmiops.workbench.notebooks.ApiClient;
import org.pmiops.workbench.notebooks.ApiException;
import org.pmiops.workbench.notebooks.api.ClusterApi;
import org.pmiops.workbench.notebooks.model.Cluster;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * DeleteClusters is a operation utility for destroying all clusters available
 * to the current user.
 */
@SpringBootApplication
public class DeleteClusters {

  private static final Logger log = Logger.getLogger(DeleteClusters.class.getName());
  private static final String[] BILLING_SCOPES = new String[] {
      "https://www.googleapis.com/auth/userinfo.profile",
      "https://www.googleapis.com/auth/userinfo.email",
      "https://www.googleapis.com/auth/cloud-billing"
  };

  private static Set<String> commaDelimitedStringToSet(String str) {
    return Arrays.asList(str.split(",")).stream()
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toSet());
  }

  private static ApiClient newApiClient() throws IOException {
    ApiClient apiClient = new ApiClient();
    GoogleCredential credential = GoogleCredential.getApplicationDefault()
        .createScoped(Arrays.asList(BILLING_SCOPES));
    credential.refreshToken();
    apiClient.setAccessToken(credential.getAccessToken());
    return apiClient;
  }

  private static String clusterId(Cluster c) {
    return c.getGoogleProject() + "/" + c.getClusterName();
  }

  @Bean
  public CommandLineRunner run() {
    return (args) -> {
      // User-friendly command-line parsing is done in devstart.rb, so we do only simple positional
      // argument parsing here.
      if (args.length != 3) {
        throw new IllegalArgumentException(
            "Expected 3 args (min_age, ids, dry_run). Got " + Arrays.asList(args));
      }
      Duration age = Duration.ZERO;
      if (!args[0].isEmpty()) {
        age = Duration.ofDays(Long.parseLong(args[0]));
      }
      Instant oldest = Clock.systemUTC().instant().minus(age);
      log.info("only clusters created before " + oldest + " will be considered");

      // Note: IDs are optional, this set may be empty.
      Set<String> ids = commaDelimitedStringToSet(args[1]);
      Set<String> remaining = new HashSet<>(ids);
      boolean dryRun = Boolean.valueOf(args[2]);
      String dryMsg = dryRun? "[DRY RUN]: would have... " : "";

      Gson gson = new Gson();
      ClusterApi api = new ClusterApi();
      api.setApiClient(newApiClient());
      long deleted = api.listClusters(null, false).stream()
          .sorted(Comparator.comparing(c -> Instant.parse(c.getCreatedDate())))
          .filter((c) -> {
            Instant createdDate = Instant.parse(c.getCreatedDate());
            if (oldest != null && createdDate.isAfter(oldest)) {
              return false;
            }
            String cid = clusterId(c);
            if (!ids.isEmpty() && !ids.contains(cid)) {
              return false;
            }
            if (!dryRun) {
              try {
                api.deleteCluster(c.getGoogleProject(), c.getClusterName());
              } catch (ApiException e) {
                log.log(Level.SEVERE, "failed to deleted cluster " + cid, e);
                return false;
              }
            }
            JsonObject labels = gson.toJsonTree(c.getLabels()).getAsJsonObject();
            System.out.println(
                String.format("%sdeleted cluster: %-40.40s %-40.40s %-15s", dryMsg, cid,
                    labels.get("created-by").getAsString(), c.getCreatedDate()));
            remaining.remove(cid);
            return true;
          })
          .count();
      if (!remaining.isEmpty()) {
        log.log(Level.SEVERE, "failed to find/delete clusters: {1}",
            new Object[]{Joiner.on(", ").join(remaining)});
      }
      System.out.println(String.format("%sdeleted %d clusters", dryMsg, deleted));
    };
  }

  public static void main(String[] args) throws Exception {
    new SpringApplicationBuilder(DeleteClusters.class).web(false).run(args);
  }
}
