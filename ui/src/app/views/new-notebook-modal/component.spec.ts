import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {By} from '@angular/platform-browser';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';

import {ClarityModule} from '@clr/angular';

import {SignInService} from 'app/services/sign-in.service';
import {Kernels} from 'app/utils/notebook-kernels';

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
        FormsModule
      ],
      declarations: [
        NewNotebookModalComponent
      ],
      providers: [
        {provide: SignInService, useValue: new SignInServiceStub()}
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
  }));

  it('does not allow blank names', fakeAsync(() => {
    spyOn(window, 'open');
    updateAndTick(fixture);
    simulateClick(fixture, fixture.debugElement.query(By.css('.confirm-name-btn')));
    expect(window.open).not.toHaveBeenCalled();
  }));

  it('allows creation of Py notebooks', fakeAsync(() => {
    spyOn(window, 'open');
    const name = 'new-name-py';
    updateAndTick(fixture);
    simulateInput(fixture, fixture.debugElement.query(By.css('#new-name')), name);
    updateAndTick(fixture);
    simulateClick(fixture, fixture.debugElement.query(By.css('.confirm-name-btn')));
    const expectedUrl = `/workspaces/${WorkspaceStubVariables.DEFAULT_WORKSPACE_NS}/` +
      `${WorkspaceStubVariables.DEFAULT_WORKSPACE_ID}/` +
      `notebooks/create/?notebook-name=` + encodeURIComponent(name) +
      `&kernel-type=${Kernels.Python3}`;
    expect(window.open).toHaveBeenCalledWith(expectedUrl, '_blank');
  }));


  // TODO: Get this to work. for some reason nothing would register
  //       as an actual successful change of the select element.
  it('allows creation of R notebooks', fakeAsync(() => {
    spyOn(window, 'open');
    const name = 'new-name-r';
    updateAndTick(fixture);
    simulateInput(fixture, fixture.debugElement.query(By.css('#new-name')), name);
    fixture.componentInstance.kernelType = Kernels.R;
    updateAndTick(fixture);
    simulateClick(fixture, fixture.debugElement.query(By.css('.confirm-name-btn')));
    const expectedUrlR = `/workspaces/${WorkspaceStubVariables.DEFAULT_WORKSPACE_NS}/` +
      `${WorkspaceStubVariables.DEFAULT_WORKSPACE_ID}/` +
      `notebooks/create/?notebook-name=` + encodeURIComponent(name) +
      `&kernel-type=${Kernels.R}`;
    expect(window.open).toHaveBeenCalledWith(expectedUrlR, '_blank');
  }));
});
