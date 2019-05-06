import {mount} from 'enzyme';
import * as React from 'react';

import {SidebarContent} from './sidebar-content.component';

describe('SidebarContent', () => {
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
