import {DebugElement} from '@angular/core';
import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {Http} from '@angular/http';
import {By} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {ActivatedRoute, UrlSegment} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';

import {IconsModule} from 'app/icons/icons.module';
import {ProfileStorageService} from 'app/services/profile-storage.service';
import {ServerConfigService} from 'app/services/server-config.service';
import {SignInService} from 'app/services/sign-in.service';
import {BugReportComponent} from 'app/views/bug-report/component';
import {ConfirmDeleteModalComponent} from 'app/views/confirm-delete-modal/component';
import {WorkspaceNavBarComponent} from 'app/views/workspace-nav-bar/component';
import {WorkspaceShareComponent} from 'app/views/workspace-share/component';
import {WorkspaceComponent} from 'app/views/workspace/component';

import {
  BugReportService,
  ClusterService,
  CohortsService,
  ProfileService,
  WorkspaceAccessLevel,
  WorkspacesService
} from 'generated';
import {
  JupyterService,
  NotebooksService,
} from 'notebooks-generated';

import {BugReportServiceStub} from 'testing/stubs/bug-report-service-stub';
import {ClusterServiceStub} from 'testing/stubs/cluster-service-stub';
import {CohortsServiceStub} from 'testing/stubs/cohort-service-stub';
import {HttpStub} from 'testing/stubs/http-stub';
import {JupyterServiceStub} from 'testing/stubs/jupyter-service-stub';
import {NotebooksServiceStub} from 'testing/stubs/notebooks-service-stub';
import {ProfileServiceStub} from 'testing/stubs/profile-service-stub';
import {ProfileStorageServiceStub} from 'testing/stubs/profile-storage-service-stub';
import {ServerConfigServiceStub} from 'testing/stubs/server-config-service-stub';
import {WorkspacesServiceStub, WorkspaceStubVariables} from 'testing/stubs/workspace-service-stub';

import {updateAndTick} from 'testing/test-helpers';

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
    const de = this.fixture.debugElement;
    this.cohortsTableRows = de.queryAll(By.css('.cohort-table-row'));
    this.notebookTableRows = de.queryAll(By.css('.notebook-table-row'));
    this.cdrText = de.query(By.css('.cdr-version-text'));
    this.workspaceDescription = de.query(By.css('.description-text'));
    this.loggedOutMessage = de.query(By.css('.logged-out-message'));
    this.createAndLaunch = de.query(By.css('#createAndLaunch'));
  }
}

const activatedRouteStub  = {
  snapshot: {
    url: [
      {path: 'workspaces'},
      {path: WorkspaceStubVariables.DEFAULT_WORKSPACE_NS},
      {path: WorkspaceStubVariables.DEFAULT_WORKSPACE_ID}
    ],
    params: {
      'ns': WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
      'wsid': WorkspaceStubVariables.DEFAULT_WORKSPACE_ID
    },
    data: {
      workspace: {
        ...WorkspacesServiceStub.stubWorkspace(),
        accessLevel: WorkspaceAccessLevel.OWNER,
      }
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
        BrowserAnimationsModule,
        IconsModule,
        ClarityModule.forRoot()
      ],
      declarations: [
        BugReportComponent,
        ConfirmDeleteModalComponent,
        WorkspaceComponent,
        WorkspaceNavBarComponent,
        WorkspaceShareComponent
      ],
      providers: [
        { provide: BugReportService, useValue: new BugReportServiceStub() },
        { provide: ClusterService, useValue: new ClusterServiceStub() },
        { provide: CohortsService, useValue: new CohortsServiceStub() },
        { provide: NotebooksService, useValue: new NotebooksServiceStub() },
        { provide: JupyterService, useValue: new JupyterServiceStub() },
        { provide: Http, useValue: new HttpStub() },
        { provide: ProfileStorageService, useValue: new ProfileStorageServiceStub() },
        { provide: SignInService, useValue: SignInService },
        { provide: WorkspacesService, useValue: new WorkspacesServiceStub() },
        { provide: ActivatedRoute, useValue: activatedRouteStub },
        { provide: ProfileService, useValue: new ProfileServiceStub() },
        {
          provide: ServerConfigService,
          useValue: new ServerConfigServiceStub({
            gsuiteDomain: 'fake-research-aou.org'
          })
        }
      ]}).compileComponents().then(() => {
        workspacePage = new WorkspacePage(TestBed);
      });
      tick();
  }));

  it('displays correct notebook information', fakeAsync(() => {
    // Mock notebook service in workspace stub will be called as part of ngInit
    const fixture = workspacePage.fixture;
    const app = fixture.debugElement.componentInstance;
    expect(app.notebookList.length).toEqual(1);
    expect(app.notebookList[0].name).toEqual('FileDetails');
    expect(app.notebookList[0].path).toEqual('gs://bucket/notebooks/mockFile');
    expect(workspacePage.fixture.debugElement.queryAll(
      By.css('.research-purpose-item')).length).toEqual(2);
  }));
});
