import {Component, DebugElement} from '@angular/core';
import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {By} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {
  Router,
} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';
import {Observable} from 'rxjs/Observable';

import {ProfileStorageService} from 'app/services/profile-storage.service';
import {ServerConfigService} from 'app/services/server-config.service';
import {UnregisteredComponent} from 'app/views/unregistered/component';
import {ProfileServiceStub, ProfileStubVariables} from 'testing/stubs/profile-service-stub';
import {ProfileStorageServiceStub} from 'testing/stubs/profile-storage-service-stub';
import {ServerConfigServiceStub} from 'testing/stubs/server-config-service-stub';

import {
  DataAccessLevel,
  IdVerificationStatus,
  Profile,
  ProfileService,
} from 'generated';


class FailingProfileStub extends ProfileServiceStub {
  constructor(private fails: number) {
    super();
  }

  submitTermsOfService(extraHttpRequestParams?: any): Observable<Profile> {
    this.fails--;
    if (this.fails >= 0) {
      throw {status: 409};
    }
    return super.submitTermsOfService(extraHttpRequestParams);
  }
}

class TrainingCompletesRegistrationStub extends ProfileServiceStub {
  completeEthicsTraining(extraHttpRequestParams?: any): Observable<Profile> {
    const obs = super.completeEthicsTraining(extraHttpRequestParams);
    this.profile.dataAccessLevel = DataAccessLevel.Registered;
    return obs;
  }
}

const pendingText = 'pending verification';

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
  let unregisteredComponent: UnregisteredComponent;
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

    // Grab the rendered component from within the fake app container. We don't
    // specify a custom selector so it gets rendered as 'ng-component'.
    unregisteredComponent = de.query(By.css('ng-component')).componentInstance;
  }));

  const loadProfileWithRegistrationSettings = (p: any) => {
    const profile = {
      ...ProfileStubVariables.PROFILE_STUB,
      dataAccessLevel: p.dataAccessLevel,
      idVerificationStatus: p.idVerificationStatus || IdVerificationStatus.UNVERIFIED,
      requestedIdVerification: !!p.requestedIdVerification,
      termsOfServiceCompletionTime: p.termsOfServiceCompletionTime,
      ethicsTrainingCompletionTime: p.ethicsTrainingCompletionTime,
      demographicSurveyCompletionTime: p.demographicSurveyCompletionTime,
    };
    profileStub.profile = profile;
    profileStorageStub.profile.next(profile);
  };

  const expectAllRegistrationSubmitted = (p: Profile) => {
    expect(p.requestedIdVerification).toBeTruthy();
    expect(p.termsOfServiceCompletionTime).toBeTruthy();
    expect(p.trainingCompletionTime).toBeTruthy();
    expect(p.demographicSurveyCompletionTime).toBeTruthy();
  };

  it('should show unregistered for unregistered', fakeAsync(() => {
    loadProfileWithRegistrationSettings({
      dataAccessLevel: DataAccessLevel.Unregistered
    });
    tick();
    fixture.detectChanges();

    expect(de.nativeElement.textContent).toContain(pendingText);
  }));

  it('should submit incomplete registration steps', fakeAsync(() => {
    loadProfileWithRegistrationSettings({
      dataAccessLevel: DataAccessLevel.Unregistered,
      idVerificationStatus: IdVerificationStatus.UNVERIFIED,
      requestedIdVerification: false,
    });

    expectAllRegistrationSubmitted(profileStub.profile);
  }));

  it('should retry failed registration steps', fakeAsync(() => {
    profileStub = new FailingProfileStub(2);
    unregisteredComponent.setProfileService(profileStub);

    loadProfileWithRegistrationSettings({
      dataAccessLevel: DataAccessLevel.Unregistered,
      idVerificationStatus: IdVerificationStatus.UNVERIFIED,
      requestedIdVerification: false,
    });

    // Tick the retry delays.
    tick(1000);
    tick(1000);
    fixture.detectChanges();

    expect(de.nativeElement.textContent).toContain(pendingText);
    expectAllRegistrationSubmitted(profileStub.profile);
  }));

  it('should redirect if profile becomes completed', fakeAsync(() => {
    profileStub = new TrainingCompletesRegistrationStub();
    unregisteredComponent.setProfileService(profileStub);

    loadProfileWithRegistrationSettings({
      dataAccessLevel: DataAccessLevel.Unregistered,
      idVerificationStatus: IdVerificationStatus.UNVERIFIED,
      requestedIdVerification: false,
    });

    tick();
    fixture.detectChanges();

    expect(de.queryAll(By.css('app-fake-root')).length).toEqual(1);
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

