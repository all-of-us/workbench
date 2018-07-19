import {Component, DebugElement, Injectable} from '@angular/core';
import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {By} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {
  ActivatedRouteSnapshot,
  Resolve,
  Router,
  RouterStateSnapshot,
} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';

import {ProfileStorageService} from 'app/services/profile-storage.service';
import {ServerConfigService} from 'app/services/server-config.service';
import {ServerConfigServiceStub} from 'testing/stubs/server-config-service-stub';
import {ProfileServiceStub} from 'testing/stubs/profile-service-stub';
import {ProfileStorageServiceStub} from 'testing/stubs/profile-storage-service-stub';
import {UnregisteredComponent} from 'app/views/unregistered/component';
import {updateAndTick} from 'testing/test-helpers';

import {
  ProfileService,
} from 'generated';


@Component({
  selector: 'app-test',
  template: '<router-outlet></router-outlet>'
})
class FakeAppComponent {}

@Component({
  selector: 'app-fake-root',
  template: '<div class="fake-root"></div>'
})
class FakeRootComponent {}

describe('UnregisteredComponent', () => {
  let fixture: ComponentFixture<FakeAppComponent>;
  let de: DebugElement;
  let router: Router;
  let profileStorageStub: ProfileStorageServiceStub;

  beforeEach(fakeAsync(() => {
    profileStorageStub = new ProfileStorageServiceStub();
    TestBed.configureTestingModule({
      declarations: [
        FakeAppComponent,
        FakeRootComponent,
        UnregisteredComponent,
      ],
      imports: [
        BrowserAnimationsModule,
        RouterTestingModule.withRoutes([
          {path: '', component: FakeRootComponent},
          {path: 'unregistered', component: UnregisteredComponent},
        ]),
        ClarityModule.forRoot()
      ],
      providers: [
        {
          provide: ServerConfigService,
          useValue: new ServerConfigServiceStub({
            enforceRegistered: true
          })
        },
        { provide: ProfileService, useValue: profileStorageStub },
        { provide: ProfileStorageService, useValue: new ProfileStorageServiceStub() },
      ]
    }).compileComponents();
  }));

  beforeEach(fakeAsync(() => {
    fixture = TestBed.createComponent(FakeAppComponent);
    de = fixture.debugElement;
    router = TestBed.get(Router);
  }));

  fit('should show unregistered for unregistered', fakeAsync(() => {
    router.navigateByUrl('/unregistered');
    tick();
    fixture.detectChanges();

    expect(de.nativeElement.textContent).toContain('Awaiting identity verification');
  }));
});

