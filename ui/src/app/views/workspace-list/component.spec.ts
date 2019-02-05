import {Component, DebugElement} from '@angular/core';
import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {By} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {Router, UrlSegment} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';

import {ErrorHandlingService} from 'app/services/error-handling.service';
import {ProfileStorageService} from 'app/services/profile-storage.service';
import {ServerConfigService} from 'app/services/server-config.service';
import {BugReportComponent} from 'app/views/bug-report/component';
import {ConfirmDeleteModalComponent} from 'app/views/confirm-delete-modal/component';
import {TopBoxComponent} from 'app/views/top-box/component';
import {WorkspaceListComponent} from 'app/views/workspace-list/component';
import {WorkspaceShareComponent} from 'app/views/workspace-share/component';
import {BugReportServiceStub} from 'testing/stubs/bug-report-service-stub';
import {ErrorHandlingServiceStub} from 'testing/stubs/error-handling-service-stub';
import {ProfileStorageServiceStub} from 'testing/stubs/profile-storage-service-stub';
import {ServerConfigServiceStub} from 'testing/stubs/server-config-service-stub';
import {UserServiceStub} from 'testing/stubs/user-service-stub';
import {
  setupModals,
  simulateClick,
  updateAndTick
} from 'testing/test-helpers';

import {
  BugReportService,
  UserService, WorkspacesService,
} from 'generated';

import {registerApiClient, workspacesApi} from 'app/services/swagger-fetch-clients';
import {WorkspacesApi} from 'generated/fetch';
import {WorkspacesServiceStub} from 'testing/stubs/workspace-service-stub';
import {WorkspacesApiStub} from 'testing/stubs/workspaces-api-stub';

class WorkspaceListPage {
  fixture: ComponentFixture<WorkspaceListComponent>;
  routerStub: Router;
  route: UrlSegment[];
  workspaceCards: DebugElement[];
  loggedOutMessage: DebugElement;

  constructor(testBed: typeof TestBed) {
    this.fixture = testBed.createComponent(WorkspaceListComponent);
    setupModals(this.fixture);
    this.routerStub = this.fixture.debugElement.injector.get(Router);
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
  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        BrowserAnimationsModule,
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
        TopBoxComponent,
        WorkspaceListComponent,
        WorkspaceShareComponent,
      ],
      providers: [
        { provide: BugReportService, useValue: new BugReportServiceStub() },
        { provide: UserService, useValue: new UserServiceStub() },
        { provide: WorkspacesService, useValue: new WorkspacesServiceStub() },
        { provide: WorkspacesApi, useValue: new WorkspacesApiStub() },
        { provide: ErrorHandlingService, useValue: new ErrorHandlingServiceStub() },
        { provide: ProfileStorageService, useValue: new ProfileStorageServiceStub() },
        {
          provide: ServerConfigService,
          useValue: new ServerConfigServiceStub({
            gsuiteDomain: 'fake-research-aou.org'
          })
        },
      ] }).compileComponents().then(() => {
        registerApiClient(WorkspacesApi, new WorkspacesApiStub());
        workspaceListPage = new WorkspaceListPage(TestBed);
      });
    tick();
  }));


  it('displays correct number of workspaces in home-page', fakeAsync(() => {
    let expectedWorkspaces: number;
    workspacesApi().getWorkspaces()
      .then(workspaces => {
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

    expect(workspaceListPage.routerStub.url)
      .toEqual('/workspaces/' + firstWorkspace.namespace +
        '/' + firstWorkspace.id + '/edit');
  }));

  it('enables deleting workspaces', fakeAsync(() => {
    // TODO: These tests should be included on conversion to React
    // const de = workspaceListPage.fixture.debugElement;
    // const deleteSpy = spyOn(TestBed.get(WorkspacesApi), 'deleteWorkspace')
    //   .and.callThrough();
    // const firstWorkspace = workspaceListPage.fixture.componentInstance.workspaceList[0]
    // .workspace;
    // const numWorkspaces = workspaceListPage.fixture.componentInstance.workspaceList.length;
    // simulateClick(workspaceListPage.fixture,
    //   workspaceListPage.workspaceCards[0].query(By.css('.dropdown-toggle')));
    // updateAndTick(workspaceListPage.fixture);
    // simulateClick(workspaceListPage.fixture, de.query(By.css('.delete-item')));
    // updateAndTick(workspaceListPage.fixture);
    // expect(workspaceListPage.fixture.componentInstance.confirmDeleting).toBeTruthy();
    // expect(workspaceListPage.fixture.componentInstance.workspace)
    //   .toEqual(firstWorkspace);
    // simulateClickReact(workspaceListPage.fixture, '[data-test-id="confirm-delete"]');
    // expect(deleteSpy).toHaveBeenCalled();
    // const numNewWorkspaces = workspaceListPage.fixture.componentInstance.workspaceList.length;
    // expect(numNewWorkspaces).toEqual(numWorkspaces - 1);

  }));

  it('enables cloning workspaces', fakeAsync(() => {
    const firstWorkspace = workspaceListPage.fixture.componentInstance.workspaceList[0].workspace;
    simulateClick(workspaceListPage.fixture,
      workspaceListPage.workspaceCards[0].query(By.css('.dropdown-toggle')));
    updateAndTick(workspaceListPage.fixture);
    simulateClick(workspaceListPage.fixture,
      workspaceListPage.workspaceCards[0].query(By.css('.clone-item')));
    updateAndTick(workspaceListPage.fixture);
    expect(workspaceListPage.routerStub.url)
      .toEqual('/workspaces/' + firstWorkspace.namespace +
        '/' + firstWorkspace.id + '/clone');
  }));

  it('enables sharing workspaces', fakeAsync(() => {
    // TODO: These tests should be included upon conversion to React
    // const firstWorkspace = workspaceListPage.fixture.componentInstance
    // .workspaceList[0].workspace;
    // simulateClick(workspaceListPage.fixture,
    //   workspaceListPage.workspaceCards[0].query(By.css('.dropdown-toggle')));
    // updateAndTick(workspaceListPage.fixture);
    // simulateClick(workspaceListPage.fixture,
    //   workspaceListPage.workspaceCards[0].query(By.css('.share-item')));
    // updateAndTick(workspaceListPage.fixture);
    // expect(workspaceListPage.fixture.componentInstance.sharing).toBeTruthy();
    // expect(workspaceListPage.fixture.componentInstance.workspace)
    //   .toEqual(firstWorkspace);
    // Further tests in the workspace share component
  }));

  it('has the correct permissions classes', fakeAsync(() => {
    // Testing the CSS classes applied, rather than actual logic on the component
    const workspaceList = workspaceListPage.fixture.componentInstance.workspaceList;
    const permissionsOnPage = workspaceListPage
        .fixture.debugElement.queryAll(By.css('.permission-box'));
    for (let i = 0; i < workspaceList.length; i++) {
      if (workspaceList[i].isOwner) {
        expect (permissionsOnPage[i].nativeNode.classList).toContain('owner-permission');
      } else if (workspaceList[i].isReadOnly) {
        expect (permissionsOnPage[i].nativeNode.classList).toContain('reader-permission');
      } else {
        expect (permissionsOnPage[i].nativeNode.classList).toContain('writer-permission');
      }
    }
  }));
});
