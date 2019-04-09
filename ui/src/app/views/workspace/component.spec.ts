import {ComponentFixture, fakeAsync, TestBed} from '@angular/core/testing';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {Http} from '@angular/http';
import {By} from '@angular/platform-browser';
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
import {BugReportComponent} from 'app/views/bug-report/component';
import {ConfirmDeleteModalComponent} from 'app/views/confirm-delete-modal/component';
import {RecentWorkComponent} from 'app/views/recent-work/component';
import {ResourceCardComponent, ResourceCardMenuComponent} from 'app/views/resource-card/component';
import {ToolTipComponent} from 'app/views/tooltip/component';
import {TopBoxComponent} from 'app/views/top-box/component';
import {WorkspaceNavBarComponent} from 'app/views/workspace-nav-bar/component';
import {WorkspaceShareComponent} from 'app/views/workspace-share/component';
import {WorkspaceWrapperComponent} from 'app/views/workspace-wrapper/component';
import {WorkspaceComponent} from 'app/views/workspace/component';

import {
  ClusterService,
  CohortsService,
  ConceptSetsService,
  DataAccessLevel,
  ProfileService,
  UserMetricsService,
  UserService,
  WorkspaceAccessLevel,
  WorkspacesService
} from 'generated';
import {UserMetricsApi} from 'generated/fetch';
import {
  JupyterService,
  NotebooksService,
} from 'notebooks-generated';

import {CdrVersionStorageServiceStub} from 'testing/stubs/cdr-version-storage-service-stub';
import {ClusterServiceStub} from 'testing/stubs/cluster-service-stub';
import {CohortsServiceStub} from 'testing/stubs/cohort-service-stub';
import {ConceptSetsServiceStub} from 'testing/stubs/concept-sets-service-stub';
import {HttpStub} from 'testing/stubs/http-stub';
import {JupyterServiceStub} from 'testing/stubs/jupyter-service-stub';
import {NotebooksServiceStub} from 'testing/stubs/notebooks-service-stub';
import {ProfileServiceStub} from 'testing/stubs/profile-service-stub';
import {ProfileStorageServiceStub} from 'testing/stubs/profile-storage-service-stub';
import {ServerConfigServiceStub} from 'testing/stubs/server-config-service-stub';
import {UserMetricsApiStub} from 'testing/stubs/user-metrics-api-stub';
import {UserMetricsServiceStub} from 'testing/stubs/user-metrics-service-stub';
import {UserServiceStub} from 'testing/stubs/user-service-stub';
import {WorkspacesServiceStub} from 'testing/stubs/workspace-service-stub';

import {NewNotebookModalComponent} from 'app/views/new-notebook-modal/component';
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
        ResourceCardComponent,
        ResourceCardMenuComponent,
        ToolTipComponent,
        TopBoxComponent,
        WorkspaceComponent,
        WorkspaceNavBarComponent,
        WorkspaceWrapperComponent,
        WorkspaceShareComponent
      ],
      providers: [
        { provide: ClusterService, useValue: new ClusterServiceStub() },
        { provide: CohortsService, useValue: new CohortsServiceStub() },
        { provide: NotebooksService, useValue: new NotebooksServiceStub() },
        { provide: JupyterService, useValue: new JupyterServiceStub() },
        { provide: Http, useValue: new HttpStub() },
        { provide: ConceptSetsService, useValue: new ConceptSetsServiceStub() },
        { provide: ProfileStorageService, useValue: new ProfileStorageServiceStub() },
        { provide: SignInService, useValue: SignInService },
        { provide: WorkspacesService, useValue: new WorkspacesServiceStub() },
        { provide: UserMetricsService, useValue: new UserMetricsServiceStub() },
        { provide: ProfileService, useValue: new ProfileServiceStub() },
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
            gsuiteDomain: 'fake-research-aou.org'
          })
        }
      ]}).compileComponents().then(() => {
        fixture = TestBed.createComponent(WorkspaceComponent);
        updateAndTick(fixture);
      });
    currentWorkspaceStore.next({
      ...WorkspacesServiceStub.stubWorkspace(),
      accessLevel: WorkspaceAccessLevel.OWNER,
    });
    registerApiClient(UserMetricsApi, new UserMetricsApiStub());
  }));

  it('displays research purpose', fakeAsync(() => {
    expect(fixture.debugElement.queryAll(
      By.css('.research-purpose-item')).length).toEqual(2);
  }));
});
