import {AccountCreationComponent} from '../account-creation/component';
import {ClarityModule} from 'clarity-angular';
import {ComponentFixture, fakeAsync, TestBed,tick} from '@angular/core/testing';
import {DebugElement} from '@angular/core';
import {ErrorHandlingService} from '../../services/error-handling.service';
import {ErrorHandlingServiceStub} from 'testing/stubs/error-handling-service-stub';
import {FormsModule} from '@angular/forms';
import {InvitationCodeComponent} from '../invitation-code/component';
import {ProfileService} from 'generated';
import {ProfileServiceStub} from 'testing/stubs/profile-service-stub';
import {
    queryByCss, simulateClick,
    updateAndTick
} from '../../../testing/test-helpers';
import {RouterTestingModule} from '@angular/router/testing';
import {SignInService} from '../../services/sign-in.service';
import {UrlSegment} from '@angular/router';




class InvitationCodePage {
    fixture: ComponentFixture<InvitationCodeComponent>;
    route: UrlSegment[];
    nextButton: DebugElement;

    constructor(testBed: typeof TestBed) {
        this.fixture = testBed.createComponent(InvitationCodeComponent);
        this.readPageData();
    }

    readPageData() {
        updateAndTick(this.fixture);
        updateAndTick(this.fixture);
        this.nextButton = queryByCss(this.fixture, '#next');

    }
}
describe('InvitationCodeComponent', () => {
    let invitationCodePage: InvitationCodePage;

    beforeEach(fakeAsync(() => {
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
      ] }).compileComponents().then(() => {
        invitationCodePage = new InvitationCodePage(TestBed);
    });
    tick();
  }));

  it('should create the app', fakeAsync(() => {
    const fixture = TestBed.createComponent(InvitationCodeComponent);
    const app = fixture.debugElement.componentInstance;
    expect(app).toBeTruthy();
    expect(app.invitationSucc).toBeFalsy();
    expect(app.invitationKeyReq).toBeFalsy();
    expect(app.invitationKeyInvalid).toBeFalsy();

  }));


    it('required invitation code', fakeAsync(() => {

        simulateClick(invitationCodePage.fixture, invitationCodePage.nextButton);
        const app = invitationCodePage.fixture.debugElement.componentInstance;

        expect(app.invitationSucc).toBeFalsy();
        expect(app.invitationKeyReq).toBeTruthy();
        expect(app.invitationKeyInvalid).toBeFalsy();
    }));
    it('invalid invitation code', fakeAsync(() => {

        const app = invitationCodePage.fixture.debugElement.componentInstance;
        app.invitationKey = 'invalid';

        simulateClick(invitationCodePage.fixture, invitationCodePage.nextButton);
        expect(app.invitationSucc).toBeFalsy();
        expect(app.invitationKeyReq).toBeFalsy();
        expect(app.invitationKeyInvalid).toBeTruthy();
    }));

    it('correct invitation code', fakeAsync(() => {

        const app = invitationCodePage.fixture.debugElement.componentInstance;
        app.invitationKey = 'dummy';

        simulateClick(invitationCodePage.fixture, invitationCodePage.nextButton);
        expect(app.invitationSucc).toBeTruthy();
        expect(app.invitationKeyReq).toBeFalsy();
        expect(app.invitationKeyInvalid).toBeFalsy();
    }));

});
