import {Component} from '@angular/core';
import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {Router} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';

import {ClarityModule} from '@clr/angular';

import {ProfileStorageService} from 'app/services/profile-storage.service';
import {ServerConfigService} from 'app/services/server-config.service';
import {WorkspaceStorageService} from 'app/services/workspace-storage.service';

import {BugReportComponent} from 'app/views/bug-report/component';
import {ConfirmDeleteModalComponent} from 'app/views/confirm-delete-modal/component';
import {WorkspaceNavBarComponent} from 'app/views/workspace-nav-bar/component';
import {WorkspaceShareComponent} from 'app/views/workspace-share/component';
import {WorkspaceWrapperComponent} from 'app/views/workspace-wrapper/component';

import {UserService, WorkspaceAccessLevel, WorkspacesService} from 'generated';

import {ProfileStorageServiceStub} from 'testing/stubs/profile-storage-service-stub';
import {ServerConfigServiceStub} from 'testing/stubs/server-config-service-stub';
import {UserServiceStub} from 'testing/stubs/user-service-stub';
import {WorkspacesServiceStub, WorkspaceStubVariables} from 'testing/stubs/workspace-service-stub';
import {WorkspaceStorageServiceStub} from 'testing/stubs/workspace-storage-service-stub';

import {updateAndTick} from 'testing/test-helpers';

@Component({
  selector: 'app-test',
  template: '<router-outlet></router-outlet>'
})
class FakeAppComponent {}


describe('WorkspaceWrapperComponent', () => {
  let fixture: ComponentFixture<FakeAppComponent>;
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
        {provide: WorkspacesService, useValue: new WorkspacesServiceStub()},
        {provide: WorkspaceStorageService, useValue: new WorkspaceStorageServiceStub()},
      ]
    }).compileComponents().then(() => {
      fixture = TestBed.createComponent(FakeAppComponent);
      router = TestBed.get(Router);

      router.navigateByUrl(
        `/workspaces/${WorkspaceStubVariables.DEFAULT_WORKSPACE_NS}/` +
        WorkspaceStubVariables.DEFAULT_WORKSPACE_ID);
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
});
