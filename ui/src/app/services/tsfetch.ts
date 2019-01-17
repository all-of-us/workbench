import {
  AuditApi,
  AuthDomainApi,
  BaseAPI,
  BugReportApi,
  CdrVersionsApi,
  ClusterApi,
  CohortAnnotationDefinitionApi,
  CohortBuilderApi,
  CohortReviewApi,
  CohortsApi,
  ConceptsApi,
  ConceptSetsApi,
  ConfigApi,
  Configuration as FetchConfiguration,
  CronApi,
  FetchAPI,
  OfflineClusterApi,
  ProfileApi,
  StatusApi,
  UserApi,
  UserMetricsApi,
  WorkspacesApi,
} from 'generated/fetch';


let frozen = false;
function checkFrozen() {
  if (frozen) {
    throw Error('tsfetch registry is already frozen; bindClients() should only be called onced');
  }
}

// All known API client constructors.
const apiCtors: (new() => BaseAPI)[] = [];

// Constructor -> implementation.
const registry: Map<new() => BaseAPI, BaseAPI> = new Map();

/**
 * Convenience function to minimize boilerplate below per-service while
 * maintaining the API client type. Also registers the constructor for standard
 * client initialization later.
 *
 * Returns a getter for the service implementation (backed by the registry).
 */
function bindCtor<T extends BaseAPI>(ctor: new() => T): () => T {
  apiCtors.push(ctor);
  return () => {
    if (!registry.has(ctor)) {
      throw Error('API client is not registered: ensure you are not ' +
                  'retrieving an API client before app initialization. In ' +
                  'unit tests, be sure to call registerApiClient() for all ' +
                  'API clients in use, else call bindApiClients(): ' + ctor);
    }
    return registry.get(ctor) as T;
  };
}

// To add a new service, add a new entry below. Note that these properties are
// getters for the API clients, e.g.: clusterApi().listClusters();
export const auditApi = bindCtor(AuditApi);
export const authDomainApi = bindCtor(AuthDomainApi);
export const bugReportApi = bindCtor(BugReportApi);
export const cdrVersionsApi = bindCtor(CdrVersionsApi);
export const clusterApi = bindCtor(ClusterApi);
export const cohortAnnotationDefinitionApi = bindCtor(CohortAnnotationDefinitionApi);
export const cohortBuilderApi = bindCtor(CohortBuilderApi);
export const cohortReviewApi = bindCtor(CohortReviewApi);
export const cohortsApi = bindCtor(CohortsApi);
export const conceptsApi = bindCtor(ConceptsApi);
export const conceptSetsApi = bindCtor(ConceptSetsApi);
export const configApi = bindCtor(ConfigApi);
export const cronApi = bindCtor(CronApi);
export const offlineClusterApi = bindCtor(OfflineClusterApi);
export const profileApi = bindCtor(ProfileApi);
export const statusApi = bindCtor(StatusApi);
export const userApi = bindCtor(UserApi);
export const userMetricsApi = bindCtor(UserMetricsApi);
export const workspacesApi = bindCtor(WorkspacesApi);

/**
 * Binds standard API clients. To be called at most once for production use,
 * e.g. during app initialization.
 */
export function bindApiClients(conf: FetchConfiguration, f: FetchAPI) {
  for (const ctor of apiCtors) {
    // We use an anonymous subclass here because ts-fetch generates API client
    // classes with default ctor's only. BaseAPI functionality is only
    // accessible on protected properties.
    registerApiClient(ctor, new class extends ctor {
      constructor() {
        super();
        this.configuration = conf;
        this.basePath = conf.basePath;
        this.fetch = f;
      }
    }());
  }
  frozen = true;
}

/**
 * Registers an API client implementation. Can be used to bind a non-standard
 * API implementation, e.g. for testing.
 */
export function registerApiClient<T extends BaseAPI>(ctor: new() => T, impl: T) {
  checkFrozen();
  registry.set(ctor, impl);
}

/**
 * Clears the API client registry, e.g. for test teardown.
 */
export function clearApiClients() {
  checkFrozen();
  registry.clear();
}
