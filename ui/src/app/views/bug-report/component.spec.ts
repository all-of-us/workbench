import {DebugElement} from '@angular/core';
import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';

import {BugReportComponent} from 'app/views/bug-report/component';
import {BugReportServiceStub} from 'testing/stubs/bug-report-service-stub';
import {ProfileServiceStub, ProfileStubVariables} from 'testing/stubs/profile-service-stub';
import {
  queryByCss,
  simulateClick,
  simulateInput,
  updateAndTick
} from 'testing/test-helpers';

import {BugReportService, ProfileService} from 'generated';


class BugReportPage {
  fixture: ComponentFixture<BugReportComponent>;
  bugReportService: BugReportService;
  reportBugButton: DebugElement;
  sendButton: DebugElement;
  shortDescription: DebugElement;
  reproSteps: DebugElement;

  constructor(testBed: typeof TestBed) {
    this.fixture = testBed.createComponent(BugReportComponent);
    this.bugReportService = this.fixture.debugElement.injector.get(BugReportService);
    this.readPageData();
  }

  readPageData() {
    updateAndTick(this.fixture);
    updateAndTick(this.fixture);
    this.reportBugButton = queryByCss(this.fixture, '#report-bug');
    this.sendButton = queryByCss(this.fixture, '#send-bug-report');
    this.shortDescription = queryByCss(this.fixture, '#bug-report-short-descr');
    this.reproSteps = queryByCss(this.fixture, '#bug-report-repro-steps');
  }
}

describe('BugReportComponent', () => {
  let bugReportPage: BugReportPage;
  const testShortDescription = 'Short Description';
  const testReproSteps = 'Repro Steps';
  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        BrowserAnimationsModule,
        FormsModule,
        ClarityModule.forRoot()
      ],
      declarations: [
        BugReportComponent
      ],
      providers: [
        { provide: BugReportService, useValue: new BugReportServiceStub() },
        { provide: ProfileService, useValue: new ProfileServiceStub() }
      ] }).compileComponents().then(() => {
        bugReportPage = new BugReportPage(TestBed);
      });
      tick();
  }));


  it('submits a bug report', fakeAsync(() => {
    simulateClick(bugReportPage.fixture, bugReportPage.reportBugButton);
    bugReportPage.readPageData();
    simulateInput(bugReportPage.fixture, bugReportPage.shortDescription, testShortDescription);
    simulateInput(bugReportPage.fixture, bugReportPage.reproSteps, testReproSteps);
    bugReportPage.readPageData();
    simulateClick(bugReportPage.fixture, bugReportPage.sendButton);
    expect(bugReportPage.fixture.componentInstance.bugReport.shortDescription)
      .toBe(testShortDescription);
    expect(bugReportPage.fixture.componentInstance.bugReport.reproSteps).toBe(testReproSteps);
    expect(bugReportPage.fixture.componentInstance.bugReport.contactEmail)
      .toBe(ProfileStubVariables.PROFILE_STUB.contactEmail);
  }));


});
