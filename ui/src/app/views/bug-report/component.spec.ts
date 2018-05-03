import {DebugElement} from '@angular/core';
import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {By} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';

import {ProfileStorageService} from 'app/services/profile-storage.service';
import {BugReportComponent} from 'app/views/bug-report/component';

import {BugReportServiceStub} from 'testing/stubs/bug-report-service-stub';
import {ProfileStubVariables} from 'testing/stubs/profile-service-stub';
import {ProfileStorageServiceStub} from 'testing/stubs/profile-storage-service-stub';
import {
  simulateClick,
  simulateInput,
  updateAndTick
} from 'testing/test-helpers';

import {BugReportService} from 'generated';


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
    const de = this.fixture.debugElement;
    this.sendButton = de.query(By.css('#send-bug-report'));
    this.shortDescription = de.query(By.css('#bug-report-short-descr'));
    this.reproSteps = de.query(By.css('#bug-report-repro-steps'));
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
        { provide: ProfileStorageService, useValue: new ProfileStorageServiceStub() },
      ] }).compileComponents().then(() => {
        bugReportPage = new BugReportPage(TestBed);
      });
      tick();
  }));


  it('submits a bug report', fakeAsync(() => {
    bugReportPage.fixture.componentRef.instance.profileStorageService.reload();
    tick();
    bugReportPage.fixture.componentRef.instance.reportBug();
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
