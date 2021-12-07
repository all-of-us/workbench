import { EmptyResponse, ResearchPurposeReviewRequest, WorkspaceAdminApi, WorkspaceListResponse } from "generated/fetch";
import { stubNotImplementedError } from "./stub-utils";
import { workspaceStubs } from "./workspaces";


export class WorkspaceAdminApiStub extends WorkspaceAdminApi {

  constructor(public workspaces = workspaceStubs) {
    super(undefined, undefined, (..._: any[]) => { throw stubNotImplementedError; });
  }

  getWorkspacesForReview(options?: any): Promise<WorkspaceListResponse> {
    return new Promise<WorkspaceListResponse>(resolve => {
      resolve({
        items: this.workspaces
      });
    });
  }

  reviewWorkspace(workspaceNamespace: string, workspaceId: string,
    review?: ResearchPurposeReviewRequest): Promise<EmptyResponse> {
    return new Promise<EmptyResponse>(resolve => {
      resolve({});
    });
  }


}
