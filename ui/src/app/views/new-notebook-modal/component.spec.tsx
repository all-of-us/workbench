import {mount} from 'enzyme';
import * as React from 'react';

import {NewNotebookModal} from './component';

describe('NewNotebookModal', () => {
  it('should render', () => {
    const wrapper = mount(<NewNotebookModal
      onClose={() => {}}
      workspace={{name: 'a'}}
      existingNotebooks={[]}
    />);
    expect(wrapper.exists()).toBeTruthy();
  });
});
