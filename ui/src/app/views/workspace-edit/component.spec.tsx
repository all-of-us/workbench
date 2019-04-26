import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {mount} from 'enzyme';
import {WorkspaceAccessLevel} from 'generated';
import {CdrVersionsApi} from 'generated/fetch';
import * as React from 'react';
import {ClusterApiStub} from 'testing/stubs/cluster-api-stub';
import {WorkspacesServiceStub} from 'testing/stubs/workspace-service-stub';
import {WorkspaceEdit, WorkspaceEditMode} from './component';

let props: {};
const workspace = {
  ...WorkspacesServiceStub.stubWorkspace(),
  accessLevel: WorkspaceAccessLevel.OWNER,
};

const component = () => {
  return mount(<WorkspaceEdit {...props}/>);
};

beforeEach(() => {
  registerApiClient(CdrVersionsApi, new ClusterApiStub());
  props = {workspace: workspace, routeConfigData: {mode: WorkspaceEditMode.Create}};
  currentWorkspaceStore.next(workspace);
});

it('displays workspaces edit page', () => {
  const wrapper = component();
  expect(wrapper).toBeTruthy();
});
