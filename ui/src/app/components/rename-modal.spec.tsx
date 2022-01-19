import { mount } from 'enzyme';
import * as React from 'react';

import { RenameModal } from './rename-modal';
import { ResourceType } from 'generated/fetch';

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
});
