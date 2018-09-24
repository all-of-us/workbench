import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';

import {ClarityModule} from '@clr/angular';

import {
  BugReportService,
  UserService,
  WorkspaceAccessLevel,
  WorkspacesService,
} from 'generated';

import {BugReportServiceStub} from 'testing/stubs/bug-report-service-stub';
import {ProfileStorageServiceStub} from 'testing/stubs/profile-storage-service-stub';
import {ServerConfigServiceStub} from 'testing/stubs/server-config-service-stub';
import {UserServiceStub} from 'testing/stubs/user-service-stub';
import {WorkspacesServiceStub, WorkspaceStubVariables} from 'testing/stubs/workspace-service-stub';

import {
  updateAndTick
} from '../../../testing/test-helpers';

import {ProfileStorageService} from '../../services/profile-storage.service';
import {ServerConfigService} from '../../services/server-config.service';

import {BugReportComponent} from '../bug-report/component';
import {ConfirmDeleteModalComponent} from '../confirm-delete-modal/component';
import {WorkspaceNavBarComponent} from '../workspace-nav-bar/component';
import {WorkspaceShareComponent} from '../workspace-share/component';

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

describe('WorkspaceNavBarComponent', () => {
  let fixture: ComponentFixture<WorkspaceNavBarComponent>;
  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        FormsModule,
        RouterTestingModule,
        ClarityModule.forRoot()
      ],
      declarations: [
        BugReportComponent,
        ConfirmDeleteModalComponent,
        WorkspaceNavBarComponent,
        WorkspaceShareComponent,
      ],
      providers: [
        {provide: ActivatedRoute, useValue: activatedRouteStub},
        {provide: BugReportService, useValue: new BugReportServiceStub()},
        {provide: ProfileStorageService, useValue: new ProfileStorageServiceStub()},
        {provide: UserService, useValue: new UserServiceStub()},
        {
          provide: ServerConfigService,
          useValue: new ServerConfigServiceStub({
            gsuiteDomain: 'fake-research-aou.org'
          })
        },
        {provide: WorkspacesService, useValue: new WorkspacesServiceStub()},
      ]
    }).compileComponents().then(() => {
      fixture = TestBed.createComponent(WorkspaceNavBarComponent);
      tick();
    });
  }));

  it('should render', fakeAsync(() => {
    updateAndTick(fixture);
    expect(fixture).toBeTruthy();
  }));
});
