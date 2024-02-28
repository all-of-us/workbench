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
import { appendAnalysisFileSuffixByOldName } from 'app/pages/analysis/util';
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
  generateNewName(
    newName: string,
    oldName: string,
    resourceType: ResourceType
  ): string {
    // Append .ipynb, .Rmd, .R, .sas to the filename (if needed) based on the oldName format
    return resourceType === ResourceType.NOTEBOOK
      ? appendAnalysisFileSuffixByOldName(
          newName?.toLowerCase().trim(),
          oldName.toLowerCase()
        )
      : newName?.toLowerCase().trim();
  }

  validateNewName(
    newName: string,
    oldName: string,
    resourceType: ResourceType,
    existingNames: string[]
  ) {
    const lowerCaseExistingNames = existingNames.map((name) =>
      name.toLowerCase()
    );
    const formattedNewName = this.generateNewName(
      newName,
      oldName,
      resourceType
    );
    const errors = validate(
      { newName: formattedNewName },
      { newName: nameValidationFormat(lowerCaseExistingNames, resourceType) }
    );
    return errors;
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
      newName,
      oldName,
      resourceType,
      existingNames
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
