import {
  FeaturedWorkspace,
  FeaturedWorkspaceCategory,
  FeaturedWorkspacesConfigApi,
  FeaturedWorkspacesConfigResponse
} from 'generated/fetch';
import {WorkspaceStubVariables} from './workspaces-api-stub';


const phenotypeWorkspace = {
  name: WorkspaceStubVariables.DEFAULT_WORKSPACE_NAME + ' Phenotype Library',
  namespace: WorkspaceStubVariables.DEFAULT_WORKSPACE_NS + ' Phenotype Library',
  id: WorkspaceStubVariables.DEFAULT_WORKSPACE_ID + ' Phenotype Library',
  category: FeaturedWorkspaceCategory.PHENOTYPELIBRARY
};

const tutorialWorkspace = {
  name: WorkspaceStubVariables.DEFAULT_WORKSPACE_NAME + ' Tutorial Workspace',
  namespace: WorkspaceStubVariables.DEFAULT_WORKSPACE_NS + ' Tutorial Workspace',
  id: WorkspaceStubVariables.DEFAULT_WORKSPACE_ID + ' Tutorial Workspace',
  category: FeaturedWorkspaceCategory.TUTORIALWORKSPACES
};

export class FeaturedWorkspacesConfigApiStub extends FeaturedWorkspacesConfigApi {
  featuredWorkspacesList: FeaturedWorkspace[];

  constructor() {
    super(undefined, undefined, (..._: any[]) => { throw Error('cannot fetch in tests'); });
    this.featuredWorkspacesList = [phenotypeWorkspace, tutorialWorkspace];
  }

  public getFeaturedWorkspacesConfig(): Promise<FeaturedWorkspacesConfigResponse> {
    return new Promise<FeaturedWorkspacesConfigResponse>(resolve => {
      resolve({featuredWorkspacesList: this.featuredWorkspacesList});
    });
  }
}
