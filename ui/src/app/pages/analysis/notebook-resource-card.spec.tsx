import {mount} from 'enzyme';
import * as React from 'react';

import {NotebookResourceCard} from 'app/pages/analysis/notebook-resource-card';
import {WorkspaceAccessLevel} from 'generated/fetch';


describe('NotebookResourceCard', () => {
  const component = () => {
    const props = {
      notebook: {
        name: 'name'
      },
      permission: WorkspaceAccessLevel.WRITER
    };

    return mount(<NotebookResourceCard
      resource={props}
      existingNameList={[]}
      onUpdate={() => {}}/>);
  };

  it('should render', () => {
    const wrapper = component();
    expect(wrapper).toBeTruthy();
  });

});
