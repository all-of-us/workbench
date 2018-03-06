import {DebugElement} from '@angular/core';
import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import {Http} from '@angular/http';
import {ActivatedRoute, UrlSegment} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';

import {IconsModule} from 'app/icons/icons.module';
import {SignInService} from 'app/services/sign-in.service';
import {WorkspaceComponent} from 'app/views/workspace/component';
import {ClusterService, CohortsService, WorkspacesService} from 'generated';
import {ClusterServiceStub} from 'testing/stubs/cluster-service-stub';
import {CohortsServiceStub} from 'testing/stubs/cohort-service-stub';
import {HttpStub} from 'testing/stubs/http-stub';
import {WorkspacesServiceStub, WorkspaceStubVariables} from 'testing/stubs/workspace-service-stub';

import {
  queryAllByCss,
  queryByCss,
  simulateClick,
  updateAndTick
} from 'testing/test-helpers';

class WorkspacePage {
  fixture: ComponentFixture<WorkspaceComponent>;
  cohortsService: CohortsService;
  workspacesService: WorkspacesService;
  route: UrlSegment[];
  workspaceNamespace: string;
  workspaceId: string;
  cohortsTableRows: DebugElement[];
  notebookTableRows: DebugElement[];
  cdrText: DebugElement;
  workspaceDescription: DebugElement;
  loggedOutMessage: DebugElement;
  createAndLaunch: DebugElement;

  constructor(testBed: typeof TestBed) {
    this.fixture = testBed.createComponent(WorkspaceComponent);
    this.cohortsService = this.fixture.debugElement.injector.get(CohortsService);
    this.route = this.fixture.debugElement.injector.get(ActivatedRoute).snapshot.url;
    this.workspacesService = this.fixture.debugElement.injector.get(WorkspacesService);
    this.readPageData();
  }

  readPageData() {
    updateAndTick(this.fixture);
    updateAndTick(this.fixture);
    this.workspaceNamespace = this.route[1].path;
    this.workspaceId = this.route[2].path;
    this.cohortsTableRows = queryAllByCss(this.fixture, '.cohort-table-row');
    this.notebookTableRows = queryAllByCss(this.fixture, '.notebook-table-row');
    this.cdrText = queryByCss(this.fixture, '.cdr-text');
    this.workspaceDescription = queryByCss(this.fixture, '.description-text');
    this.loggedOutMessage = queryByCss(this.fixture, '.logged-out-message');
    this.createAndLaunch = queryByCss(this.fixture, '#createAndLaunch');
  }
}

const activatedRouteStub  = {
  snapshot: {
    url: [
      {path: 'workspace'},
      {path: WorkspaceStubVariables.DEFAULT_WORKSPACE_NS},
      {path: WorkspaceStubVariables.DEFAULT_WORKSPACE_ID}
    ],
    params: {
      'ns': WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
      'wsid': WorkspaceStubVariables.DEFAULT_WORKSPACE_ID
    }
  }
};

describe('WorkspaceComponent', () => {
  let workspacePage: WorkspacePage;
  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        FormsModule,
        RouterTestingModule,
        IconsModule,
        ClarityModule.forRoot()
      ],
      declarations: [
        WorkspaceComponent
      ],
      providers: [
        { provide: ClusterService, useValue: new ClusterServiceStub() },
        { provide: CohortsService, useValue: new CohortsServiceStub() },
        { provide: Http, useValue: new HttpStub() },
        { provide: SignInService, useValue: SignInService },
        { provide: WorkspacesService, useValue: new WorkspacesServiceStub() },
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
    expect(workspacePage.notebookTableRows.length).toEqual(1);
  }));

  it('fetches the correct workspace', fakeAsync(() => {
    workspacePage.fixture.componentRef.instance.ngOnInit();
    updateAndTick(workspacePage.fixture);
    updateAndTick(workspacePage.fixture);
    expect(workspacePage.cdrText.nativeElement.innerText)
      .toMatch(WorkspaceStubVariables.DEFAULT_WORKSPACE_CDR_VERSION);
    expect(workspacePage.workspaceDescription.nativeElement.innerText)
      .toMatch(WorkspaceStubVariables.DEFAULT_WORKSPACE_DESCRIPTION);
  }));

  it('deletes the correct workspace', fakeAsync(() => {
    let originalWorkspaceLength = 0;
    workspacePage.workspacesService.getWorkspaces().subscribe((workspaces) => {
      originalWorkspaceLength = workspaces.items.length;
    });
    simulateClick(workspacePage.fixture, queryByCss(workspacePage.fixture, '.btn-deleting'));
    let workspaceLength;
    workspacePage.workspacesService.getWorkspaces().subscribe((workspaces) => {
      workspaceLength = workspaces.items.length;
    });
    tick();
    expect(workspaceLength).toBe(originalWorkspaceLength - 1);

  }));

  it('displays correct notebook information', fakeAsync(() => {
    // Mock notebook service in workspace stub will be called as part of ngInit
    const fixture = workspacePage.fixture;
    const app = fixture.debugElement.componentInstance;
    expect(app.notebookList.length).toEqual(1);
    expect(app.notebookList[0].name).toEqual('FileDetails');
    expect(app.notebookList[0].path).toEqual('gs://bucket/notebooks/mockFile');
  }));

  it('Calls localize All files after creating cluster', fakeAsync(() => {
    const fixture = workspacePage.fixture;
    const app = fixture.debugElement.componentInstance;
    fixture.componentRef.instance.createAndLaunchNotebook();
    tick(5000);
    expect(app.clusterPulled).toEqual(true);
  }));


});
