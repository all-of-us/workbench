import * as React from 'react';
import { mount } from 'enzyme';

import { waitOneTickAndUpdate } from 'testing/react-test-helpers';

import { NewJupyterNotebookModal } from './new-jupyter-notebook-modal';

describe('NewNotebookModal', () => {
  it('should show error is new name already exist', async () => {
    const wrapper = mount(
      <NewJupyterNotebookModal
        onClose={() => {}}
        workspace={{ name: 'a' }}
        existingNameList={['123.ipynb']}
      />
    );
    wrapper
      .find('[data-test-id="create-jupyter-new-name-input"]')
      .first()
      .simulate('change', { target: { value: '123' } });
    await waitOneTickAndUpdate(wrapper);
    expect(
      wrapper
        .find('[data-test-id="create-jupyter-new-name-invalid"]')
        .first()
        .text()
    ).toEqual('Name already exists');
  });
});
