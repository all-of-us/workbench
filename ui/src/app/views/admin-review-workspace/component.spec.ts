import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';

import {ClarityModule} from '@clr/angular';

import {
  ProfileService,
  WorkspacesService
} from 'generated';

import {ProfileServiceStub} from 'testing/stubs/profile-service-stub';
import {ProfileStorageServiceStub} from 'testing/stubs/profile-storage-service-stub';
import {ServerConfigServiceStub} from 'testing/stubs/server-config-service-stub';
import {WorkspacesServiceStub} from 'testing/stubs/workspace-service-stub';

import {
  updateAndTick
} from 'testing/test-helpers';

import {ProfileStorageService} from 'app/services/profile-storage.service';
import {ServerConfigService} from 'app/services/server-config.service';

import {AdminReviewWorkspaceComponent} from 'app/views/admin-review-workspace/component';
import {BugReportComponent} from 'app/views/bug-report/component';

describe('AdminReviewWorkspaceComponent', () => {
  let fixture: ComponentFixture<AdminReviewWorkspaceComponent>;
  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        FormsModule,
        ClarityModule.forRoot()
      ],
      declarations: [
        AdminReviewWorkspaceComponent,
        BugReportComponent
      ],
      providers: [
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
      fixture = TestBed.createComponent(AdminReviewWorkspaceComponent);
      tick();
    });
  }));

  it('should render', fakeAsync(() => {
    updateAndTick(fixture);
    expect(fixture).toBeTruthy();
  }));
});
