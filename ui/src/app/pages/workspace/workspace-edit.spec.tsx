import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {cdrVersionStore, currentWorkspaceStore} from 'app/utils/navigation';
import {mount} from 'enzyme';
import {CdrVersionsApi, WorkspaceAccessLevel} from 'generated/fetch';
import * as React from 'react';
import {ClusterApiStub} from 'testing/stubs/cluster-api-stub';
import {WorkspaceEdit, WorkspaceEditMode} from './workspace-edit';
import {workspaceStubs} from 'testing/stubs/workspaces-api-stub';
import {cdrVersionListResponse} from 'testing/stubs/cdr-versions-api-stub';


let props: {};
const workspace = {
  ...workspaceStubs[0],
  accessLevel: WorkspaceAccessLevel.OWNER,
};

const component = () => {
  return mount(<WorkspaceEdit {...props}/>);
};

beforeEach(() => {
  registerApiClient(CdrVersionsApi, new ClusterApiStub());
  props = {workspace: workspace, routeConfigData: {mode: WorkspaceEditMode.Create}};
  currentWorkspaceStore.next(workspace);
  cdrVersionStore.next(cdrVersionListResponse);
});

it('displays workspaces edit page', () => {
  const wrapper = component();
  expect(wrapper).toBeTruthy();
});
