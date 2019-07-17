import {FeaturedWorkspace, FeaturedWorkspacesConfigApi, FeaturedWorkspacesConfigResponse} from 'generated/fetch';
import {WorkspaceStubVariables} from './workspaces-api-stub';


const featuredWorkspace = {
  name: WorkspaceStubVariables.DEFAULT_WORKSPACE_NAME,
  namespace: WorkspaceStubVariables.DEFAULT_WORKSPACE_NS,
  id: WorkspaceStubVariables.DEFAULT_WORKSPACE_ID
};

export class FeaturedWorkspacesConfigApiStub extends FeaturedWorkspacesConfigApi {
  featuredWorkspacesList: FeaturedWorkspace[];

  constructor() {
    super(undefined, undefined, (..._: any[]) => { throw Error('cannot fetch in tests'); });
    this.featuredWorkspacesList = [featuredWorkspace];
  }

  public getFeaturedWorkspacesConfig(): Promise<FeaturedWorkspacesConfigResponse> {
    return new Promise<FeaturedWorkspacesConfigResponse>(resolve => {
      resolve({featuredWorkspacesList: this.featuredWorkspacesList});
    });
  }
}
