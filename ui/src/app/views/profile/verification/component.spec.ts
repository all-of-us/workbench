import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {Http} from '@angular/http';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';
import {ProfilePageComponent} from './component';

import {IconsModule} from 'app/icons/icons.module';
import {ErrorHandlingService} from 'app/services/error-handling.service';
import {ProfileService} from 'generated';
import {ErrorHandlingServiceStub} from 'testing/stubs/error-handling-service-stub';
import {HttpStub} from 'testing/stubs/http-stub';
import {ProfileServiceStub} from 'testing/stubs/profile-service-stub';
import {updateAndTick} from 'testing/test-helpers';

class ProfilePage {
  fixture: ComponentFixture<ProfilePageComponent>;
  profileService: ProfileService;

  constructor(testBed: typeof TestBed) {
    this.fixture = testBed.createComponent(ProfilePageComponent);
    this.profileService = this.fixture.debugElement.injector.get(ProfileService);
    this.readPageData();
  }

  readPageData() {
    updateAndTick(this.fixture);
    updateAndTick(this.fixture);
  }
}

describe('ProfileComponent', () => {
  let profilePage: ProfilePage;
  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        FormsModule,
        RouterTestingModule,
        IconsModule,
        ClarityModule.forRoot()
      ],
      declarations: [
        ProfilePageComponent
      ],
      providers: [
        { provide: ErrorHandlingService, useValue: new ErrorHandlingServiceStub() },
        { provide: Http, useValue: new HttpStub() },
        { provide: ProfileService, useValue: new ProfileServiceStub()}
      ] }).compileComponents().then(() => {
      profilePage = new ProfilePage(TestBed);
    });
    tick();
  }));

  it('calculates Progress information correctly', fakeAsync(() => {
    const fixture = profilePage.fixture;
    const app = fixture.debugElement.componentInstance;
    tick();
    expect(app.progressPercent).toBe('50%');
    expect(app.progressCount).toBe(2);
  }));
});
