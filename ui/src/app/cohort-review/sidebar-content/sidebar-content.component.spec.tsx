import {currentWorkspaceStore} from 'app/utils/navigation';
import {mount} from 'enzyme';
import * as React from 'react';
import {workspaceDataStub} from 'testing/stubs/workspaces-api-stub';

import {SidebarContent} from './sidebar-content.component';

describe('SidebarContent', () => {
  beforeEach(() => {
    currentWorkspaceStore.next(workspaceDataStub);
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
    />);
    expect(wrapper.exists()).toBeTruthy();
  });
});
