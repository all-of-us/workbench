import {
  FeaturedWorkspace,
  FeaturedWorkspaceCategory,
  FeaturedWorkspacesConfigApi,
  FeaturedWorkspacesConfigResponse,
} from 'generated/fetch';

import { WorkspaceStubVariables } from './workspaces';

const phenotypeWorkspace: FeaturedWorkspace = {
  name: WorkspaceStubVariables.DEFAULT_WORKSPACE_NAME + ' Phenotype Library',
  namespace: WorkspaceStubVariables.DEFAULT_WORKSPACE_NS + ' Phenotype Library',
  id:
    WorkspaceStubVariables.DEFAULT_WORKSPACE_TERRA_NAME + ' Phenotype Library',
  category: FeaturedWorkspaceCategory.PHENOTYPE_LIBRARY,
};

const tutorialWorkspace: FeaturedWorkspace = {
  name: WorkspaceStubVariables.DEFAULT_WORKSPACE_NAME + ' Tutorial Workspace',
  namespace:
    WorkspaceStubVariables.DEFAULT_WORKSPACE_NS + ' Tutorial Workspace',
  id:
    WorkspaceStubVariables.DEFAULT_WORKSPACE_TERRA_NAME + ' Tutorial Workspace',
  category: FeaturedWorkspaceCategory.TUTORIAL_WORKSPACES,
};

export class FeaturedWorkspacesConfigApiStub extends FeaturedWorkspacesConfigApi {
  featuredWorkspacesList: FeaturedWorkspace[];

  constructor() {
    super(undefined);
    this.featuredWorkspacesList = [phenotypeWorkspace, tutorialWorkspace];
  }

  public getFeaturedWorkspacesConfig(): Promise<FeaturedWorkspacesConfigResponse> {
    return new Promise<FeaturedWorkspacesConfigResponse>((resolve) => {
      resolve({ featuredWorkspacesList: this.featuredWorkspacesList });
    });
  }
}
