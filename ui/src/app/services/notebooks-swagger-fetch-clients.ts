/**
 * Duplicated from swagger-fetch-clients.ts
 *
 * It was a lot easier to recreate this file for the Notebooks fetch client instead of modifying
 * the existing one to service both fetch clients. There's probably some work to be done to
 * still share some code between this file and swagger-fetch-client.ts.
 */

import {
  BaseAPI,
  ClusterApi,
  Configuration as FetchConfiguration,
  FetchAPI, JupyterApi, NotebooksApi
} from 'notebooks-generated/fetch';

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
export const notebooksClusterApi = bindCtor(ClusterApi);
export const notebooksApi = bindCtor(NotebooksApi);
export const jupyterApi = bindCtor(JupyterApi);

export function bindApiClients(conf: FetchConfiguration, f: FetchAPI) {
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
        this.fetch = f;
      }
    }());
  }
  frozen = true;
}

export function registerApiClient<T extends BaseAPI>(ctor: new() => T, impl: T) {
  checkFrozen();
  registry.set(ctor, impl);
}

export function clearApiClients() {
  checkFrozen();
  registry.clear();
}
