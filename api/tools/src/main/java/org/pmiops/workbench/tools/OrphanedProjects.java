package org.pmiops.workbench.tools;

import static com.google.api.client.googleapis.util.Utils.getDefaultJsonFactory;

import com.google.api.client.http.HttpTransport;
import com.google.api.services.cloudresourcemanager.v3.CloudResourceManager;
import com.google.api.services.cloudresourcemanager.v3.model.Folder;
import com.google.api.services.cloudresourcemanager.v3.model.ListProjectsResponse;
import com.google.api.services.cloudresourcemanager.v3.model.Organization;
import com.google.api.services.cloudresourcemanager.v3.model.Project;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.tuple.Pair;
import org.pmiops.workbench.db.dao.WorkspaceDao;
import org.pmiops.workbench.db.dao.WorkspaceDao.FieldsForOrphanChecking;
import org.pmiops.workbench.exceptions.BadRequestException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

public class OrphanedProjects {
  private static final Logger LOG = Logger.getLogger(OrphanedProjects.class.getName());

  private static final List<String> ALLOWED_ORGANIZATIONS =
      ImmutableList.of("firecloud.org", "test.firecloud.org");
  private static final List<String> ALLOWED_RW_ENVS =
      ImmutableList.of("all-of-us-workbench-test", "all-of-us-rw-perf");

  private static final Map<String, String> ENV_TO_PARENT_FOLDER =
      ImmutableMap.of("all-of-us-workbench-test", "dev", "all-of-us-rw-perf", "perf");

  private static final Multimap<String, String> ENV_TO_FOLDERS =
      ImmutableListMultimap.<String, String>builder()
          .putAll(
              "all-of-us-workbench-test",
              ImmutableList.of("terra_dev_aou_test", "terra_dev_aou_test_2"))
          .putAll(
              "all-of-us-rw-perf", ImmutableList.of("terra_perf_aou_perf", "terra_perf_aou_perf_2"))
          .build();

  private static final Multimap<String, String> ENV_TO_PREFIXES =
      ImmutableListMultimap.<String, String>builder()
          .putAll("all-of-us-workbench-test", ImmutableList.of("aou-rw-test", "aou-rw-local1"))
          .putAll("all-of-us-rw-perf", ImmutableList.of("aou-rw-perf"))
          .build();

  private static final Option ORGANIZATION_OPT =
      Option.builder()
          .longOpt("organization")
          .desc(
              "The Google Organization to query for projects.  firecloud.org and test.firecloud.org are supported.")
          .required()
          .hasArg()
          .build();

  private static final Option RW_ENV_OPT =
      Option.builder()
          .longOpt("environment")
          .desc("The AoU environment to access for project cleanup.")
          .required()
          .hasArg()
          .build();

  private static final Option TERRA_ADMIN_TOKEN_OPT =
      Option.builder()
          .longOpt("terra-admin-token")
          .desc("A current bearer token of a Terra admin, used to query for google projects.")
          .required()
          .hasArg()
          .build();

  private static final Options OPTIONS =
      new Options()
          .addOption(ORGANIZATION_OPT)
          .addOption(RW_ENV_OPT)
          .addOption(TERRA_ADMIN_TOKEN_OPT);

  private void processTier(
      CloudResourceManager manager,
      List<Folder> folders,
      String environment,
      String tierName,
      Collection<FieldsForOrphanChecking> workspaces) {
    LOG.info(
        String.format(
            "Retrieved %d active workspaces in the %s tier from the AoU DB",
            workspaces.size(), tierName));

    String folderName = getFolderNameForTier(environment, tierName);
    Folder folder =
        folders.stream()
            .filter(f -> f.getDisplayName().equals(folderName))
            .findFirst()
            .orElseThrow(
                () ->
                    new BadRequestException(
                        String.format(
                            "Folder name %s not found for tier %s", folderName, tierName)));

    List<Project> projects = OPUtils.getProjectsInFolder(manager, folder);

    List<Pair<FieldsForOrphanChecking, Project>> matches = findMatches(workspaces, projects);

    Set<FieldsForOrphanChecking> dbMatched =
        matches.stream().map(Pair::getLeft).collect(Collectors.toSet());
    Set<Project> cloudMatched = matches.stream().map(Pair::getRight).collect(Collectors.toSet());

    Set<FieldsForOrphanChecking> dbOnly = Sets.difference(new HashSet<>(workspaces), dbMatched);
    Set<Project> cloudOnly = Sets.difference(new HashSet<>(projects), cloudMatched);

    LOG.info(
        String.format(
            "%d projects match, %d projects are only in the DB, and %d projects are only in the cloud",
            matches.size(), dbOnly.size(), cloudOnly.size()));

    Path outputDir = makeOutputDir();
    OPUtils.writeMatchesToTierCSV(outputDir, tierName, matches);
    OPUtils.writeWorkspacesToTierCSV(outputDir, tierName, dbOnly);
    OPUtils.writeProjectsToTierCSV(outputDir, tierName, cloudOnly);

    // can't use Multimap.index() because some projects are null
    Map<Optional<String>, List<String>> dbOnlyProjectsByPrefix =
        dbOnly.stream()
            .map(FieldsForOrphanChecking::getGoogleProject)
            .collect(
                Collectors.groupingBy(
                    name -> matchPrefixes(ENV_TO_PREFIXES.get(environment), name)));
    writeToPrefixFiles(outputDir, tierName, "db", dbOnlyProjectsByPrefix);

    // can't use Multimap.index() because some projects are null
    Map<Optional<String>, List<String>> cloudOnlyProjectsByPrefix =
        cloudOnly.stream()
            .map(Project::getProjectId)
            .collect(
                Collectors.groupingBy(
                    name -> matchPrefixes(ENV_TO_PREFIXES.get(environment), name)));
    writeToPrefixFiles(outputDir, tierName, "cloud", cloudOnlyProjectsByPrefix);
  }

  public static void main(String[] args) {
    CommandLineToolConfig.runCommandLine(OrphanedProjects.class, args);
  }

  @Bean
  public CommandLineRunner run(HttpTransport httpTransport, WorkspaceDao workspaceDao) {
    return (args) -> {
      final CommandLine opts = new DefaultParser().parse(OPTIONS, args);
      final String orgOpt = opts.getOptionValue(ORGANIZATION_OPT.getLongOpt());
      if (!ALLOWED_ORGANIZATIONS.contains(orgOpt)) {
        throw new IllegalArgumentException("Unsupported organization: " + orgOpt);
      }
      final String rwEnvOpt = opts.getOptionValue(RW_ENV_OPT.getLongOpt());
      if (!ALLOWED_RW_ENVS.contains(rwEnvOpt)) {
        throw new IllegalArgumentException("Unsupported RW environment: " + rwEnvOpt);
      }
      final String tokenOpt = opts.getOptionValue(TERRA_ADMIN_TOKEN_OPT.getLongOpt());

      CloudResourceManager manager = OPUtils.getCloudResourceManager(httpTransport, tokenOpt);
      Organization organization = OPUtils.getOrganization(manager, orgOpt);
      List<Folder> folders =
          OPUtils.getFoldersForEnv(
              manager,
              organization,
              ENV_TO_PARENT_FOLDER.get(rwEnvOpt),
              ENV_TO_FOLDERS.get(rwEnvOpt));

      // All such workspaces should:
      // * have 1PPW google projects
      // * be associated with a CDR in the registered or controlled tier
      // * accordingly, their google projects should be in the Service Perimeter and Google Cloud
      // Folder appropriate for the tier
      List<FieldsForOrphanChecking> activeWorkspaces =
          workspaceDao.getActiveWorkspacesForOrphanChecking();
      Multimap<String, FieldsForOrphanChecking> activeWorkspacesByTier =
          Multimaps.index(activeWorkspaces, FieldsForOrphanChecking::getTierName);

      activeWorkspacesByTier
          .asMap()
          .forEach(
              (tierName, workspaces) ->
                  processTier(manager, folders, rwEnvOpt, tierName, workspaces));
    };
  }

  static boolean anyFieldMatches(FieldsForOrphanChecking workspace, Project project) {
    Set<String> wsFields = ImmutableSet.of(workspace.getGoogleProject(), workspace.getNamespace());

    // ImmutableSet does not like nulls
    String cloudDisplayName = OPUtils.nullSafeString(project.getDisplayName());
    Set<String> cloudFields =
        ImmutableSet.of(
            project.getProjectId(), cloudDisplayName, OPUtils.nullSafeNamespaceLabel(project));

    return !Collections.disjoint(wsFields, cloudFields);
  }

  static List<Pair<FieldsForOrphanChecking, Project>> findMatches(
      Collection<FieldsForOrphanChecking> workspaces, List<Project> projects) {
    LOG.info(
        String.format(
            "Attempting to match %d workspaces against %d projects",
            workspaces.size(), projects.size()));

    return workspaces.stream()
        .flatMap(
            w ->
                projects.stream()
                    .filter(p -> anyFieldMatches(w, p))
                    .findFirst()
                    .map(p -> Stream.of(Pair.of(w, p)))
                    .orElse(Stream.empty()))
        .collect(Collectors.toList());
  }

  private static Path makeOutputDir() {
    String now = Instant.now().toString().replace(':', '-'); // : is awkward
    Path outputDir = Paths.get("orphaned-projects", now);
    LOG.info("Creating output directory " + outputDir);
    try {
      return Files.createDirectories(outputDir);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static Optional<String> matchPrefixes(Collection<String> prefixes, String name) {
    for (String prefix : prefixes) {
      if (name != null && name.startsWith(prefix)) return Optional.of(prefix);
    }
    return Optional.empty();
  }

  static void writeToPrefixFiles(
      Path outputDir,
      String tierName,
      String type,
      Map<Optional<String>, List<String>> projectsByPrefix) {
    projectsByPrefix.forEach(
        (maybePrefix, projectNames) -> {
          String prefix = maybePrefix.orElse("other");
          Path prefixedProjectsFile =
              outputDir.resolve(String.format("%s_%s_%s.txt", tierName, type, prefix));
          try {
            Files.write(
                prefixedProjectsFile,
                OPUtils.nullSafeSort(projectNames),
                StandardOpenOption.CREATE_NEW);
            OPUtils.logFileWrite(prefixedProjectsFile, projectNames.size());
          } catch (IOException ex) {
            throw new RuntimeException(ex);
          }
        });
  }

  private Table<String, String, String> folderNameTable;

  private Table<String, String, String> initFolderNameTable() {
    Table<String, String, String> table = HashBasedTable.create();
    table.put("all-of-us-workbench-test", "registered", "terra_dev_aou_test");
    table.put("all-of-us-workbench-test", "controlled", "terra_dev_aou_test_2");
    table.put("all-of-us-rw-perf", "registered", "terra_perf_aou_perf");
    table.put("all-of-us-rw-perf", "controlled", "terra_perf_aou_perf_2");
    return table;
  }

  private String getFolderNameForTier(String environment, String tierName) {
    if (folderNameTable == null) {
      folderNameTable = initFolderNameTable();
    }
    return folderNameTable.get(environment, tierName);
  }

  public static class OPUtils {
    private static final Logger LOG = Logger.getLogger(OrphanedProjects.class.getName());

    private static Folder logFolder(Folder folder, String prefix) {
      LOG.info(String.format("%s %s (%s)", prefix, folder.getDisplayName(), folder.getName()));
      return folder;
    }

    static void logFileWrite(Path path, int size) {
      LOG.info(String.format("Wrote %d lines to file %s", size, path.toString()));
    }

    private static void logProgress(Folder folder, int count) {
      LOG.info(
          String.format(
              "Retrieving %d projects from folder %s in Google Cloud...",
              count, folder.getDisplayName()));
    }

    private static void logProgressFinal(Folder folder, int count) {
      LOG.info(
          String.format(
              "Retrieved %d projects from folder %s in Google Cloud",
              count, folder.getDisplayName()));
    }

    private static int countSinceLastLog = 0;

    private static void maybeLogProgress(Folder folder, int pageCount, int totalCount) {
      final int logThreshold = 10000;

      countSinceLastLog += pageCount;
      if (countSinceLastLog >= logThreshold) {
        logProgress(folder, totalCount);
        countSinceLastLog = 0;
      }
    }

    // some data structures don't tolerate nulls - use this instead
    private static final String NULL_STRING = "[NULL]";

    static String nullSafeString(String maybeString) {
      return Optional.ofNullable(maybeString).orElse(NULL_STRING);
    }

    static List<String> nullSafeSort(Collection<String> maybeStrings) {
      return maybeStrings.stream()
          .map(name -> name == null ? NULL_STRING : name)
          .sorted()
          .collect(Collectors.toList());
    }

    static String nullSafeNamespaceLabel(Project project) {
      return Optional.ofNullable(project.getLabels())
          .flatMap(labels -> Optional.ofNullable(labels.get("workspacenamespace")))
          .orElse(NULL_STRING);
    }

    private static void writeToTierCSV(
        Path outputDir,
        String tierName,
        String type,
        String csvHeader,
        Collection<String> unsortedLines) {
      Path tierFile = outputDir.resolve(String.format("%s_%s.csv", tierName, type));

      List<String> lines = nullSafeSort(unsortedLines);
      // prepend CSV header
      lines.add(0, csvHeader);

      try {
        Files.write(tierFile, lines, StandardOpenOption.CREATE_NEW);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      logFileWrite(tierFile, lines.size());
    }

    static void writeMatchesToTierCSV(
        Path outputDir,
        String tierName,
        List<Pair<FieldsForOrphanChecking, Project>> matchedPairs) {
      List<String> lines =
          matchedPairs.stream()
              .map(
                  p ->
                      String.join(
                          ",",
                          p.getLeft().getGoogleProject(),
                          p.getLeft().getNamespace(),
                          p.getRight().getProjectId(),
                          p.getRight().getDisplayName(),
                          nullSafeNamespaceLabel(p.getRight())))
              .collect(Collectors.toList());

      writeToTierCSV(
          outputDir,
          tierName,
          "match",
          "ws.googleProject,ws.namespace,cloud.projectId,cloud.displayName,cloud.labels.workspacenamespace",
          lines);
    }

    static void writeWorkspacesToTierCSV(
        Path outputDir, String tierName, Collection<FieldsForOrphanChecking> workspaces) {
      List<String> lines =
          workspaces.stream()
              .map(w -> String.join(",", w.getGoogleProject(), w.getNamespace()))
              .collect(Collectors.toList());

      writeToTierCSV(outputDir, tierName, "db", "ws.googleProject,ws.namespace", lines);
    }

    static void writeProjectsToTierCSV(
        Path outputDir, String tierName, Collection<Project> projects) {
      List<String> lines =
          projects.stream()
              .map(
                  p ->
                      String.join(
                          ",", p.getProjectId(), p.getDisplayName(), nullSafeNamespaceLabel(p)))
              .collect(Collectors.toList());

      writeToTierCSV(
          outputDir,
          tierName,
          "cloud",
          "cloud.projectId,cloud.displayName,cloud.labels.workspacenamespace",
          lines);
    }

    // CloudResourceManager methods

    static CloudResourceManager getCloudResourceManager(
        HttpTransport httpTransport, String accessToken) {

      GoogleCredentials creds = new GoogleCredentials(new AccessToken(accessToken, null));
      return new CloudResourceManager.Builder(
              httpTransport, getDefaultJsonFactory(), new HttpCredentialsAdapter(creds))
          .build();
    }

    static Organization getOrganization(CloudResourceManager manager, String organization) {
      final List<Organization> orgs;
      try {
        orgs = manager.organizations().search().execute().getOrganizations();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      if (orgs == null) {
        throw new BadRequestException(
            "User does not have access to any organizations in CloudResourceManager");
      }

      orgs.forEach(
          o -> LOG.info(String.format("Organization %s (%s)", o.getDisplayName(), o.getName())));

      return orgs.stream()
          .filter(o -> o.getDisplayName().equals(organization))
          .findFirst()
          .orElseThrow(
              () ->
                  new BadRequestException(
                      "User does not have access to organization " + organization));
    }

    private static List<Folder> getAllParentFolders(
        CloudResourceManager manager, Organization organization) {
      final List<Folder> allParentFolders;
      try {
        allParentFolders =
            manager.folders().list().setParent(organization.getName()).execute().getFolders();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      if (allParentFolders == null) {
        LOG.warning("Did not retrieve any parent folders");
        return Collections.emptyList();
      }
      return allParentFolders;
    }

    private static Stream<Folder> getSubFolders(CloudResourceManager manager, Folder parent) {
      try {
        return manager.folders().list().setParent(parent.getName()).execute().getFolders().stream();
      } catch (IOException e) {
        // need to convert checked to unchecked in order to call from Streams
        throw new RuntimeException(e);
      }
    }

    static List<Project> getProjectsInFolder(CloudResourceManager manager, Folder folder) {
      List<Project> projects = new ArrayList<>();
      String pageToken = null;
      do {
        final ListProjectsResponse response;
        try {
          response =
              manager
                  .projects()
                  .list()
                  .setParent(folder.getName())
                  .setPageToken(pageToken)
                  .execute();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        List<Project> pageProjects = response.getProjects();
        projects.addAll(pageProjects);
        maybeLogProgress(folder, pageProjects.size(), projects.size());
        pageToken = response.getNextPageToken();
      } while (pageToken != null);
      logProgressFinal(folder, projects.size());

      return projects;
    }

    static List<Folder> getFoldersForEnv(
        CloudResourceManager manager,
        Organization organization,
        String parentFolder,
        Collection<String> envFolders) {

      // our hierarchy is organization -> parent-folder -> folder

      return getAllParentFolders(manager, organization).stream()
          .filter(pf -> pf.getDisplayName().equals(parentFolder))
          .map(pf -> logFolder(pf, "..Folder"))
          .flatMap(pf -> getSubFolders(manager, pf))
          .filter(f -> envFolders.contains(f.getDisplayName()))
          .map(pf -> logFolder(pf, "....Folder"))
          .collect(Collectors.toList());
    }

    // similar to above, but show all retrieved folders and hardcode expected folder names - for
    // debugging
    static List<Folder> getAllFolders(CloudResourceManager manager, Organization organization) {

      final List<String> AOU_PARENT_FOLDERS = ImmutableList.of("dev", "perf");
      final List<String> AOU_FOLDERS =
          ImmutableList.of(
              "terra_dev_aou_test",
              "terra_dev_aou_test_2",
              "terra_perf_aou_perf",
              "terra_perf_aou_perf_2");

      return getAllParentFolders(manager, organization).stream()
          .map(pf -> logFolder(pf, "Parent Folder"))
          .filter(pf -> AOU_PARENT_FOLDERS.contains(pf.getDisplayName()))
          .map(pf -> logFolder(pf, "AoU Parent Folder"))
          .flatMap(pf -> getSubFolders(manager, pf))
          .map(pf -> logFolder(pf, "Folder"))
          .filter(f -> AOU_FOLDERS.contains(f.getDisplayName()))
          .map(pf -> logFolder(pf, "AoU Folder"))
          .collect(Collectors.toList());
    }
  }
}
