# Command Line Tools

## What are they?

This document refers to the command line tools that reside in [workbench-api.yaml](https://github.com/all-of-us/workbench/blob/master/api/tools).
They're usually used to invoke operational or one-off tasks that are more complex than a small bash script but 
haven't made it into our standard code base yet. The tools are able to compile and use classes from the 
standard code base so they are relatively quick to write but difficult to maintain. It's not uncommon for a tool to silently break because one of its dependencies in
the standard code base has changed. Point being, it's great that we have the capability to write these tools but don't 
rely on them too much for more complex tasks that we expect to invoke regularly.

Some of the best use cases are operational tasks that would otherwise require manual SQL queries or tasks that 
need to invoke an external API call.

Some examples of tools we've used  
- Changing billing permissions for all users
- Setting a user's authority roles 
  - `./project.rb set-authority`
- Listing runtimes in an environment 
  - `./project.rb list-runtimes`

Advantages of writing tools
- It provides a quick way to write a script that can leverage much of the standard code base
- It handles environment setup, including authentication and database connections

Disadvantages
- No automated testing
- No automated compilation checks
- Still requires recreating many of the Spring Beans to fulfill the dependencies of the services being injected
    - We have many of them covered in `CommandLineToolConfig.java` but importing more complex classes like WorkspaceController 
    will be quite difficult because of the large number of dependencies being pulled in with it.

## Structure of a tool

Try invoking `./project.rb list-runtimes --project all-of-us-workbench-test` to see an example of a working tool. 

Invoking an API tool will generally follow this flow of
1. Running the project.rb command line parsing code which calls...
2. a Gradle function which calls...
3. the tools Java code

### project.rb

The first step in writing a tool is to register the command in [devstart.rb](https://github.com/all-of-us/workbench/blob/master/api/libproject/devstart.rb) 
and write the argument parsing code. This step is mostly boilerplate and you just need to figure out what pieces
you need to pull in.

Here are two examples that cover most of the cases.

list-runtime
```
Common.register_command({
  :invocation => "list-runtimes",
  :description => "List all runtimes in this environment",
  :fn => ->(*args) { list_runtimes("list-runtimes", *args) }
})

def list_runtimes(cmd_name, *args)
  ensure_docker cmd_name, args
  op = WbOptionsParser.new(cmd_name, args)
  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate

  api_url = get_leo_api_url(gcc.project)
  ServiceAccountContext.new(gcc.project).run do
    common = Common.new
    common.run_inline %W{
      gradle manageLeonardoRuntimes -PappArgs=['list','#{api_url}']
    }
  end
end
```


set-authority
```
def authority_options(cmd_name, args)
  op = WbOptionsParser.new(cmd_name, args)
  op.opts.remove = false
  op.opts.dry_run = false
  op.add_option(
       "--email [EMAIL,...]",
       ->(opts, v) { opts.email = v},
       "Comma-separated list of user accounts to change. Required.")
  op.add_option(
      "--authority [AUTHORITY,...]",
      ->(opts, v) { opts.authority = v},
      "Comma-separated list of user authorities to add or remove for the users. ")
  op.add_option(
      "--remove",
      ->(opts, _) { opts.remove = "true"},
      "Remove authorities (rather than adding them.)")
  op.add_option(
      "--dry_run",
      ->(opts, _) { opts.dry_run = "true"},
      "Make no changes.")
  op.add_validator ->(opts) { raise ArgumentError unless opts.email and opts.authority}
  return op
end

def set_authority(cmd_name, *args)
  ensure_docker cmd_name, args
  op = authority_options(cmd_name, args)
  gcc = GcloudContextV2.new(op)
  op.parse.validate
  gcc.validate

  with_cloud_proxy_and_db(gcc) do
    common = Common.new
    common.run_inline %W{
      gradle setAuthority
     -PappArgs=['#{op.opts.email}','#{op.opts.authority}',#{op.opts.remove},#{op.opts.dry_run}]}
  end
end

Common.register_command({
  :invocation => "set-authority",
  :description => "Set user authorities (permissions). See set-authority --help.",
  :fn => ->(*args) { set_authority("set-authority", *args) }
})
```

- `common.run_inline` specifies which gradle task will be run
- gradle arguments are passed through -PappArgs=[]
- ensure_docker will ensure that your command runs within the docker environment. Every command should include
  this unless there is a specific reason not to.
- GcloudContextV2 is a bit overloaded right now, but its primary purpose is to add the `--project`
argument parser and validate it. It should be added if you're planning to add `ServiceAccountContext`
- Wrapping the gradle call with `ServiceAccountContext` like in list-runtime will ensure that the 
correct `sa-key.json` credentials are set. (The one specified by `--project`)
- Wrapping the gradle call with `with_cloud_proxy_and_db` will allow the tool to connect to
the workbench database corresponding to the `--project` flag.
- Being able to use those two wrappers is one of the best reasons to write a tool. It allows your tool
  to run against any cloud AoU environment by just switching the `--project` flag. *Does not work on local
  environment.
- Things that may change from tool to tool are the argument parsing logic and which context wrapper(s) to use

### build.gradle
```
// Called by devs from the command-line:
// - devstart.rb > list_runtimes
task manageLeonardoRuntimes(type: JavaExec) {
  classpath sourceSets.__tools__.runtimeClasspath
  main = "org.pmiops.workbench.tools.ManageLeonardoRuntimes"
  systemProperties = commandLineSpringProperties
  if (project.hasProperty("appArgs")) {
    args Eval.me(appArgs)
  }
}
```

- The only parts of this that will change for a new tool are the task name and the class name defined by `main`
- It is actually  possible to run your Java tool using just gradle and without the project.rb wrapper. I
prefer this while developing because of the faster cycle time.
  - It is also the only way to run your Java tool against your local environment.
  - ex. `./gradlew setAuthority -PappArgs="['ericsong@fake-research-aou.org','DEVELOPER',false,false]"`
    is the local equivalent of `./project.rb set-authority --project all-of-us-workbench-test --email ericsong@fake-research-aou.org --authority DEVELOPER`
  - It may be necessary to run through the [gradle setup](https://github.com/all-of-us/workbench#api-faster-api-startup-for-macos) 
  for some tasks.

### Java tool

Note: The following code samples were extracted to highlight common patterns in our tools. They're not 
meant to be compilable examples, but you can look up the file in our code base to see the full source code.

```
@Configuration
@Import({
  ActionAuditSpringConfiguration.class,
  AppEngineMetadataSpringConfiguration.class,
  LogsBasedMetricServiceImpl.class,
  MonitoringServiceImpl.class,
  MonitoringSpringConfiguration.class,
  NotebooksServiceImpl.class,
  StackdriverStatsExporterService.class,
  UserRecentResourceServiceImpl.class
})
@ComponentScan(
    value = "org.pmiops.workbench.firecloud",
    excludeFilters =
        // The base CommandlineToolConfig also imports the retry handler, which causes conflicts.
        @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = FirecloudRetryHandler.class))
public class ExportWorkspaceData {

  private static final Logger log = Logger.getLogger(ExportWorkspaceData.class.getName());

  private static Option exportFilenameOpt =
      Option.builder()
          .longOpt("export-filename")
          .desc("Filename of export")
          .required()
          .hasArg()
          .build();
  private static Option includeDemographicsOpt =
      Option.builder()
          .longOpt("include-demographics")
          .desc("Whether to include researcher demographics in the export (sensitive)")
          .build();

  private static Options options =
      new Options().addOption(exportFilenameOpt).addOption(includeDemographicsOpt);

  // Short circuit the DI wiring here with a "mock" WorkspaceService
  // Importing the real one requires importing a large subtree of dependencies
  @Bean
  public WorkspaceService workspaceService() {
    return new WorkspaceServiceFakeImpl();
  }

  @Bean
  ServiceAccountAPIClientFactory serviceAccountAPIClientFactory(WorkbenchConfig config) {
    return new ServiceAccountAPIClientFactory(config.firecloud.baseUrl);
  }

  @Bean
  @Primary
  @Qualifier(FireCloudConfig.END_USER_WORKSPACE_API)
  WorkspacesApi workspaceApi(ServiceAccountAPIClientFactory factory) throws IOException {
    return factory.workspacesApi();
  }

  private WorkspaceDao workspaceDao;
  private CohortDao cohortDao;
  private ConceptSetDao conceptSetDao;
  private DataSetDao dataSetDao;
  private NotebooksService notebooksService;
  private WorkspaceFreeTierUsageDao workspaceFreeTierUsageDao;
  private UserDao userDao;
  private WorkspacesApi workspacesApi;
  private VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao;
  private SimpleDateFormat dateFormat;

  @Bean
  public CommandLineRunner run(
      @Qualifier("entityManagerFactory") EntityManagerFactory emf,
      WorkspaceDao workspaceDao,
      CohortDao cohortDao,
      ConceptSetDao conceptSetDao,
      DataSetDao dataSetDao,
      NotebooksService notebooksService,
      WorkspaceFreeTierUsageDao workspaceFreeTierUsageDao,
      UserDao userDao,
      WorkspacesApi workspacesApi,
      VerifiedInstitutionalAffiliationDao verifiedInstitutionalAffiliationDao) {
    this.workspaceDao = workspaceDao;
    this.cohortDao = cohortDao;
    this.conceptSetDao = conceptSetDao;
    this.dataSetDao = dataSetDao;
    this.notebooksService = notebooksService;
    this.workspaceFreeTierUsageDao = workspaceFreeTierUsageDao;
    this.userDao = userDao;
    this.workspacesApi = workspacesApi;
    this.verifiedInstitutionalAffiliationDao = verifiedInstitutionalAffiliationDao;
    this.dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

    return (args) -> {
      // Binding the EntityManager allows us to use lazy lookups. This simulates what is done on
      // the server, where an entity manager is bound for the duration of a request.
      EntityManagerHolder emHolder = new EntityManagerHolder(emf.createEntityManager());
      TransactionSynchronizationManager.bindResource(emf, emHolder);

      CommandLine opts = new DefaultParser().parse(options, args);
      boolean includeDemographics = opts.hasOption(includeDemographicsOpt.getLongOpt());

      userDao.findAll();
      workspaceDao.findAll();
    };
  }

  public static void main(String[] args) {
    CommandLineToolConfig.runCommandLine(ExportWorkspaceData.class, args);
  }

}
```

- Most of the code here will be specific to each tool but there are still some patterns to
  follow.
- Argument validation and parsing is run using the Option/Options classes from Apache Commons
- Services that the tool needs are specified in the arguments of the `run()` function and provided 
  using Spring dependency injection.
- However, the tools cannot directly use the dependency graph from the standard code base 
  so the implementation for every dependency must be specified for each tool.
- `CommandLineToolConfig` provides many of the common dependencies used in the tools, but it's
likely that you will have to provide some more depending on what services you're injecting.
- In the code example above, there are a few approaches for providing the new 
dependencies.
  - Specify the classes to  import in the `@Import` annotation
  - Specify a package to import using the `@ComponentScan` annotation
  - Define `@Bean`s within the class 
- One useful hack is to provide a nonfunctional "stubbed" out implementation of a service if you 
know that your code will not use it but it needs to be provided to satisfy dependency injection. See
  `workspaceService()` above for an example.

```
@Configuration
public class ManageLeonardoRuntimes {

  private static RuntimesApi newApiClient(String apiUrl) throws IOException {
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(apiUrl);
    apiClient.setAccessToken(
        ServiceAccounts.getScopedServiceAccessToken(Arrays.asList(BILLING_SCOPES)));
    apiClient.setDebugging(true);
    RuntimesApi api = new RuntimesApi();
    api.setApiClient(apiClient);
    return api;
  }

  @Bean
  public CommandLineRunner run() {
    return (args) -> {
      if (args.length < 1) {
        throw new IllegalArgumentException("must specify a command 'list', 'describe' or 'delete'");
      }
      String apiUrl = args[0];

      newApiClient(apiUrl).listRuntimes(null, false).stream()...
    };
  }

  public static void main(String[] args) throws Exception {
    // This tool doesn't currently need database access, so it doesn't extend the
    // CommandLineToolConfig. To add database access, extend from that config and update project.rb
    // to ensure a Cloud SQL proxy is available when this command is run.
    new SpringApplicationBuilder(ManageLeonardoRuntimes.class).web(WebApplicationType.NONE).run(args);
  }
}
```

- Sometimes, it's easier to not inject anything at all and just manually create the service that you need.
This code example demonstrates how to create an API client which is a common use case for the tools.
- In this case, you can forgo the `CommandLineToolConfig` and construct the runner using `SpringApplicationBuilder`
- You're not restricted to using only dependency injection or only manually creating instances. Sometimes
the easiest path forward is to use a combination; import some of the simpler dependencies to 
  instantiate a more complex service.
