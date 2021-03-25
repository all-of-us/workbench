import {fakeAsync, TestBed, tick} from '@angular/core/testing';

import {ServerConfigService} from 'app/services/server-config.service';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {stubNotImplementedError} from 'testing/stubs/stub-utils';

import {ConfigApi, ConfigResponse} from 'generated/fetch';

const testConfig: ConfigResponse = {
  gsuiteDomain: 'fake-domain',
  publicApiKeyForErrorReports: '123',
  projectId: 'foo'
};

class ConfigApiStub extends ConfigApi {
  promise: Promise<ConfigResponse>;
  calls = 0;

  constructor() {
    super(undefined, undefined, (..._: any[]) => { throw stubNotImplementedError; });
  }

  getConfig(): Promise<ConfigResponse> {
    this.calls++;
    if (this.promise) {
      return this.promise;
    }
    return new Promise<ConfigResponse>(accept => {
      setTimeout(() => accept(testConfig));
    });
  }
}

describe('ServerConfigService', () => {
  let service: ServerConfigService;
  let configApiStub: ConfigApiStub;
  beforeEach(fakeAsync(() => {
    configApiStub = new ConfigApiStub();
    registerApiClient(ConfigApi, configApiStub);
    TestBed.configureTestingModule({
      providers: [
        ServerConfigService
      ]
    });
    service = TestBed.get(ServerConfigService);
  }));

  it('dedupes simple multi-access', fakeAsync(() => {
    const getConfigWithTick = () => {
      let got: ConfigResponse;
      service.getConfig().subscribe(c => {
        got = c;
      });
      tick();
      expect(got).toEqual(testConfig);
    };

    getConfigWithTick();
    expect(configApiStub.calls).toEqual(1);
    getConfigWithTick();
    expect(configApiStub.calls).toEqual(1);
  }));

  it('dedupes staggered multi-access', fakeAsync(() => {
    let fireXhr;
    configApiStub.promise = new Promise(accept => {
      fireXhr = () => {
        setTimeout(() => accept(testConfig), 0);
      };
    });

    let gotBefore1, gotBefore2: ConfigResponse;
    service.getConfig().subscribe(c => {
      gotBefore1 = c;
    });
    service.getConfig().subscribe(c => {
      gotBefore2 = c;
    });
    tick();

    // Simulate XHR firing.
    fireXhr(testConfig);
    tick();
    expect(gotBefore1).toEqual(testConfig);
    expect(gotBefore2).toEqual(testConfig);

    let gotAfter: ConfigResponse;
    service.getConfig().subscribe(c => {
      gotAfter = c;
    });
    expect(gotAfter).toEqual(testConfig);
    expect(configApiStub.calls).toEqual(1);
  }));
});
