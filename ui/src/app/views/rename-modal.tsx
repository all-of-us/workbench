import {validate} from 'validate.js';

import {
  Button
} from 'app/components/buttons';
import {styles as headerStyles} from 'app/components/headers';
import {TextInput, ValidationError} from 'app/components/inputs';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {workspacesApi} from 'app/services/swagger-fetch-clients';

import {summarizeErrors} from 'app/utils';

import * as React from 'react';


const fullName = name => {
  return !name || /^.+\.ipynb$/.test(name) ? name : `${name}.ipynb`;
};

interface RenameModalProps {
  notebookName: string;
  workspace: {namespace: string, name: string};
  onRename: Function;
  onCancel: Function;
}

export class RenameModal extends React.Component<RenameModalProps, {
  saving: boolean;
  newName: string;
  existingNames: string[];
  nameTouched: boolean;
}> {
  constructor(props: RenameModalProps) {
    super(props);
    this.state = {
      saving: false,
      newName: '',
      existingNames: [],
      nameTouched: false
    };
  }

  async componentDidMount() {
    try {
      const {workspace} = this.props;
      const notebooks = await workspacesApi().getNoteBookList(workspace.namespace, workspace.name);
      this.setState({existingNames: notebooks.map(n => n.name)});
    } catch (error) {
      console.error(error); // TODO: better error handling
    }
  }

  private async rename() {
    try {
      const {onRename, workspace, notebookName} = this.props;
      const {newName} = this.state;
      this.setState({saving: true});
      await workspacesApi().renameNotebook(workspace.namespace, workspace.name, {
        name: notebookName,
        newName: fullName(newName)
      });
      onRename();
    } catch (error) {
      console.error(error); // TODO: better error handling
    } finally {
      this.setState({saving: false});
    }
  }

  render() {
    const {notebookName, onCancel} = this.props;
    const {saving, newName, existingNames, nameTouched} = this.state;
    const errors = validate({newName: fullName(newName)}, {newName: {
      presence: {allowEmpty: false},
      format: {
        pattern: /^[^\/]*$/,
        message: 'can\'t contain a slash'
      },
      exclusion: {
        within: existingNames,
        message: 'already exists'
      }
    }});
    return <Modal loading={saving}>
      <ModalTitle>Please enter the new name for {notebookName}</ModalTitle>
      <ModalBody>
        <div style={headerStyles.formLabel}>New Name:</div>
        <TextInput autoFocus id='new-name'
           onChange={v => this.setState({newName: v, nameTouched: true})}
        />
        <ValidationError>
          {summarizeErrors(nameTouched && errors && errors.newName)}
        </ValidationError>
      </ModalBody>
      <ModalFooter>
        <Button type='secondary' onClick={onCancel}>Cancel</Button>
        <TooltipTrigger content={summarizeErrors(errors)}>
          <Button
            data-test-id='rename-button'
            disabled={!!errors || saving}
            style={{marginLeft: '0.5rem'}}
            onClick={() => this.rename()}
          >Rename Notebook</Button>
        </TooltipTrigger>
      </ModalFooter>
    </Modal>;
  }
}
