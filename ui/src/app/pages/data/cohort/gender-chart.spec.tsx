import * as React from 'react';
import { shallow } from 'enzyme';

import { currentWorkspaceStore } from 'app/utils/navigation';

import { workspaceDataStub } from 'testing/stubs/workspaces';

import { GenderChart } from './gender-chart';

describe('GenderChart', () => {
  beforeEach(() => {
    currentWorkspaceStore.next(workspaceDataStub);
  });

  it('should render', () => {
    const wrapper = shallow(<GenderChart data={[]} />);
    expect(wrapper.exists()).toBeTruthy();
  });
});
