import {DebugElement} from '@angular/core';
import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {UrlSegment} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';

import {WorkspaceListComponent} from 'app/views/workspace-list/component';
import {WorkspacesServiceStub} from 'testing/stubs/workspace-service-stub';
import {
  queryAllByCss,
  queryByCss,
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
    this.workspaceCards = queryAllByCss(this.fixture, '.card');
    this.loggedOutMessage = queryByCss(this.fixture, '.logged-out-message');
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
        { provide: WorkspacesService, useValue: new WorkspacesServiceStub() }
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
