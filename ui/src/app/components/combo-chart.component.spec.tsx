import {shallow} from 'enzyme';
import * as React from 'react';

import {currentWorkspaceStore} from 'app/utils/navigation';
import {workspaceDataStub} from 'testing/stubs/workspaces-api-stub';
import {ComboChart} from './combo-chart.component';

describe('GenderChart', () => {
  beforeEach(() => {
    currentWorkspaceStore.next(workspaceDataStub);
  });

  it('should render', () => {
    const wrapper = shallow(<ComboChart data={[]} mode='percent'/>);
    expect(wrapper.exists()).toBeTruthy();
  });
});
