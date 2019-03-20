import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';

import {ClarityModule} from '@clr/angular';

import {
  AuthDomainService,
  ProfileService,
} from 'generated';

import {ProfileServiceStub} from 'testing/stubs/profile-service-stub';
import {ServerConfigServiceStub} from 'testing/stubs/server-config-service-stub';

import {ServerConfigService} from 'app/services/server-config.service';
import {
  setupModals,
  updateAndTick
} from 'testing/test-helpers';

import {AdminUserComponent} from 'app/views/admin-user/component';

describe('AdminUserComponent', () => {
  let fixture: ComponentFixture<AdminUserComponent>;
  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        ClarityModule.forRoot()
      ],
      declarations: [
        AdminUserComponent
      ],
      providers: [
        { provide: AuthDomainService, useValue: {} },
        { provide: ProfileService, useValue: new ProfileServiceStub() },
        {
          provide: ServerConfigService,
          useValue: new ServerConfigServiceStub({
            gsuiteDomain: 'fake-research-aou.org'
          })
        }
      ]
    }).compileComponents().then(() => {
      fixture = TestBed.createComponent(AdminUserComponent);
      setupModals(fixture);
      tick();
    });
  }));

  it('should render', fakeAsync(() => {
    updateAndTick(fixture);
    expect(fixture).toBeTruthy();
  }));
});
