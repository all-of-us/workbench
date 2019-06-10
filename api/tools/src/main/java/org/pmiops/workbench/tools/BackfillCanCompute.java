package org.pmiops.workbench.tools;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.pmiops.workbench.firecloud.ApiClient;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.api.WorkspacesApi;
import org.pmiops.workbench.firecloud.model.Workspace;
import org.pmiops.workbench.firecloud.model.WorkspaceACL;
import org.pmiops.workbench.firecloud.model.WorkspaceACLUpdate;
import org.pmiops.workbench.firecloud.model.WorkspaceAccessEntry;
import org.pmiops.workbench.firecloud.model.WorkspaceResponse;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Backfill script for granting canCompute permission to all AoU editors and owners. For
 * http://broad.io/1ppw, collaborators will need canCompute in order to launch clusters within
 * shared billing projects.
 */
@Configuration
public class BackfillCanCompute {
  private static Option fcBaseUrlOpt =
      Option.builder()
          .longOpt("fc-base-url")
          .desc("Firecloud API base URL")
          .required()
          .hasArg()
          .build();
  private static Option billingProjectPrefixOpt =
      Option.builder()
          .longOpt("billing-project-prefix")
          .desc("Billing project prefix to filter by, other workspaces are ignored")
          .required()
          .hasArg()
          .build();
  private static Option dryRunOpt =
      Option.builder()
          .longOpt("dry-run")
          .desc("If specified, the tool runs in dry run mode; no modifications are made")
          .build();
  private static Options options =
      new Options().addOption(fcBaseUrlOpt).addOption(billingProjectPrefixOpt).addOption(dryRunOpt);

  private static final String[] FC_SCOPES =
      new String[] {
        "https://www.googleapis.com/auth/userinfo.profile",
        "https://www.googleapis.com/auth/userinfo.email"
      };
  private static final Logger log = Logger.getLogger(BackfillCanCompute.class.getName());
  private static final Set<String> CAN_COMPUTE_ROLES =
      ImmutableSet.of("PROJECT_OWNER", "OWNER", "WRITER");

  private static WorkspacesApi newWorkspacesApi(String apiUrl) throws IOException {
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(apiUrl);
    GoogleCredential credential =
        GoogleCredential.getApplicationDefault().createScoped(Arrays.asList(FC_SCOPES));
    credential.refreshToken();
    apiClient.setAccessToken(credential.getAccessToken());
    WorkspacesApi api = new WorkspacesApi();
    api.setApiClient(apiClient);
    return api;
  }

  private static void dryLog(boolean dryRun, String msg) {
    String prefix = "";
    if (dryRun) {
      prefix = "[DRY RUN] Would have... ";
    }
    log.info(prefix + msg);
  }

  /**
   * Swagger Java codegen does not handle the WorkspaceACL model correctly; it returns a GSON map
   * instead. Run this through a typed Gson conversion process to coerce it into the desired type.
   */
  private static Map<String, WorkspaceAccessEntry> extractAclResponse(WorkspaceACL aclResp) {
    Type accessEntryType = new TypeToken<Map<String, WorkspaceAccessEntry>>() {}.getType();
    Gson gson = new Gson();
    return gson.fromJson(gson.toJson(aclResp.getAcl(), accessEntryType), accessEntryType);
  }

  private static void backfill(
      WorkspacesApi workspacesApi, String billingProjectPrefix, boolean dryRun)
      throws ApiException {
    int userUpgrades = 0;
    int workspaceUpdates = 0;
    for (WorkspaceResponse resp : workspacesApi.listWorkspaces()) {
      Workspace w = resp.getWorkspace();
      if (!w.getNamespace().startsWith(billingProjectPrefix)) {
        continue;
      }

      String id = w.getNamespace() + "/" + w.getName();
      if (!"PROJECT_OWNER".equals(resp.getAccessLevel())) {
        log.warning(
            String.format(
                "service account has '%s' access to workspace '%s'; skipping",
                resp.getAccessLevel(), id));
        continue;
      }

      List<WorkspaceACLUpdate> updates = new ArrayList<>();
      Map<String, WorkspaceAccessEntry> acl =
          extractAclResponse(workspacesApi.getWorkspaceAcl(w.getNamespace(), w.getName()));
      for (String user : acl.keySet()) {
        WorkspaceAccessEntry entry = acl.get(user);
        if (!CAN_COMPUTE_ROLES.contains(entry.getAccessLevel()) || entry.getCanCompute()) {
          continue;
        }
        dryLog(
            dryRun,
            String.format("upgrading user '%s' (%s) to canCompute", user, entry.getAccessLevel()));
        userUpgrades++;
        updates.add(
            new WorkspaceACLUpdate()
                .email(user)
                .canCompute(true)
                .canShare(entry.getCanShare())
                .accessLevel(entry.getAccessLevel()));
      }

      if (!updates.isEmpty()) {
        dryLog(dryRun, String.format("applying upgrades to workspace '%s'", id));
        workspaceUpdates++;
        if (!dryRun) {
          workspacesApi.updateWorkspaceACL(
              w.getNamespace(), w.getName(), /* inviteUsersNotFound */ false, updates);
        }
      }
    }

    dryLog(
        dryRun,
        String.format("upgraded %d users in %d workspace updates", userUpgrades, workspaceUpdates));
  }

  @Bean
  public CommandLineRunner run() {
    return (args) -> {
      CommandLine opts = new DefaultParser().parse(options, args);
      backfill(
          newWorkspacesApi(opts.getOptionValue(fcBaseUrlOpt.getLongOpt())),
          opts.getOptionValue(billingProjectPrefixOpt.getLongOpt()),
          opts.hasOption(dryRunOpt.getLongOpt()));
    };
  }

  public static void main(String[] args) {
    new SpringApplicationBuilder(BackfillCanCompute.class).web(false).run(args);
  }
}
