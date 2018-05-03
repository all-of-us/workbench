import {DebugElement} from '@angular/core';
import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {By} from '@angular/platform-browser';
import {UrlSegment} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';

import {ErrorHandlingService} from 'app/services/error-handling.service';
import {ProfileStorageService} from 'app/services/profile-storage.service';
import {WorkspaceListComponent} from 'app/views/workspace-list/component';
import {ErrorHandlingServiceStub} from 'testing/stubs/error-handling-service-stub';
import {ProfileStorageServiceStub} from 'testing/stubs/profile-storage-service-stub';
import {WorkspacesServiceStub} from 'testing/stubs/workspace-service-stub';
import {
  updateAndTick
} from 'testing/test-helpers';

import {WorkspacesService} from 'generated';

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
    this.workspaceCards = de.queryAll(By.css('.card'));
    this.loggedOutMessage = de.query(By.css('.logged-out-message'));
  }
}


describe('WorkspaceListComponent', () => {
  let workspaceListPage: WorkspaceListPage;
  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        ClarityModule
      ],
      declarations: [
        WorkspaceListComponent
      ],
      providers: [
        { provide: WorkspacesService, useValue: new WorkspacesServiceStub() },
        { provide: ErrorHandlingService, useValue: new ErrorHandlingServiceStub() },
        { provide: ProfileStorageService, useValue: new ProfileStorageServiceStub() },
      ] }).compileComponents().then(() => {
        workspaceListPage = new WorkspaceListPage(TestBed);
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

});
