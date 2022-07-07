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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
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
import java.util.Map.Entry;
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
import org.pmiops.workbench.model.WorkspaceActiveStatus;
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
              "all-of-us-rw-perf", ImmutableList.of("terra_perf_aou_perf", "terra_perf_aou_perf"))
          .build();

  private static final Multimap<String, String> ENV_TO_PREFIXES =
      ImmutableListMultimap.<String, String>builder()
          .putAll("all-of-us-workbench-test", ImmutableList.of("aou-rw-test", "aou-rw-local1"))
          .putAll(
              "all-of-us-rw-perf", ImmutableList.of("terra_perf_aou_perf", "terra_perf_aou_perf"))
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
          .desc("User whose credentials should be used to query for and delete google projects.")
          .required()
          .hasArg()
          .build();

  private static final Options OPTIONS =
      new Options()
          .addOption(ORGANIZATION_OPT)
          .addOption(RW_ENV_OPT)
          .addOption(TERRA_ADMIN_TOKEN_OPT);

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

      List<FieldsForOrphanChecking> workspaces = workspaceDao.getFieldsForOrphanChecking();
      Multimap<String, FieldsForOrphanChecking> workspacesByProject =
          Multimaps.index(workspaces, FieldsForOrphanChecking::getGoogleProject);
      Set<String> dbGoogleProjects = workspacesByProject.keySet();
      LOG.info(
          String.format(
              "Retrieved %d workspaces with %d unique google projects",
              workspaces.size(), dbGoogleProjects.size()));

      Map<String, FieldsForOrphanChecking> aouPpwDbWorkspaces =
          workspacesByProject.asMap().entrySet().stream()
              .filter(e -> e.getValue().size() == 1)
              .collect(Collectors.toMap(Entry::getKey, e -> e.getValue().iterator().next()));

      LOG.info("aouPpwDbWorkspaces count = " + aouPpwDbWorkspaces.size());

      // can't use Multimap.index() because some projects are null
      Map<Optional<String>, List<String>> dbGoogleProjectsByPrefix =
          dbGoogleProjects.stream()
              .collect(
                  Collectors.groupingBy(
                      name -> matchPrefixes(ENV_TO_PREFIXES.get(rwEnvOpt), name)));

      dbGoogleProjectsByPrefix.forEach(
          (prefix, projects) -> {
            Multimap<WorkspaceActiveStatus, String> foo =
                Multimaps.index(
                    projects,
                    p -> {
                      Collection<FieldsForOrphanChecking> wsInProj = workspacesByProject.get(p);
                      //                      if (wsInProj.size() > 1) {
                      //                        LOG.info(
                      //                            String.format(
                      //                                "Project %s is associated with %d
                      // workspaces", p, wsInProj.size()));
                      //                      }
                      return wsInProj.iterator().next().getActiveStatusEnum();
                    });
            foo.asMap()
                .forEach(
                    (status, workspacesByStatus) ->
                        LOG.info(
                            String.format(
                                "Workspaces with prefix %s and status %s: %d ",
                                prefix.orElse("NO MATCH"), status, workspacesByStatus.size())));
          });

      CloudResourceManager manager = getCloudResourceManager(httpTransport, tokenOpt);
      Organization organization =
          getOrganization(manager, orgOpt)
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "User does not have access to organization " + orgOpt));

      for (Folder folder : getFoldersForEnv(manager, organization, rwEnvOpt)) {
        Set<String> projectNames =
            getProjectsInFolder(manager, folder).stream()
                .map(Project::getDisplayName)
                .collect(Collectors.toSet());

        // can't use Multimap.index() because some projects are null
        Map<Optional<String>, List<String>> projectsByPrefix =
            projectNames.stream()
                .collect(
                    Collectors.groupingBy(
                        name -> matchPrefixes(ENV_TO_PREFIXES.get(rwEnvOpt), name)));

        Set<String> dbProjectsInFolder = Sets.intersection(dbGoogleProjects, projectNames);
        LOG.info(
            String.format(
                "Workspaces in the AoU DB in folder %s: %d out of %d total",
                folder.getDisplayName(), dbProjectsInFolder.size(), projectNames.size()));

        projectsByPrefix.forEach(
            (prefix, projectsInPrefix) -> {
              LOG.info("Start prefix " + prefix.orElse("NO MATCH"));
              //
              //              projectsInPrefix.stream().sorted().limit(5).forEach(p ->
              // LOG.info("projectsInPrefix - " + p));
              //              dbGoogleProjectsWithPrefix.stream().sorted().limit(5).forEach(p ->
              // LOG.info("dbGoogleProjectsWithPrefix - " + p));

              List<String> dbGoogleProjectsWithPrefix =
                  Optional.ofNullable(dbGoogleProjectsByPrefix.get(prefix))
                      .orElse(Collections.emptyList());
              LOG.info("dbGoogleProjectsWithPrefix count = " + dbGoogleProjectsWithPrefix.size());

              Set<String> dbProjectsInPrefix =
                  Sets.intersection(
                      new HashSet<>(dbGoogleProjectsWithPrefix), new HashSet<>(projectsInPrefix));

              LOG.info(
                  String.format(
                      "Workspaces in the AoU DB in folder %s with prefix %s: %d",
                      folder.getDisplayName(),
                      prefix.orElse("NO MATCH"),
                      dbProjectsInPrefix.size()));
            });

        List<FieldsForOrphanChecking> workspacesInFolder =
            dbProjectsInFolder.stream()
                .flatMap(p -> workspacesByProject.get(p).stream())
                .collect(Collectors.toList());

        Multimap<Pair<WorkspaceActiveStatus, String>, FieldsForOrphanChecking> foo =
            Multimaps.index(
                workspacesInFolder, w -> Pair.of(w.getActiveStatusEnum(), w.getTierName()));

        foo.asMap()
            .forEach(
                (k, ws) ->
                    LOG.info(
                        String.format(
                            "Workspaces in the AoU DB in folder %s with tier %s and active status %s: %d",
                            folder.getDisplayName(), k.getRight(), k.getLeft(), ws.size())));
      }
      // Create the output dir for the files created by this tool
      //      Path outputDir = makeOutputDir();
      //
      //      for (Folder folder : getFoldersForEnv(manager, organization, rwEnvOpt)) {
      //        List<String> projectNames =
      //            getProjectsInFolder(manager, folder).stream()
      //                .map(Project::getDisplayName)
      //                .collect(Collectors.toList());
      //
      //        Path allProjectsFile = outputDir.resolve(String.format("%s.txt",
      // folder.getDisplayName()));
      //        Files.write(allProjectsFile, projectNames, StandardOpenOption.CREATE_NEW);
      //
      //        Map<Optional<String>, List<String>> projectsByPrefix =
      //            projectNames.stream()
      //                .collect(
      //                    Collectors.groupingBy(
      //                        name -> matchPrefixes(ENV_TO_PREFIXES.get(rwEnvOpt), name)));
      //        writeToPrefixFiles(outputDir, folder, projectsByPrefix);
      //      }
    };
  }

  private CloudResourceManager getCloudResourceManager(
      HttpTransport httpTransport, String accessToken) {

    GoogleCredentials creds = new GoogleCredentials(new AccessToken(accessToken, null));
    return new CloudResourceManager.Builder(
            httpTransport, getDefaultJsonFactory(), new HttpCredentialsAdapter(creds))
        .build();
  }

  private Optional<Organization> getOrganization(CloudResourceManager manager, String organization)
      throws IOException {

    List<Organization> orgs = manager.organizations().search().execute().getOrganizations();
    if (orgs == null) {
      LOG.warning("Did not retrieve any organizations");
      return Optional.empty();
    }
    orgs.forEach(
        o -> LOG.info(String.format("Organization %s (%s)", o.getDisplayName(), o.getName())));

    return orgs.stream().filter(o -> o.getDisplayName().equals(organization)).findFirst();
  }

  private Stream<Folder> getSubFolders(CloudResourceManager manager, Folder parent) {
    try {
      return manager.folders().list().setParent(parent.getName()).execute().getFolders().stream();
    } catch (IOException e) {
      // need to convert checked to unchecked in order to call from Streams
      throw new RuntimeException(e);
    }
  }

  private void logProgress(Folder folder, int count) {
    LOG.info(String.format("retrieved %d projects in folder %s", count, folder.getDisplayName()));
  }

  int countSinceLastLog = 0;

  private void maybeLogProgress(Folder folder, int pageCount, int totalCount) {
    final int logThreshold = 10000;

    countSinceLastLog += pageCount;
    if (countSinceLastLog >= logThreshold) {
      logProgress(folder, totalCount);
      countSinceLastLog = 0;
    }
  }

  private List<Project> getProjectsInFolder(CloudResourceManager manager, Folder folder)
      throws IOException {
    List<Project> projects = new ArrayList<>();
    String pageToken = null;
    do {
      ListProjectsResponse response =
          manager.projects().list().setParent(folder.getName()).setPageToken(pageToken).execute();
      List<Project> pageProjects = response.getProjects();
      projects.addAll(pageProjects);
      maybeLogProgress(folder, pageProjects.size(), projects.size());
      pageToken = response.getNextPageToken();
    } while (pageToken != null

    //  && projects.size() < 1000

    );
    logProgress(folder, projects.size());

    return projects;
  }

  private Path makeOutputDir() throws IOException {
    Path outputDir = Paths.get("orphaned-projects", Instant.now().toString());
    LOG.info("Creating output directory " + outputDir);
    return Files.createDirectories(outputDir);
  }

  private Optional<String> matchPrefixes(Collection<String> prefixes, String name) {
    for (String prefix : prefixes) {
      if (name != null && name.startsWith(prefix)) return Optional.of(prefix);
    }
    return Optional.empty();
  }

  private void writeToPrefixFiles(
      Path outputDir, Folder folder, Map<Optional<String>, List<String>> projectsByPrefix)
      throws IOException {

    for (Entry<Optional<String>, List<String>> e : projectsByPrefix.entrySet()) {
      String prefix = e.getKey().orElse("other");
      Path prefixedProjectsFile =
          outputDir.resolve(String.format("%s__%s.txt", folder.getDisplayName(), prefix));
      Files.write(prefixedProjectsFile, e.getValue(), StandardOpenOption.CREATE_NEW);
    }
  }

  private List<Folder> getAllParentFolders(CloudResourceManager manager, Organization organization)
      throws IOException {
    List<Folder> allParentFolders =
        manager.folders().list().setParent(organization.getName()).execute().getFolders();
    if (allParentFolders == null) {
      LOG.warning("Did not retrieve any parent folders");
      return Collections.emptyList();
    }
    return allParentFolders;
  }

  private List<Folder> getFoldersForEnv(
      CloudResourceManager manager, Organization organization, String environment)
      throws IOException {

    // our hierarchy is organization -> parent-folder -> folder

    return getAllParentFolders(manager, organization).stream()
        .filter(pf -> pf.getDisplayName().equals(ENV_TO_PARENT_FOLDER.get(environment)))
        .map(pf -> logFolder(pf, "..Folder"))
        .flatMap(pf -> getSubFolders(manager, pf))
        .filter(f -> ENV_TO_FOLDERS.get(environment).contains(f.getDisplayName()))
        .map(pf -> logFolder(pf, "....Folder"))
        .collect(Collectors.toList());
  }

  // for debugging
  private List<Folder> getAllFolders(CloudResourceManager manager, Organization organization)
      throws IOException {

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

  // for debugging - can insert into a stream with .map()
  private Folder logFolder(Folder folder, String prefix) {
    LOG.info(String.format("%s %s (%s)", prefix, folder.getDisplayName(), folder.getName()));
    return folder;
  }

  public static void main(String[] args) {
    CommandLineToolConfig.runCommandLine(OrphanedProjects.class, args);
  }
}
