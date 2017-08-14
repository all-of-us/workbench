import {Component, DebugElement} from '@angular/core';
import {TestBed, async, tick, fakeAsync, ComponentFixture} from '@angular/core/testing';
import {Title, By} from '@angular/platform-browser';
import {ActivatedRoute, UrlSegment} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';
import {FormsModule} from '@angular/forms';
import {WorkspaceComponent} from 'app/views/workspace/component';
import {updateAndTick, simulateInput} from 'testing/test-helpers';
import {CohortsServiceStub} from 'testing/stubs/cohort-service-stub';
import {CohortsService} from 'generated';
import {UserService} from 'app/services/user.service';
import {RepositoryService} from 'app/services/repository.service';
import {ClarityModule} from 'clarity-angular';

class WorkspacePage {
  fixture: ComponentFixture<WorkspaceComponent>;
  cohortsService: CohortsService;
  userService: UserService;
  repositoryService: RepositoryService;
  route: UrlSegment[];
  workspaceNamespace: string;
  workspaceId: string;
  cohortsTableRows: DebugElement[];
  notebookTableRows: DebugElement[];
  cdrText: DebugElement;
  workspaceDescription: DebugElement;
  loggedOutMessage: DebugElement;

  constructor(testBed: typeof TestBed) {
    this.fixture = testBed.createComponent(WorkspaceComponent);
    this.cohortsService = this.fixture.debugElement.injector.get(CohortsService);
    this.userService = this.fixture.debugElement.injector.get(UserService);
    this.repositoryService = this.fixture.debugElement.injector.get(RepositoryService);
    this.route = this.fixture.debugElement.injector.get(ActivatedRoute).snapshot.url;
    this.readPageData();
  }

  readPageData() {
    updateAndTick(this.fixture);
    updateAndTick(this.fixture);
    this.workspaceNamespace = this.route[1].path;
    this.workspaceId = this.route[2].path;
    this.cohortsTableRows = this.fixture.debugElement.queryAll(By.css('.cohort-table-row'));
    this.notebookTableRows = this.fixture.debugElement.queryAll(By.css('.notebook-table-row'));
    this.cdrText = this.fixture.debugElement.query(By.css('.cdr-text'));
    this.workspaceDescription = this.fixture.debugElement.query(By.css('.description-text'));
    this.loggedOutMessage = this.fixture.debugElement.query(By.css('.logged-out-message'));
  }
}

const activatedRouteStub  = {
  snapshot: {
    url: [
      {path: 'workspace'},
      {path: WorkspaceComponent.DEFAULT_WORKSPACE_NS},
      {path: WorkspaceComponent.DEFAULT_WORKSPACE_ID}
    ]
  }
};

describe('WorkspaceComponent', () => {
  let workspacePage: WorkspacePage;

  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        ClarityModule.forRoot()
      ],
      declarations: [
        WorkspaceComponent
      ],
      providers: [
        { provide: CohortsService, useValue: new CohortsServiceStub() },
        { provide: UserService, useValue: new UserService() },
        { provide: RepositoryService, useValue: new RepositoryService() },
        { provide: ActivatedRoute, useValue: activatedRouteStub }
      ] }).compileComponents().then(() => {
        workspacePage = new WorkspacePage(TestBed);
      });
      tick();
  }));


  it('displays correct information in default workspace', fakeAsync(() => {
    let expectedCohorts: number;
    workspacePage.cohortsService.getCohortsInWorkspace(
        workspacePage.workspaceNamespace,
        workspacePage.workspaceId)
      .subscribe(cohorts => {
      expectedCohorts = cohorts.items.length;
    });
    tick();
    expect(workspacePage.cohortsTableRows.length).toEqual(expectedCohorts);
    expect(workspacePage.notebookTableRows.length).toEqual(2);
    expect(workspacePage.cdrText.nativeElement.innerText)
      .toMatch('CDR version info goes here.');
    expect(workspacePage.workspaceDescription.nativeElement.innerText)
      .toMatch('Default workspace for July, 2017 demo');
  }));

  it('displays login prompt when logged out', fakeAsync(() => {
    workspacePage.userService.logOut();
    workspacePage.userService.getLoggedInUser().then((user) => {
      updateAndTick(workspacePage.fixture);
      updateAndTick(workspacePage.fixture);
      workspacePage.fixture.componentRef.instance.ngOnInit();
      workspacePage.readPageData();
      expect(workspacePage.loggedOutMessage.nativeElement.innerText)
        .toMatch('Log in to view workspace.');
    });
  }));
  it('errors if it tries to access a non-existant workspace', fakeAsync(() => {
    workspacePage.route[1].path = 'fakeNamespace';
    workspacePage.route[2].path = '5';
    workspacePage.readPageData();

    expect(function(){
      workspacePage.fixture.componentRef.instance.ngOnInit();
      updateAndTick(workspacePage.fixture);
    }).toThrow();
  }));
});
