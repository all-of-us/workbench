import {ComponentFixture, fakeAsync, TestBed} from '@angular/core/testing';
import {RouterTestingModule} from '@angular/router/testing';

import {NotebookFrameComponent} from './component';

import {Kernels} from 'app/utils/notebook-kernels';
import {WorkspacesServiceStub, WorkspaceStubVariables} from 'testing/stubs/workspace-service-stub';
import {updateAndTick} from 'testing/test-helpers';

describe('NotebookFrameComponent',  () => {
  let fixture: ComponentFixture<NotebookFrameComponent>;
  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule
      ],
      declarations: [
        NotebookFrameComponent
      ],
      providers: []
    }).compileComponents().then(() => {
      fixture = TestBed.createComponent(NotebookFrameComponent);
      fixture.componentInstance.workspace = WorkspacesServiceStub.stubWorkspace();
    });
  }));

  it('should render', fakeAsync(() => {
    updateAndTick(fixture);
    expect(fixture).toBeTruthy();
  }));

  it('should use correct url when opening existing notebook', fakeAsync(() => {
    fixture.componentInstance.route.snapshot.params = {nbName: 'Test Notebook'};
    updateAndTick(fixture);
    expect(fixture.componentInstance.jupyterUrl).toEqual('/workspaces/' +
      WorkspaceStubVariables.DEFAULT_WORKSPACE_NS + '/' +
      WorkspaceStubVariables.DEFAULT_WORKSPACE_ID + '/notebooks/Test Notebook');
  }));

  it('should use correct url when opening new notebook', fakeAsync(() => {
    fixture.componentInstance.route.snapshot.queryParams = {
      'notebook-name': 'Test New',
      kernelType: Kernels.Python3
    };
    updateAndTick(fixture);
    expect(fixture.componentInstance.jupyterUrl).toEqual('/workspaces/' +
      WorkspaceStubVariables.DEFAULT_WORKSPACE_NS + '/' +
      WorkspaceStubVariables.DEFAULT_WORKSPACE_ID + '/notebooks/create/?notebook-name=' +
      'Test%20New&kernel-type=' + Kernels.Python3);
  }));

});
