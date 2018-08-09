import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {RouterTestingModule} from '@angular/router/testing';

import {ClarityModule} from '@clr/angular';

import {
  BugReportService,
  ProfileService,
  WorkspacesService
} from 'generated';

import {BugReportServiceStub} from 'testing/stubs/bug-report-service-stub';
import {ProfileServiceStub} from 'testing/stubs/profile-service-stub';
import {ProfileStorageServiceStub} from 'testing/stubs/profile-storage-service-stub';
import {ServerConfigServiceStub} from 'testing/stubs/server-config-service-stub';
import {WorkspacesServiceStub} from 'testing/stubs/workspace-service-stub';

import {
  updateAndTick
} from '../../../testing/test-helpers';

import {ProfileStorageService} from '../../services/profile-storage.service';
import {ServerConfigService} from '../../services/server-config.service';

import {AdminReviewWorkspaceComponent} from '../admin-review-workspace/component';
import {BugReportComponent} from '../bug-report/component';

class AdminReviewWorkspacePage {
  fixture: ComponentFixture<AdminReviewWorkspaceComponent>;
  component: AdminReviewWorkspaceComponent;

  constructor(testBed: typeof TestBed) {
    this.fixture = testBed.createComponent(AdminReviewWorkspaceComponent);
    this.component = this.fixture.componentInstance;
    this.readPageData();
  }

  readPageData() {
    updateAndTick(this.fixture);
  }
}


describe('AdminReviewWorkspaceComponent', () => {
  let page: AdminReviewWorkspacePage;
  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        FormsModule,
        ClarityModule.forRoot()
      ],
      declarations: [
        AdminReviewWorkspaceComponent,
        BugReportComponent
      ],
      providers: [
        { provide: BugReportService, useValue: new BugReportServiceStub() },
        { provide: ProfileService, useValue: new ProfileServiceStub() },
        { provide: ProfileStorageService, useValue: new ProfileStorageServiceStub() },
        {
          provide: ServerConfigService,
          useValue: new ServerConfigServiceStub({
            gsuiteDomain: 'fake-research-aou.org'
          })
        },
        { provide: WorkspacesService, useValue: new WorkspacesServiceStub() }
      ]
    }).compileComponents().then(() => {
      page = new AdminReviewWorkspacePage(TestBed);
      tick();
    });
  }));

  it('should render', fakeAsync(() => {
    page.readPageData();
    expect(page.fixture).toBeTruthy();
  }));
});
