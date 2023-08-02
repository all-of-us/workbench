import * as React from 'react';
import { mount } from 'enzyme';

import { ResourceType } from 'generated/fetch';

import { appendAnalysisFileSuffixByOldName } from 'app/pages/analysis/util';

import { waitOneTickAndUpdate } from 'testing/react-test-helpers';

import { RenameModal } from './rename-modal';

describe('RenameModal', () => {
  const existingNames = [];

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

describe('should show error if new name already exist', () => {
  test.each([
    ['123.ipynb', '123'],
    ['123.ipynb', '123.ipynb'],
    ['123.Rmd', '123'],
    ['123.Rmd', '123.Rmd'],
  ])(
    'Old analysis file name %s, new analysis file name %s',
    async (oldFilename, newFilename) => {
      const wrapper = mount(
        <RenameModal
          onRename={() => {}}
          resourceType={ResourceType.NOTEBOOK}
          onCancel={() => {}}
          oldName={oldFilename}
          existingNames={[oldFilename]}
          nameFormat={(name) =>
            appendAnalysisFileSuffixByOldName(name, oldFilename)
          }
        />
      );
      wrapper
        .find('[data-test-id="rename-new-name-input"]')
        .first()
        .simulate('change', { target: { value: newFilename } });
      await waitOneTickAndUpdate(wrapper);
      expect(
        wrapper.find('[data-test-id="rename-new-name-invalid"]').first().text()
      ).toEqual('New name already exists');
    }
  );
});
