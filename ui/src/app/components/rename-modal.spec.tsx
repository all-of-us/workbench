import '@testing-library/jest-dom';

import * as React from 'react';

import { ResourceType } from 'generated/fetch';

import { fireEvent, render, screen } from '@testing-library/react';
import { appendAnalysisFileSuffixByOldName } from 'app/pages/analysis/util';

import { RenameModal } from './rename-modal';
import { validateRenameModal } from './rename-modal-validation';

describe(RenameModal.name, () => {
  const existingNames = [];

  it('should display description only if props hideDescription is set to true', () => {
    const { getByTestId } = render(
      <RenameModal
        onRename={() => {}}
        resourceType={ResourceType.NOTEBOOK}
        onCancel={() => {}}
        oldName=''
        existingNames={existingNames}
        nameFormat={() => {}}
      />
    );
    expect(getByTestId('descriptionLabel')).toBeInTheDocument();
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
      const { getByTestId } = render(
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
      fireEvent.change(getByTestId('rename-new-name-input'), {
        target: { value: newFilename },
      });
      await screen.findByText('New name already exists');
    }
  );
});

describe('validateNewName', () => {
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

  it('should return errors when the new name for an R file is a duplicate of an existing file but with different case', () => {
    const errors = renameModal.validateNewName(
      ['existingName.r'],
      'oldName.r',
      'existingName.R',
      ResourceType.NOTEBOOK
    );
    expect(errors).toBeDefined();
    expect(errors.newName).toEqual(['New name already exists']);
  });

  it('should return errors when the new name for an R file is an exact match of an existing file', () => {
    const errors = renameModal.validateNewName(
      ['existingname.R'],
      'oldName.r',
      'existingName.R',
      ResourceType.NOTEBOOK
    );
    expect(errors).toBeDefined();
    expect(errors.newName).toEqual(['New name already exists']);
  });

  it('should not return errors when changing the case of the extension of an R file', () => {
    const errors = renameModal.validateNewName(
      ['oldName.r'],
      'oldName.r',
      'oldName.R',
      ResourceType.NOTEBOOK
    );
    expect(errors).toBeUndefined();
  });
});

describe('RenameModal form validation', () => {
  it('returns no errors for filled form', () => {
    // Arrange
    const newName = 'test';
    const exists = ['exists1', 'exists2'];

    // Act
    const errors = validateRenameModal(
      { newName },
      exists,
      ResourceType.NOTEBOOK
    );

    // Assert
    const expectedErrors = undefined; // no errors
    expect(errors).toEqual(expectedErrors);
  });

  it('returns errors for blank form', () => {
    // Arrange
    const newName = '';
    const exists = ['exists1', 'exists2'];

    // Act
    const errors = validateRenameModal(
      { newName },
      exists,
      ResourceType.NOTEBOOK
    );

    // Assert
    const expectedErrors: Record<string, string[]> = {
      newName: ["New name can't be blank"],
    };
    expect(errors).toEqual(expectedErrors);
  });

  it('returns error for bad char in name', () => {
    // Arrange
    const newName = 'this=is?bad';
    const exists = ['exists1', 'exists2'];

    // Act
    const errors = validateRenameModal(
      { newName },
      exists,
      ResourceType.NOTEBOOK
    );

    // Assert
    const expectedErrors: Record<string, string[]> = {
      newName: [
        "New name can't contain these characters: @ # $ % * + = ? , [ ] : ; / \\ ",
      ],
    };
    expect(errors).toEqual(expectedErrors);
  });

  it('returns error for name exists', () => {
    // Arrange
    const newName = 'exists2';
    const exists = ['exists1', 'exists2'];

    // Act
    const errors = validateRenameModal(
      { newName },
      exists,
      ResourceType.NOTEBOOK
    );

    // Assert
    const expectedErrors: Record<string, string[]> = {
      newName: ['New name already exists'],
    };
    expect(errors).toEqual(expectedErrors);
  });
});
