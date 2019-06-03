import {ComponentFixture, fakeAsync, TestBed} from '@angular/core/testing';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {Http} from '@angular/http';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';

import {IconsModule} from 'app/icons/icons.module';
import {CdrVersionStorageService} from 'app/services/cdr-version-storage.service';
import {ProfileStorageService} from 'app/services/profile-storage.service';
import {ServerConfigService} from 'app/services/server-config.service';
import {SignInService} from 'app/services/sign-in.service';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {BugReportComponent} from 'app/views/bug-report';
import {ConfirmDeleteModalComponent} from 'app/views/confirm-delete-modal';
import {RecentWorkComponent} from 'app/views/recent-work';
import {ResetClusterButtonComponent} from 'app/views/reset-cluster-button';
import {ToolTipComponent} from 'app/views/tooltip/component';
import {TopBoxComponent} from 'app/views/top-box/component';
import {WorkspaceNavBarComponent} from 'app/views/workspace-nav-bar';
import {WorkspaceShareComponent} from 'app/views/workspace-share';
import {WorkspaceWrapperComponent} from 'app/views/workspace-wrapper/component';
import {WorkspaceComponent} from 'app/views/workspace/component';
import {
  ClusterService,
  DataAccessLevel,
  UserMetricsService,
  UserService,
} from 'generated';
import {
  ClusterApi,
  CohortsApi,
  ConceptSetsApi,
  ProfileApi,
  UserMetricsApi,
  WorkspacesApi
} from 'generated/fetch';
import {
  JupyterService,
  NotebooksService,
} from 'notebooks-generated';

import {CdrVersionStorageServiceStub} from 'testing/stubs/cdr-version-storage-service-stub';
import {ClusterApiStub} from 'testing/stubs/cluster-api-stub';
import {ClusterServiceStub} from 'testing/stubs/cluster-service-stub';
import {CohortsApiStub} from 'testing/stubs/cohorts-api-stub';
import {ConceptSetsApiStub} from 'testing/stubs/concept-sets-api-stub';
import {HttpStub} from 'testing/stubs/http-stub';
import {JupyterServiceStub} from 'testing/stubs/jupyter-service-stub';
import {NotebooksServiceStub} from 'testing/stubs/notebooks-service-stub';
import {ProfileApiStub} from 'testing/stubs/profile-api-stub';
import {ProfileStorageServiceStub} from 'testing/stubs/profile-storage-service-stub';
import {ServerConfigServiceStub} from 'testing/stubs/server-config-service-stub';
import {UserMetricsApiStub} from 'testing/stubs/user-metrics-api-stub';
import {UserMetricsServiceStub} from 'testing/stubs/user-metrics-service-stub';
import {UserServiceStub} from 'testing/stubs/user-service-stub';
import {WorkspacesServiceStub} from 'testing/stubs/workspace-service-stub';
import {workspaceDataStub, WorkspacesApiStub} from 'testing/stubs/workspaces-api-stub';

import {NewNotebookModalComponent} from 'app/views/new-notebook-modal';
import {updateAndTick} from 'testing/test-helpers';


describe('WorkspaceComponent', () => {
  let fixture: ComponentFixture<WorkspaceComponent>;
  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        FormsModule,
        RouterTestingModule,
        BrowserAnimationsModule,
        IconsModule,
        ReactiveFormsModule,
        ClarityModule.forRoot()
      ],
      declarations: [
        BugReportComponent,
        ConfirmDeleteModalComponent,
        NewNotebookModalComponent,
        RecentWorkComponent,
        ResetClusterButtonComponent,
        ToolTipComponent,
        TopBoxComponent,
        WorkspaceComponent,
        WorkspaceNavBarComponent,
        WorkspaceWrapperComponent,
        WorkspaceShareComponent
      ],
      providers: [
        { provide: ClusterService, useValue: new ClusterServiceStub() },
        { provide: NotebooksService, useValue: new NotebooksServiceStub() },
        { provide: JupyterService, useValue: new JupyterServiceStub() },
        { provide: Http, useValue: new HttpStub() },
        { provide: ProfileStorageService, useValue: new ProfileStorageServiceStub() },
        { provide: SignInService, useValue: SignInService },
        { provide: UserMetricsService, useValue: new UserMetricsServiceStub() },
        { provide: UserService, useValue: new UserServiceStub() },
        {
          provide: CdrVersionStorageService,
          useValue: new CdrVersionStorageServiceStub({
            defaultCdrVersionId: WorkspacesServiceStub.stubWorkspace().cdrVersionId,
            items: [{
              name: 'cdr1',
              cdrVersionId: WorkspacesServiceStub.stubWorkspace().cdrVersionId,
              dataAccessLevel: DataAccessLevel.Registered,
              creationTime: 0
            }]
          })
        },
        {
          provide: ServerConfigService,
          useValue: new ServerConfigServiceStub({
            gsuiteDomain: 'fake-research-aou.org',
            useBillingProjectBuffer: false
          })
        }
      ]}).compileComponents().then(() => {
        fixture = TestBed.createComponent(WorkspaceComponent);
        updateAndTick(fixture);
      });
    currentWorkspaceStore.next(workspaceDataStub);
    registerApiClient(ClusterApi, new ClusterApiStub());
    registerApiClient(CohortsApi, new CohortsApiStub());
    registerApiClient(ConceptSetsApi, new ConceptSetsApiStub());
    registerApiClient(ProfileApi, new ProfileApiStub());
    registerApiClient(UserMetricsApi, new UserMetricsApiStub());
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
  }));

  // TODO [RW-2594] add tests.  Test for correct workspace purpose
  // details showing up in workspace about page.
  it('should render', fakeAsync(() => {
    expect(fixture).toBeTruthy();
  }));
});
