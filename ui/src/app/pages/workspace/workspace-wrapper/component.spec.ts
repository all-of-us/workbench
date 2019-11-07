import {Component} from '@angular/core';
import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {Router} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';

import {ClarityModule} from '@clr/angular';

import {ProfileStorageService} from 'app/services/profile-storage.service';
import {ServerConfigService} from 'app/services/server-config.service';
import {registerApiClient, workspacesApi} from 'app/services/swagger-fetch-clients';
import {urlParamsStore} from 'app/utils/navigation';

import {BugReportComponent} from 'app/components/bug-report';
import {ConfirmDeleteModalComponent} from 'app/components/confirm-delete-modal';
import {WorkspaceNavBarComponent} from 'app/pages/workspace/workspace-nav-bar';
import {WorkspaceShareComponent} from 'app/pages/workspace/workspace-share';
import {WorkspaceWrapperComponent} from 'app/pages/workspace/workspace-wrapper/component';

import {UserService, WorkspaceAccessLevel} from 'generated';
import {WorkspaceAccessLevel as FetchWorkspaceAccessLevel, WorkspaceResponse, WorkspacesApi} from 'generated/fetch';

import {ProfileStorageServiceStub} from 'testing/stubs/profile-storage-service-stub';
import {ServerConfigServiceStub} from 'testing/stubs/server-config-service-stub';
import {UserServiceStub} from 'testing/stubs/user-service-stub';
import {WorkspacesServiceStub} from 'testing/stubs/workspace-service-stub';
import {WorkspacesApiStub, workspaceStubs, WorkspaceStubVariables} from 'testing/stubs/workspaces-api-stub';

import {findElements} from 'testing/react-testing-utility';
import {setupModals, updateAndTick} from 'testing/test-helpers';

@Component({
  selector: 'app-test',
  template: '<router-outlet></router-outlet>'
})
class FakeAppComponent {}

const workspaceResponse: WorkspaceResponse = {
  workspace: {...workspaceStubs[0]},
  accessLevel: FetchWorkspaceAccessLevel.OWNER,
};

describe('WorkspaceWrapperComponent', () => {
  let fixture: ComponentFixture<WorkspaceWrapperComponent>;
  let router: Router;
  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        FormsModule,
        ClarityModule.forRoot(),
        RouterTestingModule.withRoutes([{
          path: 'workspaces/:ns/:wsid',
          component: WorkspaceWrapperComponent,
          data: {
            workspace: {
              ...WorkspacesServiceStub.stubWorkspace(),
              accessLevel: WorkspaceAccessLevel.OWNER,
            }
          },
          children: []
        }])
      ],
      declarations: [
        BugReportComponent,
        ConfirmDeleteModalComponent,
        FakeAppComponent,
        WorkspaceWrapperComponent,
        WorkspaceNavBarComponent,
        WorkspaceShareComponent,
      ],
      providers: [
        {provide: ProfileStorageService, useValue: new ProfileStorageServiceStub()},
        {provide: UserService, useValue: new UserServiceStub()},
        {
          provide: ServerConfigService,
          useValue: new ServerConfigServiceStub({
            gsuiteDomain: 'fake-research-aou.org'
          })
        },
      ]
    }).compileComponents().then(() => {
      registerApiClient(WorkspacesApi, new WorkspacesApiStub());
      fixture = TestBed.createComponent(WorkspaceWrapperComponent);
      setupModals(fixture);
      router = TestBed.get(Router);

      router.navigateByUrl(
        `/workspaces/${WorkspaceStubVariables.DEFAULT_WORKSPACE_NS}/` +
        `WorkspaceStubVariables.DEFAULT_WORKSPACE_ID)/about`);

      spyOn(workspacesApi(), 'getWorkspace')
        .and.returnValue(Promise.resolve(workspaceResponse));

      urlParamsStore.next({
        ns: WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
        wsid: WorkspaceStubVariables.DEFAULT_WORKSPACE_ID
      });

      // Clarity needs several ticks/redraw cycles to render its button group.
      tick();
      updateAndTick(fixture);
      updateAndTick(fixture);
    });
  }));

  it('should render', fakeAsync(() => {
    updateAndTick(fixture);
    expect(fixture).toBeTruthy();
  }));

  it('fetches user roles before opening the share dialog', fakeAsync(() => {
    updateAndTick(fixture);

    const userRolesSpy = spyOn(workspacesApi(), 'getFirecloudWorkspaceUserRoles')
      .and.callThrough();

    // Ideally, we would do this through clicks on the page, rather than running
    // the function directly. However we could not actually access the react menu
    // component to click on.
    fixture.componentInstance.handleShareAction();

    updateAndTick(fixture);
    updateAndTick(fixture);
    const elements = findElements(fixture, '[data-test-id="collab-user-name"]');
    // We should have called the userRoles API before opening the share modal.
    expect(userRolesSpy).toHaveBeenCalledTimes(1);
    let expectedRoles;
    workspacesApi().getFirecloudWorkspaceUserRoles(
      WorkspaceStubVariables.DEFAULT_WORKSPACE_NS, WorkspaceStubVariables.DEFAULT_WORKSPACE_ID)
      .then(resp => {
        expectedRoles = resp.items.length;
      });
    tick();
    expect(elements.length).toBe(expectedRoles);
  }));
});
