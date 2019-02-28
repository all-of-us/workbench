import {ConfigApi, ConfigResponse} from 'generated/fetch';


export class ConfigApiStub extends ConfigApi {

  config: ConfigResponse;

  constructor() {
    super(undefined, undefined, (..._: any[]) => {
      throw Error('cannot fetch in tests');
    });
    this.config = <ConfigResponse>{
      gsuiteDomain: 'test',
      projectId: 'test',
      enforceRegistered: true,
    };
  }

  public getConfig() {
    return Promise.resolve(this.config);
  }
}
