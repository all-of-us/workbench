import {fakeAsync, TestBed, tick} from '@angular/core/testing';
import {Observable} from 'rxjs/Observable';
import {Subscriber} from 'rxjs/Subscriber';

import {ServerConfigService} from './server-config.service';

import {ConfigResponse, ConfigService} from 'generated';

const testConfig: ConfigResponse = {
  gsuiteDomain: 'fake-domain',
  publicApiKeyForErrorReports: '123',
  projectId: 'foo'
};

class ConfigServiceStub {
  calls = 0;
  obsResp: Observable<ConfigResponse>;

  getConfig(config: ConfigResponse): Observable<ConfigResponse> {
    this.calls++;
    if (this.obsResp) {
      return this.obsResp;
    }
    return new Observable<ConfigResponse>(s => {
      setTimeout(() => {
        s.next(testConfig);
        s.complete();
      });
    });
  }
}

describe('ServerConfigService', () => {
  let service: ServerConfigService;
  let configServiceStub: ConfigServiceStub;
  beforeEach(fakeAsync(() => {
    configServiceStub = new ConfigServiceStub();
    TestBed.configureTestingModule({
      providers: [
        { provide: ConfigService, useValue: configServiceStub },
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
    expect(configServiceStub.calls).toEqual(1);
    getConfigWithTick();
    expect(configServiceStub.calls).toEqual(1);
  }));

  it('dedupes staggered multi-access', fakeAsync(() => {
    const subs: Array<Subscriber<ConfigResponse>> = [];
    configServiceStub.obsResp = new Observable<ConfigResponse>(s => {
      subs.push(s);
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
    expect(subs.length).toBe(1);
    subs.forEach(s => {
      s.next(testConfig);
      s.complete();
    });
    tick();
    expect(gotBefore1).toEqual(testConfig);
    expect(gotBefore2).toEqual(testConfig);

    let gotAfter: ConfigResponse;
    service.getConfig().subscribe(c => {
      gotAfter = c;
    });
    expect(gotAfter).toEqual(testConfig);
    expect(configServiceStub.calls).toEqual(1);
  }));
});
