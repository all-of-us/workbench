import * as React from 'react';
import { mount } from 'enzyme';

import { NewJupyterNotebookModal } from './new-jupyter-notebook-modal';

describe('NewNotebookModal', () => {
  it('should render', () => {
    const wrapper = mount(
      <NewJupyterNotebookModal
        onClose={() => {}}
        workspace={{ name: 'a' }}
        existingNameList={[]}
      />
    );
    expect(wrapper.exists()).toBeTruthy();
  });
});
