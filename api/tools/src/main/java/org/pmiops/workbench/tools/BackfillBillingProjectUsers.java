package org.pmiops.workbench.tools;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.api.BillingApi;
import org.pmiops.workbench.firecloud.api.WorkspacesApi;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceACL;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceAccessEntry;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Backfill script for granting the billing project user role to all AoU owners (corresponding to
 * Firecloud OWNER access). For http://broad.io/1ppw, collaborators need to be either writers or
 * billing project users in order to launch clusters within shared billing projects, see RW-3009 and
 * RW-3188 for details.
 */
@Configuration
public class BackfillBillingProjectUsers {
  public static final List<String> FIRECLOUD_LIST_WORKSPACES_REQUIRED_FIELDS =
      ImmutableList.of(
          "accessLevel", "workspace.namespace", "workspace.name", "workspace.createdBy");

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

  private static final Logger log = Logger.getLogger(BackfillBillingProjectUsers.class.getName());

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
  public static Map<String, FirecloudWorkspaceAccessEntry> extractAclResponse(
      FirecloudWorkspaceACL aclResp) {
    Type accessEntryType = new TypeToken<Map<String, FirecloudWorkspaceAccessEntry>>() {}.getType();
    Gson gson = new Gson();
    return gson.fromJson(gson.toJson(aclResp.getAcl(), accessEntryType), accessEntryType);
  }

  private static void backfill(
      WorkspacesApi workspacesApi,
      BillingApi billingApi,
      String billingProjectPrefix,
      boolean dryRun)
      throws ApiException {
    int userUpgrades = 0;
    for (FirecloudWorkspaceResponse resp :
        workspacesApi.listWorkspaces(FIRECLOUD_LIST_WORKSPACES_REQUIRED_FIELDS)) {
      FirecloudWorkspace w = resp.getWorkspace();
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

      Map<String, FirecloudWorkspaceAccessEntry> acl =
          extractAclResponse(workspacesApi.getWorkspaceAcl(w.getNamespace(), w.getName()));
      for (String user : acl.keySet()) {
        if (user.equals(w.getCreatedBy())) {
          // Skip the common case, creators should already be billing project users. Any edge cases
          // where this is not true will be fixed by the 1PPW migration (RW-2705).
          continue;
        }
        FirecloudWorkspaceAccessEntry entry = acl.get(user);
        if (!"OWNER".equals(entry.getAccessLevel())) {
          // Only owners should be granted billing project user.
          continue;
        }
        dryLog(
            dryRun,
            String.format(
                "granting billing project user on '%s' to '%s' (%s)",
                w.getNamespace(), user, entry.getAccessLevel()));
        if (!dryRun) {
          try {
            billingApi.addUserToBillingProject(w.getNamespace(), "user", user);
          } catch (ApiException e) {
            log.log(Level.WARNING, "failed to add user to project", e);
          }
        }
        userUpgrades++;
      }
    }

    dryLog(dryRun, String.format("added %d users as billing project users", userUpgrades));
  }

  @Bean
  public CommandLineRunner run() {
    return (args) -> {
      CommandLine opts = new DefaultParser().parse(options, args);

      ServiceAccountAPIClientFactory apiFactory =
          new ServiceAccountAPIClientFactory(opts.getOptionValue(fcBaseUrlOpt.getLongOpt()));

      backfill(
          apiFactory.workspacesApi(),
          apiFactory.billingApi(),
          opts.getOptionValue(billingProjectPrefixOpt.getLongOpt()),
          opts.hasOption(dryRunOpt.getLongOpt()));
    };
  }

  public static void main(String[] args) {
    new SpringApplicationBuilder(BackfillBillingProjectUsers.class).web(false).run(args);
  }
}
