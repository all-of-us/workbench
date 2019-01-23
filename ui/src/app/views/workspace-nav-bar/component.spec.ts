import {Component} from '@angular/core';
import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {By} from '@angular/platform-browser';
import {Router} from '@angular/router';
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
  simulateClick,
  updateAndTick
} from 'app/testing/test-helpers';

import {ProfileStorageService} from 'app/services/profile-storage.service';
import {ServerConfigService} from 'app/services/server-config.service';

import {BugReportComponent} from 'app/views/bug-report/component';
import {ConfirmDeleteModalComponent} from 'app/views/confirm-delete-modal/component';
import {WorkspaceNavBarComponent} from 'app/views/workspace-nav-bar/component';
import {WorkspaceShareComponent} from 'app/views/workspace-share/component';

@Component({
  selector: 'app-test',
  template: '<router-outlet></router-outlet>'
})
class FakeAppComponent {}

@Component({
  selector: 'app-fake-clone',
  template: 'clone'
})
class FakeCloneComponent {}

@Component({
  selector: 'app-fake-about',
  template: 'about'
})
class FakeAboutComponent {}

@Component({
  selector: 'app-fake-concepts',
  template: 'concepts'
})
class FakeConceptsComponent {}


@Component({
  selector: 'app-fake-notebooks',
  template: 'notebooks'
})
class FakeNotebooksComponent {}

@Component({
  selector: 'app-fake-cohorts',
  template: 'cohorts'
})
class FakeCohortsComponent {}

describe('WorkspaceNavBarComponent', () => {
  let fixture: ComponentFixture<FakeAppComponent>;
  let router: Router;
  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        FormsModule,
        ClarityModule.forRoot(),
        RouterTestingModule.withRoutes([{
          path: 'workspaces/:ns/:wsid',
          component: WorkspaceNavBarComponent,
          data: {
            workspace: {
              ...WorkspacesServiceStub.stubWorkspace(),
              accessLevel: WorkspaceAccessLevel.OWNER,
            }
          },
          children: [
            {
              path: '',
              component: FakeAboutComponent,
            },
            {
              path: 'clone',
              component: FakeCloneComponent,
            },
            {
              path: 'concepts',
              component: FakeConceptsComponent,
            },
            {
              path: 'notebooks',
              component: FakeNotebooksComponent,
            },
            {
              path: 'cohorts',
              component: FakeCohortsComponent,
            },
          ]
        }])
      ],
      declarations: [
        BugReportComponent,
        ConfirmDeleteModalComponent,
        FakeAboutComponent,
        FakeAppComponent,
        FakeCloneComponent,
        FakeConceptsComponent,
        FakeNotebooksComponent,
        FakeCohortsComponent,
        WorkspaceNavBarComponent,
        WorkspaceShareComponent,
      ],
      providers: [
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

  it('should highlight the active tab', fakeAsync(() => {
    const de = fixture.debugElement;
    const cohortsBtn = de.queryAll(By.css('.btn-top'))
      .find(btn => btn.nativeElement.textContent.includes('Cohorts'));
    expect(cohortsBtn).toBeTruthy();
    expect(cohortsBtn.query(By.css('.selected-tab-inner'))).toBeFalsy();
    simulateClick(fixture, cohortsBtn);

    updateAndTick(fixture);
    expect(cohortsBtn.query(By.css('.selected-tab-inner'))).toBeTruthy();
  }));

  it('should navigate on tab click', fakeAsync(() => {
    const de = fixture.debugElement;
    expect(de.query(By.css('app-fake-notebooks'))).toBeFalsy();

    const notebooksBtn = de.queryAll(By.css('.btn-top'))
      .find(btn => btn.nativeElement.textContent.includes('Notebooks'));
    simulateClick(fixture, notebooksBtn);

    updateAndTick(fixture);
    expect(de.query(By.css('app-fake-notebooks'))).toBeTruthy();
  }));


  it('should update on workspace navigate', fakeAsync(() => {
    const newId = 'my-new-wsid';
    router.navigateByUrl(`/workspaces/namespace/${newId}`);

    // Clarity needs several ticks/redraw cycles to render its button group.
    tick();
    updateAndTick(fixture);
    updateAndTick(fixture);

    const de = fixture.debugElement;
    simulateClick(fixture, de.query(By.css('.dropdown-toggle')));

    const cloneBtn = de.queryAll(By.css('clr-dropdown-menu button'))
      .find(b => b.nativeElement.textContent.includes('Clone'));
    simulateClick(fixture, cloneBtn);

    expect(de.query(By.css('app-fake-clone'))).toBeTruthy();
    expect(router.routerState.snapshot.url).toContain(newId);
  }));

  it('should close menu on action', fakeAsync(() => {
    const de = fixture.debugElement;
    simulateClick(fixture, de.query(By.css('.dropdown-toggle')));

    const cloneBtn = de.queryAll(By.css('clr-dropdown-menu button'))
      .find(b => b.nativeElement.textContent.includes('Clone'));
    simulateClick(fixture, cloneBtn);

    expect(de.queryAll(By.css('clr-dropdown-menu button')).length).toEqual(0);
  }));
});
