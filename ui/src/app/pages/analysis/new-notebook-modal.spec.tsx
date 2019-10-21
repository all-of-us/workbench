import {mount} from 'enzyme';
import * as React from 'react';

import {NewNotebookModal} from './new-notebook-modal';

describe('NewNotebookModal', () => {
  it('should render', () => {
    const wrapper = mount(<NewNotebookModal
      onClose={() => {}}
      workspace={{name: 'a'}}
      existingNameList={[]}
    />);
    expect(wrapper.exists()).toBeTruthy();
  });
});
