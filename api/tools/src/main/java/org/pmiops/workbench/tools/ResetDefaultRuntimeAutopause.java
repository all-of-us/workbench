package org.pmiops.workbench.tools;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.auth.oauth2.OAuth2Credentials;
import com.google.cloud.iam.credentials.v1.IamCredentialsClient;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.pmiops.workbench.auth.DelegatedUserCredentials;
import org.pmiops.workbench.auth.ServiceAccounts;
import org.pmiops.workbench.leonardo.ApiClient;
import org.pmiops.workbench.leonardo.ApiException;
import org.pmiops.workbench.leonardo.api.RuntimesApi;
import org.pmiops.workbench.leonardo.model.LeonardoGetRuntimeResponse;
import org.pmiops.workbench.leonardo.model.LeonardoListRuntimeResponse;
import org.pmiops.workbench.leonardo.model.LeonardoRuntimeStatus;
import org.pmiops.workbench.leonardo.model.LeonardoUpdateRuntimeRequest;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;

/** Oneoff script to restore default autopause threshold settings in an environment. */
public class ResetDefaultRuntimeAutopause {
  private static final Logger log = Logger.getLogger(ResetDefaultRuntimeAutopause.class.getName());
  private static final String ADMIN_SERVICE_ACCOUNT_NAME = "firecloud-admin";
  private static final String[] LEO_SCOPES =
      new String[] {
        "https://www.googleapis.com/auth/userinfo.profile",
        "https://www.googleapis.com/auth/userinfo.email"
      };

  private static void resetAutopause(String projectId, String apiUrl, boolean dryRun)
      throws ApiException, IOException, GeneralSecurityException {
    String dryMsg = dryRun ? "[DRY RUN]: would have... " : "";

    IamCredentialsClient iamCredentialsClient = IamCredentialsClient.create();
    HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

    AtomicInteger updated = new AtomicInteger();
    RuntimesApi api = newApiClient(apiUrl);
    List<LeonardoListRuntimeResponse> runtimes = api.listRuntimes(null, false);
    runtimes.stream()
        .sorted(Comparator.comparing(LeonardoListRuntimeResponse::getRuntimeName))
        .filter(
            (r) -> {
              LeonardoGetRuntimeResponse resp = null;
              try {
                resp = api.getRuntime(r.getGoogleProject(), r.getRuntimeName());
              } catch (ApiException e) {
                log.log(Level.SEVERE, "failed to get runtime", e);
                return false;
              }
              return resp.getAutopauseThreshold() == 0;
            })
        .forEachOrdered(
            (r) -> {
              if (!dryRun) {
                try {
                  // The GAE service account lacks sufficient permissions to update a runtime - this
                  // can only be done by the runtime creator.
                  RuntimesApi impersonatedClient =
                      newImpersonatedApiClient(
                          iamCredentialsClient,
                          httpTransport,
                          projectId,
                          apiUrl,
                          r.getAuditInfo().getCreator());
                  impersonatedClient.updateRuntime(
                      r.getGoogleProject(),
                      r.getRuntimeName(),
                      new LeonardoUpdateRuntimeRequest().autopause(true));
                } catch (ApiException | IOException e) {
                  log.log(Level.SEVERE, "failed to update runtime " + runtimeId(r), e);
                  return;
                }
              }
              updated.getAndIncrement();
              System.out.println(dryMsg + "reset runtime autopause: " + formatTabular(r));
            });

    System.out.println(
        String.format(
            "%supdated %d runtimes (of %d checked)", dryMsg, updated.get(), runtimes.size()));
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

  private static RuntimesApi newImpersonatedApiClient(
      IamCredentialsClient iamCredentialsClient,
      HttpTransport httpTransport,
      String projectId,
      String apiUrl,
      String userEmail)
      throws IOException {
    final OAuth2Credentials delegatedCreds =
        new DelegatedUserCredentials(
            ServiceAccounts.getServiceAccountEmail(ADMIN_SERVICE_ACCOUNT_NAME, projectId),
            userEmail,
            Arrays.asList(LEO_SCOPES),
            iamCredentialsClient,
            httpTransport);
    delegatedCreds.refreshIfExpired();

    RuntimesApi api = newApiClient(apiUrl);
    api.getApiClient().setAccessToken(delegatedCreds.getAccessToken().getTokenValue());
    return api;
  }

  private static String formatTabular(LeonardoListRuntimeResponse r) {
    Gson gson = new Gson();
    JsonObject labels = gson.toJsonTree(r.getLabels()).getAsJsonObject();
    String creator = "unknown";
    if (labels.has("created-by")) {
      creator = labels.get("created-by").getAsString();
    }
    LeonardoRuntimeStatus status = LeonardoRuntimeStatus.UNKNOWN;
    if (r.getStatus() != null) {
      status = r.getStatus();
    }
    return String.format(
        "%-40.40s %-50.50s %-10s %-15s",
        runtimeId(r), creator, status, r.getAuditInfo().getCreatedDate());
  }

  private static String runtimeId(LeonardoListRuntimeResponse r) {
    return r.getGoogleProject() + "/" + r.getRuntimeName();
  }

  @Bean
  public CommandLineRunner run() {
    return (args) -> {
      boolean dryRun = Boolean.parseBoolean(args[2]);
      resetAutopause(args[0], args[1], dryRun);
    };
  }

  public static void main(String[] args) throws Exception {
    // This tool doesn't currently need database access, so it doesn't extend the
    // CommandLineToolConfig. To add database access, extend from that config and update project.rb
    // to ensure a Cloud SQL proxy is available when this command is run.
    new SpringApplicationBuilder(ResetDefaultRuntimeAutopause.class)
        .web(WebApplicationType.NONE)
        .run(args)
        .close();
  }
}
