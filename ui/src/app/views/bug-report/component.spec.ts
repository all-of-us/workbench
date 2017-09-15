import {Component, DebugElement} from '@angular/core';
import {TestBed, async, tick, fakeAsync, ComponentFixture} from '@angular/core/testing';
import {Title, By} from '@angular/platform-browser';
import {ActivatedRoute, UrlSegment} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';
import {FormsModule} from '@angular/forms';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {BugReportComponent} from 'app/views/bug-report/component';
import {updateAndTick, simulateInput} from 'testing/test-helpers';
import {BugReportServiceStub} from 'testing/stubs/bug-report-service-stub';
import {BugReportService} from 'generated';
import {ClarityModule} from 'clarity-angular';

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
    this.reportBugButton = this.fixture.debugElement.query(By.css('#report-bug'));
    this.sendButton = this.fixture.debugElement.query(By.css('#send-bug-report'));
    this.shortDescription = this.fixture.debugElement.query(By.css('#bug-report-short-descr'));
    this.reproSteps = this.fixture.debugElement.query(By.css('#bug-report-repro-steps'));
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
      ] }).compileComponents().then(() => {
        bugReportPage = new BugReportPage(TestBed);
      });
      tick();
  }));


  it('submits a bug report', fakeAsync(() => {
    bugReportPage.reportBugButton.triggerEventHandler('click', null);
    updateAndTick(bugReportPage.fixture);
    updateAndTick(bugReportPage.fixture);
    bugReportPage.readPageData();
    simulateInput(bugReportPage.fixture, bugReportPage.shortDescription, testShortDescription);
    simulateInput(bugReportPage.fixture, bugReportPage.reproSteps, testReproSteps);
    updateAndTick(bugReportPage.fixture);
    updateAndTick(bugReportPage.fixture);
    bugReportPage.readPageData();
    bugReportPage.sendButton.triggerEventHandler('click', null);
    updateAndTick(bugReportPage.fixture);
    updateAndTick(bugReportPage.fixture);
    expect(bugReportPage.fixture.componentInstance.bugReport.shortDescription)
      .toBe(testShortDescription);
    expect(bugReportPage.fixture.componentInstance.bugReport.reproSteps).toBe(testReproSteps);
  }));


});
