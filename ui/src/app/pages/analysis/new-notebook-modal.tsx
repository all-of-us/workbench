import * as React from 'react';
import {validate} from 'validate.js';

import {Button} from 'app/components/buttons';
import {styles as headerStyles} from 'app/components/headers';
import {RadioButton, TextInput, ValidationError} from 'app/components/inputs';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {userMetricsApi} from 'app/services/swagger-fetch-clients';
import {summarizeErrors} from 'app/utils';
import {navigate} from 'app/utils/navigation';
import {Kernels} from 'app/utils/notebook-kernels';


import {FileDetail, Workspace} from 'generated/fetch';

export class NewNotebookModal extends React.Component<
  {onClose: Function, workspace: Workspace, existingNotebooks: FileDetail[]},
  {kernel: Kernels, name: string, nameTouched: boolean}
> {
  constructor(props) {
    super(props);
    this.state = {name: '', kernel: Kernels.Python3, nameTouched: false};
  }

  save() {
    const {workspace} = this.props;
    const {name, kernel} = this.state;
    userMetricsApi().updateRecentResource(workspace.namespace, workspace.id, {
      notebookName: `${name}.ipynb`
    });
    navigate(
      ['workspaces', workspace.namespace, workspace.id, 'notebooks', encodeURIComponent(name)],
      {queryParams: {kernelType: kernel, creating: true}}
    );
  }

  render() {
    const {onClose, existingNotebooks} = this.props;
    const {name, kernel, nameTouched} = this.state;
    const errors = validate({name, kernel}, {
      kernel: {presence: {allowEmpty: false}},
      name: {
        presence: {allowEmpty: false},
        exclusion: {
          within: existingNotebooks.map(fd => fd.name.slice(0, -6)),
          message: 'already exists'
        }
      }
    });
    return <Modal onRequestClose={onClose}>
      <ModalTitle>New Notebook</ModalTitle>
      <ModalBody>
        <div style={headerStyles.formLabel}>Name:</div>
        <TextInput
          autoFocus
          value={name}
          onChange={v => this.setState({ name: v, nameTouched: true })}
        />
        <ValidationError>
          {summarizeErrors(nameTouched && errors && errors.name)}
        </ValidationError>
        <div style={headerStyles.formLabel}>
          Programming Language:
        </div>
        <label style={{display: 'block'}}>
          <RadioButton
            checked={kernel === Kernels.Python3}
            onChange={() => this.setState({kernel: Kernels.Python3})}
          />
          &nbsp;Python 3
        </label>
        <label style={{display: 'block'}}>
          <RadioButton
            checked={kernel === Kernels.R}
            onChange={() => this.setState({kernel: Kernels.R})}
          />
          &nbsp;R
        </label>
      </ModalBody>
      <ModalFooter>
        <Button type='secondary' onClick={onClose}>Cancel</Button>
        <TooltipTrigger content={summarizeErrors(errors)}>
          <Button
            style={{marginLeft: '0.5rem'}}
            disabled={!!errors}
            onClick={() => this.save()}
          >Create Notebook</Button>
        </TooltipTrigger>
      </ModalFooter>
    </Modal>;
  }
}

