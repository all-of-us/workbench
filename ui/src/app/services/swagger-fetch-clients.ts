/**
 * This package configures/provides swagger "typescript-fetch" client
 * implementations. This layer allows for abstraction of client implementations
 * for use in React without requiring a full-blown dependency injection
 * solution. This layer can be configured for ease of testing. For discussion on
 * this, see https://github.com/all-of-us/workbench/pull/1663.
 *
 * Example usage:
 *
 * import {runtimeApi} from 'app/services/swagger-fetch-clients';
 *
 * ...
 * runtimeApi().listRuntimes();
 *
 * Example test usage:
 *
 * beforeEach(() => {
 *   registerApiClient(ProfileApi, new ProfileApiStub());
 *   ...
 * });
 */

import {cookiesEnabled, LOCAL_STORAGE_API_OVERRIDE_KEY} from 'app/utils';
import {environment} from 'environments/environment';
import {
  AuthDomainApi,
  BaseAPI,  // internal
  CdrVersionsApi,
  CohortAnnotationDefinitionApi,
  CohortBuilderApi,
  CohortReviewApi,
  CohortsApi,
  ConceptSetsApi,
  ConfigApi,
  Configuration as FetchConfiguration,
  DataSetApi,
  FeaturedWorkspacesConfigApi,
  InstitutionApi, // internal
  ProfileApi,
  RuntimeApi,
  StatusAlertApi,
  StatusApi,
  UserApi,
  UserMetricsApi,
  WorkspaceAdminApi,
  WorkspacesApi,
} from 'generated/fetch';

import * as portableFetch from 'portable-fetch';


let frozen = false;
function checkFrozen() {
  if (frozen) {
    throw Error('API clients registry is already frozen; cannot be ' +
        'configured after invocation of bindApiClients()');
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
// getters for the API clients, e.g.: runtimeApi().listRuntimes();
export const authDomainApi = bindCtor(AuthDomainApi);
export const cdrVersionsApi = bindCtor(CdrVersionsApi);
export const configApi = bindCtor(ConfigApi);
export const runtimeApi = bindCtor(RuntimeApi);
export const cohortAnnotationDefinitionApi = bindCtor(CohortAnnotationDefinitionApi);
export const cohortBuilderApi = bindCtor(CohortBuilderApi);
export const cohortReviewApi = bindCtor(CohortReviewApi);
export const cohortsApi = bindCtor(CohortsApi);
export const conceptSetsApi = bindCtor(ConceptSetsApi);
export const dataSetApi = bindCtor(DataSetApi);
export const featuredWorkspacesConfigApi = bindCtor(FeaturedWorkspacesConfigApi);
export const institutionApi = bindCtor(InstitutionApi);
export const profileApi = bindCtor(ProfileApi);
export const statusApi = bindCtor(StatusApi);
export const statusAlertApi = bindCtor(StatusAlertApi);
export const userApi = bindCtor(UserApi);
export const userMetricsApi = bindCtor(UserMetricsApi);
export const workspaceAdminApi = bindCtor(WorkspaceAdminApi);
export const workspacesApi = bindCtor(WorkspacesApi);

export const getApiBaseUrl = () => {
  if (cookiesEnabled()) {
    return localStorage.getItem(LOCAL_STORAGE_API_OVERRIDE_KEY) || environment.allOfUsApiUrl;
  } else {
    return environment.allOfUsApiUrl;
  }
};

// ConfigApi gets special treatment, since it's needed to bootstrap all of the
// other API services: these require an access token, which in turn requires the
// oauth client id, which is currently returned by the Config API.
registerApiClient(ConfigApi, new class extends ConfigApi {
  constructor() {
    super();
    this.configuration = new FetchConfiguration({
      basePath: getApiBaseUrl()
    });
    this.basePath = getApiBaseUrl();
    this.fetch = portableFetch;
  }
});

/**
 * Binds standard API clients. To be called at most once for production use,
 * e.g. during app initialization.
 */
export function bindApiClients(conf: FetchConfiguration) {
  for (const ctor of apiCtors) {
    // We use an anonymous subclass here because Swagger's typescript-fetch
    // codegen creates API client subclasses which lack a public interface for
    // configuration. Configuration of creds and basePath are only accessible on
    // the parent BaseAPI via protected properties.
    registerApiClient(ctor, new class extends ctor {
      constructor() {
        super();
        this.configuration = conf;
        this.basePath = conf.basePath;
        this.fetch = portableFetch;
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
