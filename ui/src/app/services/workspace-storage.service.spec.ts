import {fakeAsync, TestBed, tick} from '@angular/core/testing';

import {WorkspaceStorageService} from './workspace-storage.service';

import {WorkspacesServiceStub, WorkspaceStubVariables} from 'testing/stubs/workspace-service-stub';

import {WorkspacesService} from 'generated';

describe('WorkspaceStorageService', () => {
  let service: WorkspaceStorageService;
  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: WorkspacesService, useValue: new WorkspacesServiceStub() },
        WorkspaceStorageService,
      ]
    });
    service = TestBed.get(WorkspaceStorageService);
  }));

  it('loads a workspace if it doesn`t exist', fakeAsync(() => {
    spyOn(TestBed.get(WorkspacesService), 'getWorkspace')
      .and.callThrough();
    service.getWorkspace(WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
      WorkspaceStubVariables.DEFAULT_WORKSPACE_ID);
    tick();
    expect(TestBed.get(WorkspacesService).getWorkspace)
      .toHaveBeenCalledWith(WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
        WorkspaceStubVariables.DEFAULT_WORKSPACE_ID);
    expect(TestBed.get(WorkspacesService).getWorkspace).toHaveBeenCalledTimes(1);
  }));

  it('Caches a response', fakeAsync(() => {
    spyOn(TestBed.get(WorkspacesService), 'getWorkspace')
      .and.callThrough();
    service.getWorkspace(WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
      WorkspaceStubVariables.DEFAULT_WORKSPACE_ID);
    tick();
    service.getWorkspace(WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
      WorkspaceStubVariables.DEFAULT_WORKSPACE_ID);
    tick();
    expect(TestBed.get(WorkspacesService).getWorkspace).toHaveBeenCalledTimes(1);
  }));
});
