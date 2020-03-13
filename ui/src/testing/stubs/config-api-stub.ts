import {
  ConfigApi,
  ConfigResponse
} from 'generated/fetch';
import defaultServerConfig from 'testing/default-server-config';


export class ConfigApiStub extends ConfigApi {
  public config: ConfigResponse;

  constructor(configOverrides: Partial<ConfigResponse> = {}) {
    super(undefined, undefined, (..._: any[]) => { throw Error('cannot fetch in tests'); });
    this.config = {...defaultServerConfig, ...configOverrides};
  }

  getConfig(): Promise<ConfigResponse> {
    return new Promise(resolve => {
      resolve(this.config);
    });
  }
}
