
import * as React from 'react';

import {validate} from 'validate.js';

import {dataSetApi} from 'app/services/swagger-fetch-clients';

import {AlertDanger} from 'app/components/alert';
import {Button} from 'app/components/buttons';
import {ValidationError} from 'app/components/inputs';
import {TextInput} from 'app/components/inputs';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {summarizeErrors} from 'app/utils/index';
import {
  DomainValuePair
} from 'generated/fetch';

export class NewDataSetModal extends React.Component<
  {includesAllParticipants: boolean, selectedConceptSetIds: number[],
    selectedCohortIds: number[], selectedValues: DomainValuePair[], workspaceNamespace: string,
    workspaceId: string, closeFunction: Function},
  {name: string, nameTouched: boolean, conflictDataSetName: boolean,
    missingDataSetInfo: boolean}
  > {
  constructor(props) {
    super(props);
    this.state = {name: '', nameTouched: false,
      conflictDataSetName: false, missingDataSetInfo: false};
  }

  async saveDataSet() {
    this.setState({nameTouched: true});
    if (!this.state.name) {
      return;
    }
    this.setState({conflictDataSetName: false, missingDataSetInfo: false });
    const request = {
      name: this.state.name,
      includesAllParticipants: this.props.includesAllParticipants,
      description: '',
      conceptSetIds: this.props.selectedConceptSetIds,
      cohortIds: this.props.selectedCohortIds,
      values: this.props.selectedValues
    };
    try {
      await dataSetApi().createDataSet(
        this.props.workspaceNamespace, this.props.workspaceId, request);
      this.props.closeFunction();
    } catch (e) {
      if (e.status === 409) {
        this.setState({conflictDataSetName: true});
      } else if (e.status === 400) {
        this.setState({missingDataSetInfo: true});
      }
    }
  }

  render() {
    const {name, nameTouched, conflictDataSetName, missingDataSetInfo} = this.state;

    const errors = validate({name}, {
      name: {
        presence: {allowEmpty: false}
      }
    });
    return <Modal>
      <ModalTitle>Save Dataset</ModalTitle>
      <ModalBody>
        <div>
          <ValidationError>
            {summarizeErrors(nameTouched && errors && errors.name)}
          </ValidationError>
          {conflictDataSetName &&
          <AlertDanger>DataSet with same name exist</AlertDanger>
          }
          {missingDataSetInfo &&
          <AlertDanger> Data state cannot save as some information is missing</AlertDanger>
          }
          <TextInput type='text' autoFocus placeholder='Dataset Name'
                     value = {name}
                     onChange={v => this.setState({name: v, nameTouched: true,
                       conflictDataSetName: false})}/>
        </div>
      </ModalBody>
      <ModalFooter>
        <Button onClick = {this.props.closeFunction}
                type='secondary' style={{marginRight: '2rem'}}>
          Cancel
        </Button>
        <Button type='primary' disabled={errors} onClick={() => this.saveDataSet()}>SAVE</Button>
      </ModalFooter>
    </Modal>;
  }
}
