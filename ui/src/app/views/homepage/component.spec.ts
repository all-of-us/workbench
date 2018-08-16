import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {RouterTestingModule} from '@angular/router/testing';

import {ClarityModule} from '@clr/angular';

import {ProfileService} from 'generated';

import {ProfileServiceStub} from 'testing/stubs/profile-service-stub';
import {ProfileStorageServiceStub} from 'testing/stubs/profile-storage-service-stub';
import {ServerConfigServiceStub} from 'testing/stubs/server-config-service-stub';

import {
  updateAndTick
} from '../../../testing/test-helpers';

import {ProfileStorageService} from '../../services/profile-storage.service';
import {ServerConfigService} from '../../services/server-config.service';

import {HomepageComponent} from '../homepage/component';

describe('HomepageComponent', () => {
  let fixture: ComponentFixture<HomepageComponent>;
  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        ClarityModule.forRoot()
      ],
      declarations: [
        HomepageComponent,
      ],
      providers: [
        {provide: ProfileService, useValue: new ProfileServiceStub()},
        {provide: ProfileStorageService, useValue: new ProfileStorageServiceStub()},
        {
          provide: ServerConfigService,
          useValue: new ServerConfigServiceStub({
            gsuiteDomain: 'fake-research-aou.org'
          })
        },
      ]
    }).compileComponents().then(() => {
      fixture = TestBed.createComponent(HomepageComponent);
      tick();
    });
  }));

  it('should render', fakeAsync(() => {
    updateAndTick(fixture);
    expect(fixture).toBeTruthy();
  }));
});
