import {Component, DebugElement} from '@angular/core';
import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {By} from '@angular/platform-browser';
import {Router, UrlSegment} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';

import {ErrorHandlingService} from 'app/services/error-handling.service';
import {ProfileStorageService} from 'app/services/profile-storage.service';
import {ServerConfigService} from 'app/services/server-config.service';
import {BugReportComponent} from 'app/views/bug-report/component';
import {ConfirmDeleteModalComponent} from 'app/views/confirm-delete-modal/component';
import {WorkspaceListComponent} from 'app/views/workspace-list/component';
import {WorkspaceShareComponent} from 'app/views/workspace-share/component';
import {BugReportServiceStub} from 'testing/stubs/bug-report-service-stub';
import {ErrorHandlingServiceStub} from 'testing/stubs/error-handling-service-stub';
import {ProfileStorageServiceStub} from 'testing/stubs/profile-storage-service-stub';
import {ServerConfigServiceStub} from 'testing/stubs/server-config-service-stub';
import {WorkspacesServiceStub} from 'testing/stubs/workspace-service-stub';
import {
  simulateClick, updateAndTick
} from 'testing/test-helpers';

import {
  BugReportService,
  WorkspacesService
} from 'generated';

class WorkspaceListPage {
  fixture: ComponentFixture<WorkspaceListComponent>;
  workspacesService: WorkspacesService;
  route: UrlSegment[];
  workspaceCards: DebugElement[];
  loggedOutMessage: DebugElement;

  constructor(testBed: typeof TestBed) {
    this.fixture = testBed.createComponent(WorkspaceListComponent);
    this.workspacesService = this.fixture.debugElement.injector.get(WorkspacesService);
    this.readPageData();
  }

  readPageData() {
    updateAndTick(this.fixture);
    updateAndTick(this.fixture);
    const de = this.fixture.debugElement;
    this.workspaceCards = de.queryAll(By.css('.workspace-card'));
    this.loggedOutMessage = de.query(By.css('.logged-out-message'));
  }
}

@Component({
  selector: 'app-fake-edit',
  template: '<div class="fake-edit"></div>'
})
class FakeEditComponent {}

@Component({
  selector: 'app-fake-clone',
  template: '<div class="fake-clone"></div>'
})
class FakeCloneComponent {}


describe('WorkspaceListComponent', () => {
  let workspaceListPage: WorkspaceListPage;
  let routerStub: Router;
  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        FormsModule,
        RouterTestingModule.withRoutes([
          {path: 'workspaces', component: WorkspaceListComponent},
          {path: 'workspaces/:ns/:wsid/edit', component: FakeEditComponent},
          {path: 'workspaces/:ns/:wsid/clone', component: FakeCloneComponent},
        ]),
        ClarityModule
      ],
      declarations: [
        BugReportComponent,
        ConfirmDeleteModalComponent,
        FakeCloneComponent,
        FakeEditComponent,
        WorkspaceListComponent,
        WorkspaceShareComponent,
      ],
      providers: [
        { provide: BugReportService, useValue: new BugReportServiceStub() },
        { provide: WorkspacesService, useValue: new WorkspacesServiceStub() },
        { provide: ErrorHandlingService, useValue: new ErrorHandlingServiceStub() },
        { provide: ProfileStorageService, useValue: new ProfileStorageServiceStub() },
        {
          provide: ServerConfigService,
          useValue: new ServerConfigServiceStub({
            gsuiteDomain: 'fake-research-aou.org'
          })
        },
      ] }).compileComponents().then(() => {
        workspaceListPage = new WorkspaceListPage(TestBed);
        routerStub = TestBed.get(Router);
      });
      tick();
  }));


  it('displays correct number of workspaces in home-page', fakeAsync(() => {
    let expectedWorkspaces: number;
    workspaceListPage.workspacesService.getWorkspaces()
      .subscribe(workspaces => {
        expectedWorkspaces = workspaces.items.length;
    });
    tick();
    expect(workspaceListPage.workspaceCards.length).toEqual(expectedWorkspaces);
  }));


  it('enables editing workspaces', fakeAsync(() => {
    const firstWorkspace = workspaceListPage.fixture.componentInstance.workspaceList[0].workspace;
    simulateClick(workspaceListPage.fixture,
      workspaceListPage.workspaceCards[0].query(By.css('.dropdown-toggle')));
    updateAndTick(workspaceListPage.fixture);
    simulateClick(workspaceListPage.fixture,
      workspaceListPage.workspaceCards[0].query(By.css('.edit-item')));
    updateAndTick(workspaceListPage.fixture);

    expect(routerStub.url)
      .toEqual('/workspaces/' + firstWorkspace.namespace +
        '/' + firstWorkspace.id + '/edit');
  }));

  it('enables deleting workspaces', fakeAsync(() => {

  }));

  it('enables cloning workspaces', fakeAsync(() => {
    const firstWorkspace = workspaceListPage.fixture.componentInstance.workspaceList[0].workspace;
    simulateClick(workspaceListPage.fixture,
      workspaceListPage.workspaceCards[0].query(By.css('.dropdown-toggle')));
    updateAndTick(workspaceListPage.fixture);
    simulateClick(workspaceListPage.fixture,
      workspaceListPage.workspaceCards[0].query(By.css('.clone-item')));
    updateAndTick(workspaceListPage.fixture);
    let router: Router = TestBed.get(Router);
    expect(routerStub.url)
      .toEqual('/workspaces/' + firstWorkspace.namespace +
        '/' + firstWorkspace.id + '/clone');
  }));

  it('enables sharing workspaces', fakeAsync(() => {

  }));
});
