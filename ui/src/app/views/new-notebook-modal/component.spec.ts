import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {By} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import {RouterTestingModule} from '@angular/router/testing';

import {ClarityModule} from '@clr/angular';

import {SignInService} from 'app/services/sign-in.service';
import {Kernels} from 'app/utils/notebook-kernels';
import {UserMetricsService} from 'generated';

import {UserMetricsServiceStub} from 'testing/stubs/user-metrics-service-stub';

import {SignInServiceStub} from 'testing/stubs/sign-in-service-stub';
import {
  WorkspacesServiceStub,
  WorkspaceStubVariables
} from 'testing/stubs/workspace-service-stub';
import {
  simulateClick,
  simulateInput,
  updateAndTick
} from 'testing/test-helpers';

import {NewNotebookModalComponent} from '../new-notebook-modal/component';

describe('NewNotebookModalComponent', () => {
  let fixture: ComponentFixture<NewNotebookModalComponent>;
  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        BrowserAnimationsModule,
        ClarityModule.forRoot(),
        FormsModule,
        RouterTestingModule
      ],
      declarations: [
        NewNotebookModalComponent
      ],
      providers: [
        {provide: SignInService, useValue: new SignInServiceStub()},
        {provide: UserMetricsService, useValue: new UserMetricsServiceStub()}
      ]
    }).compileComponents().then(() => {
      fixture = TestBed.createComponent(NewNotebookModalComponent);
      tick();
      fixture.componentInstance.existingNotebooks = WorkspacesServiceStub.stubNotebookList();
      fixture.componentInstance.workspace = WorkspacesServiceStub.stubWorkspace();
      fixture.componentInstance.open();
    });
  }));

  it('should render', fakeAsync(() => {
    updateAndTick(fixture);
    expect(fixture).toBeTruthy();
  }));

  it('errors if name exists', fakeAsync(() => {
    updateAndTick(fixture);
    simulateInput(fixture, fixture.debugElement.query(By.css('#new-name')), 'mockFile');
    updateAndTick(fixture);
    simulateClick(fixture, fixture.debugElement.query(By.css('.confirm-name-btn')));
    expect(fixture.debugElement.query(By.css('.error'))).toBeDefined();
    expect(fixture.debugElement.query(By.css('clr-modal')).classes['open']).toBeTruthy();
  }));

  it('does not allow blank names', fakeAsync(() => {
    updateAndTick(fixture);
    const button = fixture.debugElement.query(By.css('.confirm-name-btn'));
    expect(button.properties.disabled).toBeTruthy();
  }));

  it('allows creation of Python notebooks', fakeAsync(() => {
    const name = 'new-name-py';
    spyOn(fixture.componentInstance.route, 'navigate').and.returnValue(true);
    updateAndTick(fixture);
    simulateInput(fixture, fixture.debugElement.query(By.css('#new-name')), name);
    updateAndTick(fixture);
    simulateClick(fixture, fixture.debugElement.query(By.css('#py-radio')));
    updateAndTick(fixture);
    simulateClick(fixture, fixture.debugElement.query(By.css('.confirm-name-btn')));
    expect(fixture.componentInstance.route.navigate).toHaveBeenCalledWith(['workspaces',
      WorkspaceStubVariables.DEFAULT_WORKSPACE_NS, WorkspaceStubVariables.DEFAULT_WORKSPACE_ID,
      'notebooks', 'create', 'frame'], {queryParams: {'notebook-name': name,
      'kernelType': Kernels.Python3}, relativeTo: null});
    expect(fixture.debugElement.query(By.css('clr-modal')).classes['open']).toBeFalsy();
  }));

  it('allows creation of R notebooks', fakeAsync(() => {
    const name = 'new-name-r';
    spyOn(fixture.componentInstance.route, 'navigate').and.returnValue(true);
    updateAndTick(fixture);
    simulateInput(fixture, fixture.debugElement.query(By.css('#new-name')), name);
    updateAndTick(fixture);
    simulateClick(fixture, fixture.debugElement.query(By.css('#r-radio')));
    updateAndTick(fixture);
    simulateClick(fixture, fixture.debugElement.query(By.css('.confirm-name-btn')));
    expect(fixture.componentInstance.route.navigate).toHaveBeenCalledWith(['workspaces',
      WorkspaceStubVariables.DEFAULT_WORKSPACE_NS, WorkspaceStubVariables.DEFAULT_WORKSPACE_ID,
    'notebooks', 'create', 'frame'], {queryParams: {'notebook-name': name, 'kernelType': Kernels.R},
    relativeTo: null});
    expect(fixture.debugElement.query(By.css('clr-modal')).classes['open']).toBeFalsy();
  }));
});
