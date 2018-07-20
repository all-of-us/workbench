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
import {UnregisteredComponent} from 'app/views/unregistered/component';
import {ProfileServiceStub, ProfileStubVariables} from 'testing/stubs/profile-service-stub';
import {ProfileStorageServiceStub} from 'testing/stubs/profile-storage-service-stub';
import {ServerConfigServiceStub} from 'testing/stubs/server-config-service-stub';
import {updateAndTick} from 'testing/test-helpers';

import {
  DataAccessLevel,
  IdVerificationStatus,
  Profile,
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
  let profileStub: ProfileServiceStub;
  let profileStorageStub: ProfileStorageServiceStub;

  beforeEach(fakeAsync(() => {
    profileStub = new ProfileServiceStub();
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
            gsuiteDomain: '',
            enforceRegistered: true
          })
        },
        { provide: ProfileService, useValue: profileStub },
        { provide: ProfileStorageService, useValue: profileStorageStub },
      ]
    }).compileComponents();
  }));

  beforeEach(fakeAsync(() => {
    fixture = TestBed.createComponent(FakeAppComponent);
    de = fixture.debugElement;
    router = TestBed.get(Router);

    router.navigateByUrl('/unregistered');
    tick();
    fixture.detectChanges();
  }));

  const loadProfileWithRegistrationSettings = (p: any) => {
    const profile = {
      ...ProfileStubVariables.PROFILE_STUB,
      dataAccessLevel: p.dataAccessLevel,
      idVerificationStatus: p.idVerificationStatus,
      requestedIdVerification: !!p.requestedIdVerification,
      termsOfServiceCompletionTime: p.termsOfServiceCompletionTime,
      ethicsTrainingCompletionTime: p.ethicsTrainingCompletionTime,
      demographicSurveyCompletionTime: p.demographicSurveyCompletionTime,
    };
    profileStub.profile = profile;
    profileStorageStub.profile.next(profile);
  };

  it('should show unregistered for unregistered', fakeAsync(() => {
    loadProfileWithRegistrationSettings({
      dataAccessLevel: DataAccessLevel.Unregistered
    });

    expect(de.nativeElement.textContent).toContain('Awaiting identity verification');
  }));

  it('should submit incomplete registration steps', fakeAsync(() => {
    loadProfileWithRegistrationSettings({
      dataAccessLevel: DataAccessLevel.Unregistered,
      idVerificationStatus: IdVerificationStatus.UNVERIFIED,
      requestedIdVerification: false,
    });

    expect(de.nativeElement.textContent).toContain('Awaiting identity verification');
    expect(profileStub.profile.requestedIdVerification).toBeTruthy();
    expect(profileStub.profile.termsOfServiceCompletionTime).toBeTruthy();
    expect(profileStub.profile.ethicsTrainingCompletionTime).toBeTruthy();
    expect(profileStub.profile.demographicSurveyCompletionTime).toBeTruthy();
  }));

  it('should navigate away for registered users', fakeAsync(() => {
    loadProfileWithRegistrationSettings({
      dataAccessLevel: DataAccessLevel.Registered
    });
    tick();
    fixture.detectChanges();

    expect(de.queryAll(By.css('app-fake-root')).length).toEqual(1);
  }));
});

