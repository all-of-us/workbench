import {shallow} from 'enzyme';
import * as React from 'react';

import {currentWorkspaceStore} from 'app/utils/navigation';
import {workspaceDataStub} from 'testing/stubs/workspaces-api-stub';
import {GenderChart} from './gender-chart.component';


describe('GenderChart', () => {
  beforeEach(() => {
    currentWorkspaceStore.next(workspaceDataStub);
  });

  it('should render', () => {
    const wrapper = shallow(<GenderChart data={[]}/>);
    expect(wrapper.exists()).toBeTruthy();
  });
});
