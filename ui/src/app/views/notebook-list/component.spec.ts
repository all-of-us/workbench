import {DebugElement} from '@angular/core';
import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {By} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {ActivatedRoute, UrlSegment} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';

import {IconsModule} from 'app/icons/icons.module';
import {ProfileStorageService} from 'app/services/profile-storage.service';
import {SignInService} from 'app/services/sign-in.service';
import {BugReportComponent} from 'app/views/bug-report/component';
import {CohortEditModalComponent} from 'app/views/cohort-edit-modal/component';
import {ConfirmDeleteModalComponent} from 'app/views/confirm-delete-modal/component';
import {NewNotebookModalComponent} from 'app/views/new-notebook-modal/component';
import {NotebookListComponent} from 'app/views/notebook-list/component';
import {RenameModalComponent} from 'app/views/rename-modal/component';
import {ResourceCardComponent} from 'app/views/resource-card/component';
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
import {ProfileServiceStub} from 'testing/stubs/profile-service-stub';
import {ProfileStorageServiceStub} from 'testing/stubs/profile-storage-service-stub';
import {WorkspacesServiceStub, WorkspaceStubVariables} from 'testing/stubs/workspace-service-stub';

import {simulateClick, simulateInput, updateAndTick} from 'testing/test-helpers';

class NotebookListPage {
  fixture: ComponentFixture<NotebookListComponent>;
  workspacesService: WorkspacesService;
  route: UrlSegment[];
  workspaceNamespace: string;
  workspaceId: string;
  notebookCards: DebugElement[];
  addCard: DebugElement;

  constructor(testBed: typeof TestBed) {
    this.fixture = testBed.createComponent(NotebookListComponent);
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
        ReactiveFormsModule,
        IconsModule,
        RouterTestingModule,
        ClarityModule.forRoot()
      ],
      declarations: [
        BugReportComponent,
        CohortEditModalComponent,
        ConfirmDeleteModalComponent,
        NewNotebookModalComponent,
        NotebookListComponent,
        ResourceCardComponent,
        RenameModalComponent,
        WorkspaceNavBarComponent,
        WorkspaceShareComponent
      ],
      providers: [
        { provide: BugReportService, useValue: new BugReportServiceStub() },
        { provide: CohortsService },
        { provide: SignInService, useValue: SignInService },
        { provide: ProfileStorageService, useValue: new ProfileStorageServiceStub() },
        { provide: ProfileService, useValue: new ProfileServiceStub() },
        { provide: WorkspacesService, useValue: new WorkspacesServiceStub() },
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
    expect(app.notebookList[0].name).toEqual('mockFile.ipynb');
    expect(app.notebookList[0].path).toEqual('gs://bucket/notebooks/mockFile.ipynb');
  }));

  it('displays correct information when notebook renamed', fakeAsync(() => {
    const fixture = notebookListPage.fixture;
    const de = fixture.debugElement;
    simulateClick(fixture, de.query(By.css('.resource-menu')));
    simulateClick(fixture, de.query(By.css('.pencil')));
    simulateInput(fixture, de.query(By.css('#new-name')), 'testMockFile');
    simulateClick(fixture, de.query(By.css('button#rename')));
    updateAndTick(fixture);
    const notebooksOnPage = de.queryAll(By.css('.item-card'));
    expect(notebooksOnPage.map((nb) => nb.nativeElement.innerText)).toMatch('testMockFile.ipynb');
    expect(fixture.componentInstance.resourceList[0].notebook.name)
        .toEqual('testMockFile.ipynb');
  }));

  it('displays correct information when notebook renamed with duplicate name', fakeAsync(() => {
    const fixture = notebookListPage.fixture;
    const de = fixture.debugElement;
    simulateClick(fixture, de.query(By.css('.resource-menu')));
    simulateClick(fixture, de.query(By.css('.pencil')));
    simulateInput(fixture, de.query(By.css('#new-name')), 'mockFile');
    simulateClick(fixture, de.query(By.css('button#rename')));
    updateAndTick(fixture);
    tick();
    const errorMessage = de.queryAll(By.css('.modal-title'));
    expect(errorMessage.map(com => com.nativeElement.innerText)[0]).toEqual('Error:');
    simulateClick(fixture, de.query(By.css('.close')));
    const notebooksOnPage = de.queryAll(By.css('.item-card'));
    expect(notebooksOnPage.map((nb) => nb.nativeElement.innerText)).toMatch('mockFile.ipynb');
  }));

  it('displays correct information when notebook cloned', fakeAsync(() => {
    const fixture = notebookListPage.fixture;
    const de = fixture.debugElement;
    simulateClick(fixture, de.query(By.css('.resource-menu')));
    updateAndTick(fixture);
    simulateClick(fixture, de.query(By.css('.copy')));
    fixture.componentInstance.updateList();
    tick();
    updateAndTick(fixture);
    const notebooksOnPage = de.queryAll(By.css('.item-card'));
    expect(notebooksOnPage.map((nb) => nb.nativeElement.innerText)).toMatch('mockFile Clone.ipynb');
    expect(fixture.componentInstance.resourceList.map(nb => nb.notebook.name))
        .toContain('mockFile Clone.ipynb');
  }));

  it('displays correct information when notebook deleted', fakeAsync(() => {
    const fixture = notebookListPage.fixture;
    const de = fixture.debugElement;
    simulateClick(fixture, de.query(By.css('.resource-menu')));
    simulateClick(fixture, de.query(By.css('.trash')));
    updateAndTick(fixture);
    simulateClick(fixture, de.query(By.css('.confirm-delete-btn')));
    updateAndTick(fixture);
    const notebooksOnPage = de.queryAll(By.css('.item-card'));
    expect(notebooksOnPage.length).toBe(0);
  }));
});
