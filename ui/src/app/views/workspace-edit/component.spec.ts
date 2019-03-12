import {fakeAsync, TestBed} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';

import {currentWorkspaceStore, urlParamsStore} from 'app/utils/navigation';
import {BugReportComponent} from 'app/views/bug-report/component';
import {ConfirmDeleteModalComponent} from 'app/views/confirm-delete-modal/component';
import {WorkspaceEditComponent} from 'app/views/workspace-edit/component';
import {WorkspaceNavBarComponent} from 'app/views/workspace-nav-bar/component';
import {WorkspaceShareComponent} from 'app/views/workspace-share/component';
import {WorkspaceWrapperComponent} from 'app/views/workspace-wrapper/component';

import {WorkspacesServiceStub, WorkspaceStubVariables} from 'testing/stubs/workspace-service-stub';

import {ToolTipComponent} from 'app/views/tooltip/component';
import {
  WorkspaceAccessLevel
} from 'generated';


describe('WorkspaceEditComponent', () => {
  let workspacesService: WorkspacesServiceStub;

  beforeEach(fakeAsync(() => {
    urlParamsStore.next({
      ns: WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
      wsid: WorkspaceStubVariables.DEFAULT_WORKSPACE_ID
    });
    currentWorkspaceStore.next({
      ...WorkspacesServiceStub.stubWorkspace(),
      accessLevel: WorkspaceAccessLevel.OWNER,
    });
    workspacesService = new WorkspacesServiceStub();
    TestBed.configureTestingModule({
      declarations: [
        BugReportComponent,
        ConfirmDeleteModalComponent,
        ToolTipComponent,
        WorkspaceEditComponent,
        WorkspaceNavBarComponent,
        WorkspaceWrapperComponent,
        WorkspaceShareComponent
      ],
      imports: [
        BrowserAnimationsModule,
        RouterTestingModule,
        FormsModule,
        ClarityModule.forRoot()
      ],
      providers: [
      ]});
  }));

  // it('should show a conflict-specific error when creating a name conflict workspace',
  //   fakeAsync(() => {
  //     setupComponent(WorkspaceEditMode.Create);
  //     testComponent.workspace.namespace = WorkspaceStubVariables.DEFAULT_WORKSPACE_NS;
  //     testComponent.workspace.name = WorkspaceStubVariables.DEFAULT_WORKSPACE_NAME;
  //     testComponent.workspace.id = WorkspaceStubVariables.DEFAULT_WORKSPACE_ID;
  //     testComponent.workspace.description = WorkspaceStubVariables.DEFAULT_WORKSPACE_DESCRIPTION;
  //     const originalSize = workspacesService.workspaces.length;
  //     fixture.detectChanges();
  //     fixture.debugElement.query(By.css('.add-button'))
  //       .triggerEventHandler('click', null);
  //     updateAndTick(fixture);
  //     updateAndTick(fixture);
  //     expect(workspacesService.workspaces.length).toBe(originalSize);
  //     const modalTitle = fixture.debugElement.query(By.css('.modal-title'));
  //     const modalBody = fixture.debugElement.query(By.css('.modal-body'));
  //     expect(modalTitle.nativeElement.textContent).toEqual('Error:');
  //     const errorMsg = 'You already have a workspace named ' + testComponent.workspace.name
  //     + '. Please choose another name.';
  //     expect(modalBody.nativeElement.textContent).toEqual(errorMsg);
  //   }));
  //
  // it('should show a generic error when creating an id conflict workspace', fakeAsync(() => {
  //   setupComponent(WorkspaceEditMode.Create);
  //   testComponent.workspace.namespace = WorkspaceStubVariables.DEFAULT_WORKSPACE_NS;
  //   testComponent.workspace.name = 'non-default name';
  //   testComponent.workspace.id = WorkspaceStubVariables.DEFAULT_WORKSPACE_ID;
  //   testComponent.workspace.description = WorkspaceStubVariables.DEFAULT_WORKSPACE_DESCRIPTION;
  //   const originalSize = workspacesService.workspaces.length;
  //   fixture.detectChanges();
  //   fixture.debugElement.query(By.css('.add-button'))
  //     .triggerEventHandler('click', null);
  //   updateAndTick(fixture);
  //   updateAndTick(fixture);
  //   expect(workspacesService.workspaces.length).toBe(originalSize);
  //   const modalTitle = fixture.debugElement.query(By.css('.modal-title'));
  //   const modalBody = fixture.debugElement.query(By.css('.modal-body'));
  //   expect(modalTitle.nativeElement.textContent).toEqual('Error:');
  //   const errorMsg = 'Could not create workspace.';
  //   expect(modalBody.nativeElement.textContent).toEqual(errorMsg);
  // }));
  //
  // it('should support updating a workspace', fakeAsync(() => {
  //   setupComponent(WorkspaceEditMode.Edit);
  //   testComponent.workspace.name = 'edited';
  //   fixture.detectChanges();
  //   expect(workspacesService.workspaces[0].name).not.toBe('edited');
  //
  //   fixture.debugElement.query(By.css('.add-button'))
  //     .triggerEventHandler('click', null);
  //   fixture.detectChanges();
  //   tick();
  //   expect(workspacesService.workspaces.length).toBe(1);
  //   expect(workspacesService.workspaces[0].name).toBe('edited');
  // }));
  //
  // it('should support creating a workspace', fakeAsync(() => {
  //   NavStore.navigate = jasmine.createSpy('navigate');
  //   workspacesService.workspaces = [];
  //   setupComponent(WorkspaceEditMode.Create);
  //
  //   testComponent.workspace.namespace = 'foo';
  //   testComponent.workspace.name = 'created';
  //   testComponent.workspace.id = 'created';
  //   testComponent.workspace.description = 'description';
  //   fixture.detectChanges();
  //
  //   fixture.debugElement.query(By.css('.add-button'))
  //     .triggerEventHandler('click', null);
  //   fixture.detectChanges();
  //   tick();
  //   expect(workspacesService.workspaces.length).toBe(1);
  //   expect(workspacesService.workspaces[0].name).toBe('created');
  //   expect(NavStore.navigate)
  //     .toHaveBeenCalledWith(['workspaces', 'foo', 'created']);
  // }));
  //
  // it('should support cloning a workspace', fakeAsync(() => {
  //   workspacesService.workspaceAccess.set(
  //     WorkspaceStubVariables.DEFAULT_WORKSPACE_ID, WorkspaceAccessLevel.READER);
  //   setupComponent(WorkspaceEditMode.Clone);
  //   fixture.componentRef.instance.profileStorageService.reload();
  //   tick();
  //   expect(testComponent.workspace.name).toBe(
  //     `Duplicate of ${WorkspaceStubVariables.DEFAULT_WORKSPACE_NAME}`);
  //   expect(testComponent.hasPermission).toBeTruthy(
  //     'cloner should be able to edit cloned workspace');
  //
  //   NavStore.navigate = jasmine.createSpy('navigate');
  //   fixture.debugElement.query(By.css('.add-button'))
  //     .triggerEventHandler('click', null);
  //   fixture.detectChanges();
  //   tick();
  //   expect(workspacesService.workspaces.length).toBe(2);
  //   const got = workspacesService.workspaces.find(w => w.name === testComponent.workspace.name);
  //   expect(got).not.toBeNull();
  //   expect(got.namespace).toBe(
  //     ProfileStubVariables.PROFILE_STUB.freeTierBillingProjectName);
  //   expect(NavStore.navigate).toHaveBeenCalled();
  // }));
  //
  // it('should support cloning a workspace with cdr version change', fakeAsync(() => {
  //   workspacesService.workspaceAccess.set(
  //     WorkspaceStubVariables.DEFAULT_WORKSPACE_ID, WorkspaceAccessLevel.READER);
  //   setupComponent(WorkspaceEditMode.Clone);
  //   fixture.componentRef.instance.profileStorageService.reload();
  //   tick();
  //   const de = fixture.debugElement;
  //   simulateClick(fixture, de.query(By.css('.cdr-version-dropdown')));
  //   const cdr1Option = de.queryAll(By.css('.cdr-version-item'))
  //     .find(d => d.nativeElement.textContent.includes('cdr1'));
  //   simulateClick(fixture, cdr1Option);
  //
  //   NavStore.navigate = jasmine.createSpy('navigate');
  //   fixture.debugElement.query(By.css('.add-button'))
  //     .triggerEventHandler('click', null);
  //   fixture.detectChanges();
  //   tick();
  //   expect(workspacesService.workspaces.length).toBe(2);
  //   const got = workspacesService.workspaces.find(w => w.name === testComponent.workspace.name);
  //   expect(got).not.toBeNull();
  //   expect(got.cdrVersionId).toBe('1');
  //   expect(NavStore.navigate).toHaveBeenCalled();
  // }));
  //
  // it('should use default CDR version for workspace creation', fakeAsync(() => {
  //   workspacesService.workspaceAccess.set(
  //     WorkspaceStubVariables.DEFAULT_WORKSPACE_ID, WorkspaceAccessLevel.READER);
  //   setupComponent(WorkspaceEditMode.Create);
  //   fixture.componentRef.instance.profileStorageService.reload();
  //   tick();
  //
  //   const cdrDropdown = fixture.debugElement.query(By.css('.cdr-version-dropdown'));
  //   expect(cdrDropdown.nativeElement.textContent).toContain('cdr2');
  // }));
  //
  // it('should not create a workspace without description and fill later checkbox not selected',
  //   fakeAsync(() => {
  //     NavStore.navigate = jasmine.createSpy('navigate');
  //     workspacesService.workspaces = [];
  //     setupComponent(WorkspaceEditMode.Create);
  //
  //     testComponent.workspace.namespace = 'foo';
  //     testComponent.workspace.name = 'created';
  //     testComponent.workspace.id = 'created';
  //     testComponent.workspace.description = '';
  //     testComponent.fillDetailsLater = false;
  //     fixture.detectChanges();
  //
  //     fixture.debugElement.query(By.css('.add-button'))
  //       .triggerEventHandler('click', null);
  //     fixture.detectChanges();
  //     tick();
  //     expect(workspacesService.workspaces.length).toBe(0);
  //   }));
  //
  // it('should create a workspace without description and fill later checkbox selected',
  //   fakeAsync(() => {
  //     NavStore.navigate = jasmine.createSpy('navigate');
  //     workspacesService.workspaces = [];
  //     setupComponent(WorkspaceEditMode.Create);
  //
  //     testComponent.workspace.namespace = 'foo';
  //     testComponent.workspace.name = 'created';
  //     testComponent.workspace.id = 'created';
  //     testComponent.workspace.description = '';
  //     testComponent.fillDetailsLater = true;
  //     fixture.detectChanges();
  //
  //     fixture.debugElement.query(By.css('.add-button'))
  //       .triggerEventHandler('click', null);
  //     fixture.detectChanges();
  //     tick();
  //     expect(workspacesService.workspaces.length).toBe(1);
  //     expect(workspacesService.workspaces[0].name).toBe('created');
  //     expect(workspacesService.workspaces[0].description).toBe('');
  //   }));
  //
  // it('should not create a workspace with name greater than 80 characters', fakeAsync(() => {
  //   NavStore.navigate = jasmine.createSpy('navigate');
  //   workspacesService.workspaces = [];
  //   setupComponent(WorkspaceEditMode.Create);
  //   simulateInput(fixture, fixture.debugElement.query(By.css('.input.name')),
  //     'this is more than 80 characters look at how long this is ' +
  //     'and why dont I add some more characters and make this really very long');
  //   simulateInput(fixture, fixture.debugElement.query(By.css('.input.description')),
  //     'foo');
  //   testComponent.fillDetailsLater = false;
  //   fixture.detectChanges();
  //   tick();
  //   expect(fixture.debugElement.query(By.css('.add-button')).properties.disabled).toBeTruthy();
  // }));
  //
  // it('should allow editing unset underserved population on clone', fakeAsync(() => {
  //   NavStore.navigate = jasmine.createSpy('navigate');
  //   setupComponent(WorkspaceEditMode.Clone);
  //   const de = fixture.debugElement;
  //   simulateClick(fixture, de.query(By.css('.underserved-icon')));
  //   fixture.detectChanges();
  //
  //   simulateClick(fixture, de.query(By.css(
  //     '.underserved-checkbox[ng-reflect-name="AGE_OLDER_ADULTS"]')));
  //   fixture.detectChanges();
  //
  //   simulateClick(fixture, de.query(By.css('.add-button')));
  //   tick();
  //
  //   expect(workspacesService.workspaces.length).toBe(2);
  //   expect(workspacesService.workspaces[1].researchPurpose.underservedPopulationDetails)
  //     .toContain(UnderservedPopulationEnum.AGEOLDERADULTS);
  // }));
  //
  // it('should not allow duplicate workspace name while cloning', fakeAsync(() => {
  //   workspacesService.workspaceAccess.set(
  //     WorkspaceStubVariables.DEFAULT_WORKSPACE_ID, WorkspaceAccessLevel.READER);
  //   setupComponent(WorkspaceEditMode.Clone);
  //   fixture.componentRef.instance.profileStorageService.reload();
  //   tick();
  //   let de = fixture.debugElement;
  //   const workspaceName = WorkspaceStubVariables.DEFAULT_WORKSPACE_NAME;
  //   simulateInput(fixture, de.query(By.css('.name')), workspaceName );
  //   fixture.debugElement.query(By.css('.add-button'))
  //     .triggerEventHandler('click', null);
  //   updateAndTick(fixture);
  //   updateAndTick(fixture);
  //   de = fixture.debugElement;
  //   const errorText = de.query(By.css('.modal-body')).childNodes[0].nativeNode.data;
  //   expect(errorText).toBe(
  //     'You already have a workspace named '
  //       + workspaceName + '. Please choose another name.');
  // }));
});
