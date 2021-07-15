package org.pmiops.workbench.tools;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.pmiops.workbench.firecloud.ApiException;
import org.pmiops.workbench.firecloud.FirecloudTransforms;
import org.pmiops.workbench.firecloud.api.BillingV2Api;
import org.pmiops.workbench.firecloud.api.WorkspacesApi;
import org.pmiops.workbench.firecloud.model.FirecloudBillingProjectMember;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspace;
import org.pmiops.workbench.firecloud.model.FirecloudWorkspaceResponse;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Backfill script to adjust users with improper billing project access. Dry run mode can be used to
 * audit for inconsistent access. Specifically this aims to revoke access for users who were
 * incompletely removed as OWNERs per RW-5013, though this situation can theoretically arise in the
 * event of a normal partial sharing failure (sharing and setting of the billing project role cannot
 * be done transactionally).
 */
@Configuration
public class FixDesynchronizedBillingProjectOwners {
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
  private static Option billingProjectIdsOpt =
      Option.builder()
          .longOpt("billing-project-ids")
          .desc("Billing project IDs to filter by, all projects are considered if empty")
          .hasArg()
          .build();
  private static Option researcherDomain =
      Option.builder()
          .longOpt("researcher-domain")
          .desc("Researcher email domain, e.g. researchallofus.org for prod")
          .hasArg()
          .build();
  private static Option dryRunOpt =
      Option.builder()
          .longOpt("dry-run")
          .desc("If specified, the tool runs in dry run mode; no modifications are made")
          .build();
  private static Options options =
      new Options()
          .addOption(fcBaseUrlOpt)
          .addOption(billingProjectIdsOpt)
          .addOption(researcherDomain)
          .addOption(dryRunOpt);

  private static final Logger log =
      Logger.getLogger(FixDesynchronizedBillingProjectOwners.class.getName());

  private static void dryLog(boolean dryRun, String msg) {
    String prefix = "";
    if (dryRun) {
      prefix = "[DRY RUN] Would have... ";
    }
    log.info(prefix + msg);
  }

  private static void clean(
      WorkspacesApi workspacesApi,
      BillingV2Api billingV2Api,
      Set<String> billingProjectIds,
      String researcherDomain,
      boolean dryRun)
      throws ApiException {
    int ownersRemoved = 0;
    int ownersAdded = 0;

    List<FirecloudWorkspaceResponse> workspaces =
        workspacesApi.listWorkspaces(FIRECLOUD_LIST_WORKSPACES_REQUIRED_FIELDS);
    log.info(String.format("found %d workspaces", workspaces.size()));

    // Non-1PPW workspaces still exist and need to be filtered out - this script depends on the
    // assumption that workspace ACLs are synchronized to project ACLs, which doesn't hold when
    // there are multiple workspaces per project.
    Set<String> singleWorkspaceProjects =
        workspaces.stream()
            // Count entries by billing project ID.
            .map(w -> w.getWorkspace().getNamespace())
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
            .entrySet()
            .stream()
            // Only take projects which conform to 1PPW.
            .filter(e -> e.getValue() == 1)
            .map(Entry::getKey)
            .collect(Collectors.toSet());

    for (FirecloudWorkspaceResponse resp : workspaces) {
      FirecloudWorkspace w = resp.getWorkspace();
      if (!billingProjectIds.isEmpty() && !billingProjectIds.contains(w.getNamespace())) {
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

      if (!singleWorkspaceProjects.contains(w.getNamespace())) {
        log.info(String.format("skipping workspace '%s', doesn't conform to 1PPW", id));
        continue;
      }

      Map<String, String> billingProjectRoles =
          billingV2Api.listBillingProjectMembers(w.getNamespace()).stream()
              .filter(m -> m.getEmail().endsWith("@" + researcherDomain))
              .collect(
                  Collectors.toMap(
                      FirecloudBillingProjectMember::getEmail,
                      FirecloudBillingProjectMember::getRole));
      Set<String> billingProjectOwners =
          billingProjectRoles.entrySet().stream()
              .filter(e -> "Owner".equals(e.getValue()))
              .map(Entry::getKey)
              .collect(Collectors.toSet());

      Map<String, String> workspaceRoles =
          FirecloudTransforms.extractAclResponse(
                  workspacesApi.getWorkspaceAcl(w.getNamespace(), w.getName()))
              .entrySet()
              .stream()
              .filter(e -> e.getKey().endsWith("@" + researcherDomain))
              .collect(Collectors.toMap(Entry::getKey, e -> e.getValue().getAccessLevel()));
      Set<String> workspaceOwners =
          workspaceRoles.entrySet().stream()
              .filter(e -> "OWNER".equals(e.getValue()))
              .map(e -> e.getKey())
              .collect(Collectors.toSet());

      // ShareWorkspace first updates the Workspace ACL, then the billing project role. For this
      // reason, the workspace ACL is the source of truth, as it will have been set properly in the
      // event of a partial sharing failure.
      for (String user : Sets.symmetricDifference(billingProjectOwners, workspaceOwners)) {
        dryLog(
            dryRun,
            String.format(
                "'%s': '%s', inconsistency: workspace role '%s', project role '%s'",
                w.getNamespace(), user, workspaceRoles.get(user), billingProjectRoles.get(user)));
        if (billingProjectOwners.contains(user)) {
          // This covers RW-5013, which caused incomplete owner removal.
          if (!dryRun) {
            try {
              billingV2Api.removeUserFromBillingProject(w.getNamespace(), "owner", user);
            } catch (ApiException e) {
              log.log(Level.WARNING, "failed to remove user from project", e);
            }
          }
          dryLog(
              dryRun,
              String.format("removed user '%s' from billing project '%s'", user, w.getNamespace()));
          ownersRemoved++;
        } else {
          // This covers a theoretical situation where an owner was added, but billing project role
          // was not updated. We don't have reason to believe this has been encountered, but it may
          // appear in the future.
          if (!dryRun) {
            try {
              billingV2Api.addUserToBillingProject(w.getNamespace(), "owner", user);
            } catch (ApiException e) {
              log.log(Level.WARNING, "failed to add user to project", e);
            }
          }
          dryLog(
              dryRun,
              String.format("added user '%s' to billing project '%s'", user, w.getNamespace()));
          ownersAdded++;
        }
      }
    }

    dryLog(
        dryRun,
        String.format(
            "removed %d and added %d out-of-sync users as billing project owners",
            ownersRemoved, ownersAdded));
  }

  @Bean
  public CommandLineRunner run() {
    return (args) -> {
      CommandLine opts = new DefaultParser().parse(options, args);

      ServiceAccountAPIClientFactory apiFactory =
          new ServiceAccountAPIClientFactory(opts.getOptionValue(fcBaseUrlOpt.getLongOpt()));

      // An empty set indicates no billing projects should be filtered.
      Set<String> billingProjectIds =
          ImmutableSet.copyOf(
              Optional.ofNullable(opts.getOptionValues(billingProjectIdsOpt.getLongOpt()))
                  .orElse(new String[] {}));

      clean(
          apiFactory.workspacesApi(),
          apiFactory.billingV2Api(),
          billingProjectIds,
          opts.getOptionValue(researcherDomain.getLongOpt()),
          opts.hasOption(dryRunOpt.getLongOpt()));
    };
  }

  public static void main(String[] args) {
    new SpringApplicationBuilder(FixDesynchronizedBillingProjectOwners.class)
        .web(WebApplicationType.NONE)
        .run(args)
        .close();
  }
}
