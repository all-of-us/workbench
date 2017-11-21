import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {By} from '@angular/platform-browser';
import {ActivatedRoute, UrlSegment} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from 'clarity-angular';

import {ErrorHandlingService} from 'app/services/error-handling.service';
import {WorkspaceShareComponent} from 'app/views/workspace-share/component';

import {ErrorHandlingServiceStub} from 'testing/stubs/error-handling-service-stub';
import {ProfileServiceStub} from 'testing/stubs/profile-service-stub';
import {WorkspacesServiceStub, WorkspaceStubVariables} from 'testing/stubs/workspace-service-stub';
import {updateAndTick} from 'testing/test-helpers';

import {ProfileService} from 'generated';
import {UserRole} from 'generated';
import {WorkspacesService} from 'generated';

class WorkspaceSharePage {
  fixture: ComponentFixture<WorkspaceShareComponent>;
  workspacesService: WorkspacesService;
  route: UrlSegment[];
  workspaceNamespace: string;
  workspaceId: string;
  userRolesOnPage: Array<UserRole>;

  constructor(testBed: typeof TestBed) {
    this.fixture = testBed.createComponent(WorkspaceShareComponent);
    this.route = this.fixture.debugElement.injector.get(ActivatedRoute).snapshot.url;
    this.workspacesService = this.fixture.debugElement.injector.get(WorkspacesService);
    this.readPageData();
  }

  readPageData() {
    updateAndTick(this.fixture);
    updateAndTick(this.fixture);

    this.workspaceNamespace = this.route[1].path;
    this.workspaceId = this.route[2].path;
    const setOfUsers = this.fixture.debugElement.queryAll(By.css('.user'));
    this.userRolesOnPage = [];
    setOfUsers.forEach((user) => {
      this.userRolesOnPage.push({email: user.children[0].nativeElement.innerText,
          role: user.children[1].nativeElement.innerText});
    });
  }
}

const activatedRouteStub  = {
  snapshot: {
    url: [
      {path: 'workspace'},
      {path: WorkspaceStubVariables.DEFAULT_WORKSPACE_NS},
      {path: WorkspaceStubVariables.DEFAULT_WORKSPACE_ID},
      {path: 'share'}
    ],
    params: {
      'ns': WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
      'wsid': WorkspaceStubVariables.DEFAULT_WORKSPACE_ID
    }
  }
};

describe('WorkspaceShareComponent', () => {
  let workspaceSharePage: WorkspaceSharePage;

  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        FormsModule,
        ClarityModule.forRoot()
      ],
      declarations: [
        WorkspaceShareComponent
      ],
      providers: [
        { provide: ErrorHandlingService, useValue: new ErrorHandlingServiceStub() },
        { provide: WorkspacesService, useValue: new WorkspacesServiceStub() },
        { provide: ActivatedRoute, useValue: activatedRouteStub },
        { provide: ProfileService, useValue: new ProfileServiceStub() }
      ] }).compileComponents().then(() => {
        workspaceSharePage = new WorkspaceSharePage(TestBed);
      });
      tick();
  }));


  it('displays correct information in default workspace sharing', fakeAsync(() => {
    workspaceSharePage.readPageData();
    expect(workspaceSharePage.userRolesOnPage)
       .toEqual(workspaceSharePage.fixture.componentRef.instance.workspace.userRoles);
  }));


});
