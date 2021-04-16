import {Component} from '@angular/core';
import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {RouterTestingModule} from '@angular/router/testing';

import {ClarityModule} from '@clr/angular';

import {ProfileStorageService} from 'app/services/profile-storage.service';
import {registerApiClient, workspacesApi} from 'app/services/swagger-fetch-clients';
import {serverConfigStore, urlParamsStore} from 'app/utils/navigation';

import {BugReportComponent} from 'app/components/bug-report';
import {ConfirmDeleteModalComponent} from 'app/components/confirm-delete-modal';
import {HelpSidebarComponent} from 'app/components/help-sidebar';
import {WorkspaceNavBarComponent} from 'app/pages/workspace/workspace-nav-bar';
import {WorkspaceShareComponent} from 'app/pages/workspace/workspace-share';
import {WorkspaceWrapperComponent} from 'app/pages/workspace/workspace-wrapper/component';

import {RuntimeApi, UserApi, WorkspaceAccessLevel, WorkspacesApi} from 'generated/fetch';

import {ProfileStorageServiceStub} from 'testing/stubs/profile-storage-service-stub';
import {UserApiStub} from 'testing/stubs/user-api-stub';
import {buildWorkspaceStub, WorkspaceStubVariables} from 'testing/stubs/workspaces';
import {WorkspacesApiStub} from 'testing/stubs/workspaces-api-stub';

import {cdrVersionStore} from 'app/utils/stores';
import {findElements} from 'testing/react-testing-utility';
import {cdrVersionTiersResponse} from 'testing/stubs/cdr-versions-api-stub';
import {RuntimeApiStub} from 'testing/stubs/runtime-api-stub';
import {setupModals, updateAndTick} from 'testing/test-helpers';

@Component({
  selector: 'app-test',
  template: '<router-outlet></router-outlet>'
})
class FakeAppComponent {}

describe('WorkspaceWrapperComponent', () => {
  let fixture: ComponentFixture<WorkspaceWrapperComponent>;
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
              ...buildWorkspaceStub(),
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
        HelpSidebarComponent,
        WorkspaceWrapperComponent,
        WorkspaceNavBarComponent,
        WorkspaceShareComponent,
      ],
      providers: [
        {provide: ProfileStorageService, useValue: new ProfileStorageServiceStub()},
      ]
    }).compileComponents().then(() => {
      registerApiClient(WorkspacesApi, new WorkspacesApiStub());
      registerApiClient(RuntimeApi, new RuntimeApiStub());
      registerApiClient(UserApi, new UserApiStub());
      fixture = TestBed.createComponent(WorkspaceWrapperComponent);
      setupModals(fixture);

      urlParamsStore.next({
        ns: WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
        wsid: WorkspaceStubVariables.DEFAULT_WORKSPACE_ID
      });
      serverConfigStore.next({gsuiteDomain: 'fake-research-aou.org',
        enableResearchReviewPrompt: true});
      cdrVersionStore.set(cdrVersionTiersResponse);
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
