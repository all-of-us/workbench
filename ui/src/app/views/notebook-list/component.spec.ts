import {DebugElement} from '@angular/core';
import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {By} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {ActivatedRoute, UrlSegment} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';

import {IconsModule} from 'app/icons/icons.module';
import {ProfileStorageService} from 'app/services/profile-storage.service';
import {SignInService} from 'app/services/sign-in.service';
import {BugReportComponent} from 'app/views/bug-report/component';
import {ConfirmDeleteModalComponent} from 'app/views/confirm-delete-modal/component';
import {NotebookListComponent} from 'app/views/notebook-list/component';
import {RenameModalComponent} from 'app/views/rename-modal/component';
import {WorkspaceNavBarComponent} from 'app/views/workspace-nav-bar/component';
import {WorkspaceShareComponent} from 'app/views/workspace-share/component';


import {
  BugReportService,
  CohortsService,
  ProfileService,
  WorkspaceAccessLevel,
  WorkspacesService
} from 'generated';

import {BugReportServiceStub} from 'testing/stubs/bug-report-service-stub';
import {CohortsServiceStub} from 'testing/stubs/cohort-service-stub';
import {ProfileServiceStub} from 'testing/stubs/profile-service-stub';
import {ProfileStorageServiceStub} from 'testing/stubs/profile-storage-service-stub';
import {WorkspacesServiceStub, WorkspaceStubVariables} from 'testing/stubs/workspace-service-stub';

import {updateAndTick} from 'testing/test-helpers';

class NotebookListPage {
  fixture: ComponentFixture<NotebookListComponent>;
  workspacesService: WorkspacesService;
  cohortsService: CohortsService;
  route: UrlSegment[];
  workspaceNamespace: string;
  workspaceId: string;
  notebookCards: DebugElement[];
  addCard: DebugElement;

  constructor(testBed: typeof TestBed) {
    this.fixture = testBed.createComponent(NotebookListComponent);
    this.route = this.fixture.debugElement.injector.get(ActivatedRoute).snapshot.url;
    this.workspacesService = this.fixture.debugElement.injector.get(WorkspacesService);
    this.cohortsService = this.fixture.debugElement.injector.get(CohortsService);
    this.readPageData();
  }

  readPageData() {
    updateAndTick(this.fixture);
    updateAndTick(this.fixture);
    this.workspaceNamespace = this.route[1].path;
    this.workspaceId = this.route[2].path;
    const de = this.fixture.debugElement;
    this.notebookCards = de.queryAll(By.css('.item-card'));
    this.addCard = de.queryAll((By.css('.add-card')))[0];
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

describe('NotebookListComponent', () => {
  let notebookListPage: NotebookListPage;
  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        BrowserAnimationsModule,
        FormsModule,
        IconsModule,
        RouterTestingModule,
        ClarityModule.forRoot()
      ],
      declarations: [
        BugReportComponent,
        ConfirmDeleteModalComponent,
        NotebookListComponent,
        RenameModalComponent,
        WorkspaceNavBarComponent,
        WorkspaceShareComponent
      ],
      providers: [
        { provide: BugReportService, useValue: new BugReportServiceStub() },
        { provide: SignInService, useValue: SignInService },
        { provide: ProfileStorageService, useValue: new ProfileStorageServiceStub() },
        { provide: ProfileService, useValue: new ProfileServiceStub() },
        { provide: WorkspacesService, useValue: new WorkspacesServiceStub() },
        { provide: CohortsService, useValue: new CohortsServiceStub()},
        { provide: ActivatedRoute, useValue: activatedRouteStub }
      ]}).compileComponents().then(() => {
      notebookListPage = new NotebookListPage(TestBed);
    });
    tick();
  }));


  it('displays correct information when notebooks selected.', fakeAsync(() => {
    notebookListPage.readPageData();
    tick();
    expect(notebookListPage.notebookCards.length).toEqual(1);
  }));

  it('displays correct notebook information', fakeAsync(() => {
    // Mock notebook service in workspace stub will be called as part of ngInit
    const fixture = notebookListPage.fixture;
    const app = fixture.debugElement.componentInstance;
    expect(app.notebookList.length).toEqual(1);
    expect(app.notebookList[0].name).toEqual('FileDetails');
    expect(app.notebookList[0].path).toEqual('gs://bucket/notebooks/mockFile');
  }));
});
