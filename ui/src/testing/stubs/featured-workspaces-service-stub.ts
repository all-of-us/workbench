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
    console.log(category);
    const altWorkspace = buildWorkspaceStubs(['Featured', 'category']);
    return new Promise((resolve, reject) => {
      resolve({
        items: altWorkspace.map((workspace) => {
          return {
            workspace: {
              ...workspace,
              featuredCategory: FeaturedWorkspaceCategory.COMMUNITY,
            },
            accessLevel: WorkspaceAccessLevel.OWNER,
          };
        }),
      }),
        reject('error');
    });
  }
}
