import {async, ComponentFixture, fakeAsync, TestBed} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {RouterTestingModule} from '@angular/router/testing';

import {ClarityModule} from 'clarity-angular';

import {ErrorHandlingService} from '../../services/error-handling.service';
import {SignInService} from '../../services/sign-in.service';
import {AccountCreationComponent} from '../account-creation/component';
import {InvitationCodeComponent} from '../invitation-code/component';

import {ErrorHandlingServiceStub} from 'testing/stubs/error-handling-service-stub';
import {ProfileServiceStub} from 'testing/stubs/profile-invitation-code-service-stub';

import {CohortsService, ProfileService} from 'generated';
import {
    queryAllByCss, queryByCss, simulateClick,
    updateAndTick
} from '../../../testing/test-helpers';
import {WorkspacesService} from '../../../generated';
import {DebugElement} from '@angular/core';
import {UrlSegment} from '@angular/router';
import {HomePageComponent} from '../home-page/component';



class InvitationCodePage {
    fixture: ComponentFixture<HomePageComponent>;
    route: UrlSegment[];
    nextButton: DebugElement;

    constructor(testBed: typeof TestBed) {
        this.fixture = testBed.createComponent(HomePageComponent);
        this.readPageData();
    }

    readPageData() {
        updateAndTick(this.fixture);
        updateAndTick(this.fixture);
        this.nextButton = queryByCss(this.fixture, '#next');

    }
}
describe('InvitationCodeComponent', () => {
let bugReportPage : InvitationCodePage;
  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        FormsModule,
        ClarityModule.forRoot()
      ],
      declarations: [
          AccountCreationComponent,
          InvitationCodeComponent
      ],
      providers: [
        { provide: ErrorHandlingService, useValue: new ErrorHandlingServiceStub() },
        { provide: SignInService, useValue: {} },
        { provide: ProfileService, useValue: new ProfileServiceStub() }
      ] }).compileComponents(
        bugReportPage = new InvitationCodePage(TestBed);
    );
  }));

  it('should create the app', async(() => {
    const fixture = TestBed.createComponent(InvitationCodeComponent);
    const app = fixture.debugElement.componentInstance;
    expect(app).toBeTruthy();
    expect(app.invitationSucc).toBeFalsy();

      expect(app.invitationKeyReq).toBeFalsy();

      expect(app.invitationKeyInvalid).toBeFalsy();

  }));

    /*it('submits a bug report', fakeAsync(() => {
        simulateClick(bugReportPage.fixture, bugReportPage.nextButton);

    }));
*/
});