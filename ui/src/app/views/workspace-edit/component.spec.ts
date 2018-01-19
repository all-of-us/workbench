import {ComponentFixture, fakeAsync, inject, TestBed, tick} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {By} from '@angular/platform-browser';
import {ActivatedRoute, Router} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from 'clarity-angular';

import {ErrorHandlingService} from 'app/services/error-handling.service';
import {WorkspaceEditComponent, WorkspaceEditMode} from 'app/views/workspace-edit/component';

import {ErrorHandlingServiceStub} from 'testing/stubs/error-handling-service-stub';
import {ProfileServiceStub, ProfileStubVariables} from 'testing/stubs/profile-service-stub';
import {WorkspacesServiceStub, WorkspaceStubVariables} from 'testing/stubs/workspace-service-stub';

import {ProfileService} from 'generated';
import {WorkspacesService} from 'generated';


describe('WorkspaceEditComponent', () => {
  let activatedRouteStub;
  let testComponent: WorkspaceEditComponent;
  let fixture: ComponentFixture<WorkspaceEditComponent>;
  let workspacesService: WorkspacesServiceStub;

  function setupComponent(mode: WorkspaceEditMode) {
    activatedRouteStub.routeConfig.data.mode = mode;
    fixture = TestBed.createComponent(WorkspaceEditComponent);
    testComponent = fixture.componentInstance;
    fixture.detectChanges();
    tick();
    fixture.detectChanges();
  }

  beforeEach(fakeAsync(() => {
    activatedRouteStub = {
      snapshot: {
        url: [
          {path: 'workspace'},
          {path: WorkspaceStubVariables.DEFAULT_WORKSPACE_NS},
          {path: WorkspaceStubVariables.DEFAULT_WORKSPACE_ID},
          {path: 'clone'}
        ],
        params: {
          'ns': WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
          'wsid': WorkspaceStubVariables.DEFAULT_WORKSPACE_ID
        }
      },
      routeConfig: {data: {}}
    };
    workspacesService = new WorkspacesServiceStub();
    TestBed.configureTestingModule({
      declarations: [
        WorkspaceEditComponent
      ],
      imports: [
        RouterTestingModule,
        FormsModule,
        ClarityModule.forRoot()
      ],
      providers: [
        { provide: ErrorHandlingService, useValue: new ErrorHandlingServiceStub() },
        { provide: WorkspacesService, useValue: workspacesService },
        // Wrap in a factory function so we can later mutate the value if needed
        // for testing.
        { provide: ActivatedRoute, useFactory: () => activatedRouteStub },
        { provide: ProfileService, useValue: new ProfileServiceStub() }
      ]
    }).compileComponents();
  }));


  it('should support updating a workspace', fakeAsync(() => {
    setupComponent(WorkspaceEditMode.Edit);
    testComponent.workspace.name = 'edited';
    fixture.detectChanges();
    expect(workspacesService.workspaces[0].name).not.toBe('edited');

    fixture.debugElement.query(By.css('.add-button'))
      .triggerEventHandler('click', null);
    fixture.detectChanges();
    tick();
    expect(workspacesService.workspaces.length).toBe(1);
    expect(workspacesService.workspaces[0].name).toBe('edited');
  }));

  it('should support creating a workspace', fakeAsync(() => {
    workspacesService.workspaces = [];
    setupComponent(WorkspaceEditMode.Create);

    testComponent.workspace.namespace = 'foo';
    testComponent.workspace.name = 'created';
    fixture.detectChanges();

    fixture.debugElement.query(By.css('.add-button'))
      .triggerEventHandler('click', null);
    fixture.detectChanges();
    tick();
    expect(workspacesService.workspaces.length).toBe(1);
    expect(workspacesService.workspaces[0].name).toBe('created');
  }));

  it('should support cloning a workspace', inject(
    [Router], fakeAsync((router: Router) => {
      setupComponent(WorkspaceEditMode.Clone);
      expect(testComponent.workspace.name).toBe(
        `Clone of ${WorkspaceStubVariables.DEFAULT_WORKSPACE_NAME}`);

      const spy = spyOn(router, 'navigate');
      fixture.debugElement.query(By.css('.add-button'))
        .triggerEventHandler('click', null);
      fixture.detectChanges();
      tick();

      expect(workspacesService.workspaces.length).toBe(2);
      const got = workspacesService.workspaces.find(w => w.name === testComponent.workspace.name);
      expect(got).not.toBeNull();
      expect(got.namespace).toBe(
        ProfileStubVariables.PROFILE_STUB.freeTierBillingProjectName);
      expect(spy).toHaveBeenCalled();
    })));
});
