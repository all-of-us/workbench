import {
  EmptyResponse,
  WorkspaceAdminApi,
  WorkspaceListResponse,
} from 'generated/fetch';
import { stubNotImplementedError } from './stub-utils';
import { workspaceStubs } from './workspaces';

export class WorkspaceAdminApiStub extends WorkspaceAdminApi {
  constructor(public workspaces = workspaceStubs) {
    super(undefined, undefined, (..._: any[]) => {
      throw stubNotImplementedError;
    });
  }

  getWorkspacesForReview(): Promise<WorkspaceListResponse> {
    return new Promise<WorkspaceListResponse>((resolve) => {
      resolve({
        items: this.workspaces,
      });
    });
  }

  reviewWorkspace(): Promise<EmptyResponse> {
    return new Promise<EmptyResponse>((resolve) => {
      resolve({});
    });
  }
}
