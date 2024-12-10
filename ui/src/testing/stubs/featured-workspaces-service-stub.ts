import {
  FeaturedWorkspaceApi,
  FeaturedWorkspaceCategory,
  WorkspaceAccessLevel,
  WorkspaceResponseListResponse,
} from 'generated/fetch';

import { buildWorkspaceStubs } from './workspaces';

export class FeaturedWorkspacesApiStub extends FeaturedWorkspaceApi {
  getFeaturedWorkspacesByCategory(
    category
  ): Promise<WorkspaceResponseListResponse> {
    const stubWorkspace = buildWorkspaceStubs([category, category + '1']);
    return new Promise((resolve, reject) => {
      resolve({
        items: stubWorkspace.map((workspace) => {
          return {
            workspace: {
              ...workspace,
              creatorUser: { userName: 'spec@fakeresearch.com' },
              featuredCategory: category as FeaturedWorkspaceCategory,
            },
            accessLevel: WorkspaceAccessLevel.OWNER,
          };
        }),
      }),
        reject('error');
    });
  }
}
