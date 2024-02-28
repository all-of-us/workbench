import * as React from 'react';
import { validate } from 'validate.js';

import { ResourceType } from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { styles as headerStyles } from 'app/components/headers';
import { TextInput, ValidationError } from 'app/components/inputs';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import { TooltipTrigger } from 'app/components/popups';
import colors from 'app/styles/colors';
import { reactStyles, summarizeErrors } from 'app/utils';
import { nameValidationFormat, toDisplay } from 'app/utils/resources';

const styles = reactStyles({
  fieldHeader: {
    fontSize: 14,
    color: colors.primary,
    fontWeight: 600,
    display: 'block',
  },
});

interface Props {
  hideDescription?: boolean;
  existingNames: string[];
  oldDescription?: string;
  oldName: string;
  onCancel: Function;
  onRename: Function;
  nameFormat?: Function;
  resourceType: ResourceType;
}

interface States {
  newName: string;
  nameTouched: boolean;
  resourceDescription: string;
  saving: boolean;
}
export class RenameModal extends React.Component<Props, States> {
  constructor(props) {
    super(props);
    this.state = {
      newName: '',
      nameTouched: false,
      resourceDescription: this.props.oldDescription,
      saving: false,
    };
  }

  getFileNameWithoutExtension(name: string) {
    const lastDotIndex = name?.lastIndexOf('.');
    return lastDotIndex === -1 ? name : name?.slice(0, lastDotIndex);
  }

  getFileExtension(name: string) {
    const lastDotIndex = name?.lastIndexOf('.');
    return lastDotIndex === -1 ? '' : name?.slice(lastDotIndex);
  }

  validateNewName(
    existingNames: string[],
    oldName: string,
    newName: string,
    resourceType: ResourceType
  ) {
    const { lowerCaseNames, newNameNoExtension } =
      this.getFilteredListOfFileNamesWithoutExtension(
        existingNames,
        oldName,
        newName
      );

    return validate(
      { newName: newNameNoExtension },
      {
        newName: nameValidationFormat(lowerCaseNames, resourceType),
      }
    );
  }

  private getFilteredListOfFileNamesWithoutExtension(
    existingNames: string[],
    oldName: string,
    newName: string
  ) {
    const oldNameExtension = this.getFileExtension(oldName);
    const oldNameNoExtension = this.getFileNameWithoutExtension(oldName);

    const newNameExtension = this.getFileExtension(newName);
    let newNameNoExtension = this.getFileNameWithoutExtension(newName || '');

    // Filtering only files that have the same extension as the original file and stripping file extensions from the names
    const filteredExistingNamesNoExtension = existingNames
      .filter(
        (existingName) =>
          this.getFileExtension(existingName).toLowerCase() ===
          oldNameExtension.toLowerCase()
      )
      .map(this.getFileNameWithoutExtension);

    // No need to show error if the new name is the same as the old name but the extension has different case
    // User could be fixing a typo in the extension
    // Example: newName.r -> newName.R
    const isSameNameNoExtension = newNameNoExtension === oldNameNoExtension;
    const isDifferentNameCaseSensitive = newName !== oldName;
    const isSameExtensionCaseInsensitive =
      newNameExtension?.toLowerCase() === oldNameExtension.toLowerCase();

    if (
      isSameNameNoExtension &&
      isSameExtensionCaseInsensitive &&
      isDifferentNameCaseSensitive
    ) {
      const index =
        filteredExistingNamesNoExtension.indexOf(newNameNoExtension);
      if (index !== -1) {
        filteredExistingNamesNoExtension.splice(index, 1);
      }
    }

    // Make all names lowercase for case-insensitive comparison
    newNameNoExtension = newNameNoExtension.toLowerCase();
    const lowerCaseNames = filteredExistingNamesNoExtension.map((name) =>
      name.toLowerCase()
    );

    return { lowerCaseNames, newNameNoExtension };
  }

  onRename() {
    this.setState({ saving: true });
    this.props.onRename(
      this.state.newName.trim(),
      this.state.resourceDescription
    );
  }

  render() {
    const { hideDescription, existingNames, oldName, resourceType } =
      this.props;
    let { newName, nameTouched, resourceDescription, saving } = this.state;
    if (this.props.nameFormat) {
      newName = this.props.nameFormat(newName);
    }

    const errors = this.validateNewName(
      existingNames,
      oldName,
      newName,
      resourceType
    );
    return (
      <Modal loading={saving}>
        <ModalTitle>Enter new name for {oldName}</ModalTitle>
        <ModalBody>
          <div style={headerStyles.formLabel}>New Name:</div>
          <TextInput
            autoFocus
            id='new-name'
            data-test-id='rename-new-name-input'
            onChange={(v) => this.setState({ newName: v, nameTouched: true })}
          />
          <ValidationError data-test-id='rename-new-name-invalid'>
            {summarizeErrors(nameTouched && errors?.newName)}
          </ValidationError>
          {!hideDescription && (
            <div style={{ marginTop: '1.5rem' }}>
              <label data-test-id='descriptionLabel' style={styles.fieldHeader}>
                Description:{' '}
              </label>
              <textarea
                value={resourceDescription || ''}
                onChange={(e) =>
                  this.setState({ resourceDescription: e.target.value })
                }
              />
            </div>
          )}
        </ModalBody>
        <ModalFooter>
          <Button type='secondary' onClick={() => this.props.onCancel()}>
            Cancel
          </Button>
          <TooltipTrigger content={summarizeErrors(errors)}>
            <Button
              data-test-id='rename-button'
              disabled={!!errors || saving}
              style={{ marginLeft: '0.75rem' }}
              onClick={() => this.onRename()}
            >
              Rename {toDisplay(resourceType)}
            </Button>
          </TooltipTrigger>
        </ModalFooter>
      </Modal>
    );
  }
}
