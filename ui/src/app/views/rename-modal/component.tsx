import {
  Button
} from 'app/components/buttons';
import {TextInput, ValidationError} from 'app/components/inputs';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {summarizeErrors} from 'app/utils';
import * as React from 'react';
import {validate} from 'validate.js';

interface Props {
  existingNames: string[];
  oldName: string;
  onCancel: Function;
  onRename: Function;
  nameFormat?: Function;
  type: string;
}

interface States {
  newName: string;
  nameTouched: boolean;
}
export class RenameModal extends React.Component<Props, States> {
  constructor(props) {
    super(props);
    this.state = {
      newName: '',
      nameTouched: false
    };
  }

  render() {
    const {existingNames, oldName, type} = this.props;
    let {newName, nameTouched} = this.state;
    if (this.props.nameFormat) {
      newName = this.props.nameFormat(newName);
    }
    const errors = validate({newName: newName}, {newName: {
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
    return <Modal>
      <ModalTitle>Please enter new name for {oldName}</ModalTitle>
      <ModalBody>
        <div>New Name:</div>
        <TextInput autoFocus id='new-name'
                   onChange={v => this.setState({newName: v, nameTouched: true})}/>
        <ValidationError>
          {summarizeErrors(nameTouched && errors && errors.newName)}
        </ValidationError>
      </ModalBody>
      <ModalFooter>
        <Button type='secondary' onClick={() => this.props.onCancel()}>Cancel</Button>
        <TooltipTrigger content={summarizeErrors(errors)}>
          <Button data-test-id='rename-button' style={{marginLeft: '0.5rem'}}
                  onClick={() => this.props.onRename(newName)}>Rename {type}
          </Button>
        </TooltipTrigger>
      </ModalFooter>
    </Modal>;
  }
}
