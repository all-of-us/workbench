import * as React from 'react';
import { mount } from 'enzyme';

import { ResourceType } from 'generated/fetch';

import { appendAnalysisFileSuffixByOldName } from 'app/pages/analysis/util';

import { waitOneTickAndUpdate } from 'testing/react-test-helpers';

import { RenameModal } from './rename-modal';

describe(RenameModal.name, () => {
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
    ['123.R', '123'],
    ['123.R', '123.R'],
    ['test.Rmd', 'test'],
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

  let renameModal: RenameModal;

  beforeEach(() => {
    renameModal = new RenameModal({
      existingNames: [],
      oldName: '',
      onCancel: () => {},
      onRename: () => {},
      resourceType: ResourceType.NOTEBOOK,
    });
  });

  describe('validateNewName', () => {
    it('should return no errors for valid new name', () => {
      const errors = renameModal.validateNewName(
        ['existingName.ipynb'],
        'oldName.ipynb',
        'newName.ipynb',
        ResourceType.NOTEBOOK
      );
      expect(errors).toBeUndefined();
    });

    it('should return errors for duplicate new name', () => {
      const errors = renameModal.validateNewName(
        ['existingName.ipynb'],
        'oldName.ipynb',
        'existingName.ipynb',
        ResourceType.NOTEBOOK
      );
      expect(errors).toBeDefined();
      expect(errors.newName).toEqual(['New name already exists']);
    });

    it('should return errors for duplicate R file new name case insensitive', () => {
      const errors = renameModal.validateNewName(
        ['existingName.r'],
        'oldName.r',
        'existingName.R',
        ResourceType.NOTEBOOK
      );
      expect(errors).toBeDefined();
      expect(errors.newName).toEqual(['New name already exists']);
    });

    it('should return errors for duplicate R file new name case insensitive', () => {
      const errors = renameModal.validateNewName(
        ['existingname.R'],
        'oldName.r',
        'existingName.R',
        ResourceType.NOTEBOOK
      );
      expect(errors).toBeDefined();
      expect(errors.newName).toEqual(['New name already exists']);
    });

    it('should not return errors for rename R file case insensitive', () => {
      const errors = renameModal.validateNewName(
        ['oldName.r'],
        'oldName.r',
        'oldName.R',
        ResourceType.NOTEBOOK
      );
      expect(errors).toBeUndefined();
    });
  });
});
