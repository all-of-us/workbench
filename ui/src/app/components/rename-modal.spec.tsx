import * as React from 'react';
import { mount } from 'enzyme';

import { ResourceType } from 'generated/fetch';

import { appendNotebookFileSuffixByOldName } from 'app/pages/analysis/util';

import { waitOneTickAndUpdate } from 'testing/react-test-helpers';

import { RenameModal } from './rename-modal';

describe('RenameModal', () => {
  const existingNames = [];

  it('should render', () => {
    const wrapper = mount(
      <RenameModal
        onRename={() => {}}
        resourceType={ResourceType.NOTEBOOK}
        onCancel={() => {}}
        oldName=''
        existingNames={existingNames}
        nameFormat={() => {}}
        hideDescription={true}
      />
    );
    expect(wrapper.exists()).toBeTruthy();
  });

  it('should display description only if props hideDescription is set to true', () => {
    const wrapper = mount(
      <RenameModal
        onRename={() => {}}
        resourceType={ResourceType.NOTEBOOK}
        onCancel={() => {}}
        oldName=''
        existingNames={existingNames}
        nameFormat={() => {}}
      />
    );
    expect(wrapper.exists()).toBeTruthy();
    expect(wrapper.find('[data-test-id="descriptionLabel"]')).toBeTruthy();
  });

  it('should show error is new name already exist', async () => {
    const wrapper = mount(
      <RenameModal
        onRename={() => {}}
        resourceType={ResourceType.NOTEBOOK}
        onCancel={() => {}}
        oldName='123.Rmd'
        existingNames={['123.Rmd']}
        nameFormat={(name) =>
          appendNotebookFileSuffixByOldName(name, '123.Rmd')
        }
      />
    );
    wrapper
      .find('[data-test-id="new-name-input"]')
      .first()
      .simulate('change', { target: { value: '123' } });
    await waitOneTickAndUpdate(wrapper);
    // expect(wrapper.find('[data-test-id="new-name-input"]').first().text()).toEqual("123")
    expect(
      wrapper.find('[data-test-id="new-name-invalid"]').first().text()
    ).toEqual('New name already exists');
  });
});
