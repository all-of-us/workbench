import {currentWorkspaceStore} from 'app/utils/navigation';
import {mount} from 'enzyme';
import {WorkspaceAccessLevel} from 'generated';
import * as React from 'react';
import {WorkspacesServiceStub} from 'testing/stubs/workspace-service-stub';

import {SidebarContent} from './sidebar-content.component';

describe('SidebarContent', () => {
  beforeEach(() => {
    currentWorkspaceStore.next({
      ...WorkspacesServiceStub.stubWorkspace(),
      accessLevel: WorkspaceAccessLevel.OWNER,
    });
  });

  it('should render', () => {
    const wrapper = mount(<SidebarContent
      participant={{}}
      setParticipant={() => {}}
      annotations={[]}
      annotationDefinitions={[]}
      setAnnotations={() => {}}
      openCreateDefinitionModal={() => {}}
      openEditDefinitionsModal={() => {}}
      workspace={{}}
    />);
    expect(wrapper.exists()).toBeTruthy();
  });
});
