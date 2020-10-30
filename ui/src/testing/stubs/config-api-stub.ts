import {
  ConfigApi,
  ConfigResponse
} from 'generated/fetch';
import defaultServerConfig from 'testing/default-server-config';
import {StubImplementationRequired} from 'testing/stubs/stub-utils';

export class ConfigApiStub extends ConfigApi {
  public config: ConfigResponse;

  constructor(configOverrides: Partial<ConfigResponse> = {}) {
    super(undefined, undefined, (..._: any[]) => { throw StubImplementationRequired; });
    this.config = {...defaultServerConfig, ...configOverrides};
  }

  getConfig(): Promise<ConfigResponse> {
    return new Promise(resolve => {
      resolve(this.config);
    });
  }
}
