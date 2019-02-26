import {Component, Input} from '@angular/core';
import {AlertDanger} from 'app/components/alert';
import {Button} from 'app/components/buttons';
import {TextInput, ValidationError} from 'app/components/inputs';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {WorkspaceData} from 'app/resolvers/workspace';
import {conceptSetsApi} from 'app/services/swagger-fetch-clients';
import {ReactWrapperBase, summarizeErrors, withCurrentWorkspace} from 'app/utils/index';
import {ConceptSet, Domain, DomainInfo} from 'generated/fetch';
import * as React from 'react';
import {validate} from 'validate.js';


export const CreateConceptSetModal = withCurrentWorkspace()
(class extends React.Component<{
  workspace: WorkspaceData,
  onCreate: Function,
  onClose: Function,
  conceptDomainList: Array<DomainInfo>,
  existingConceptSets: ConceptSet[],
}, {
  name: string,
  description: string,
  domain: Domain,
  nameTouched: boolean,
  saving: boolean,
  savingError: boolean,
}> {

  constructor(props) {
    super(props);
    this.state = {
      name: '',
      description: '',
      domain: props.conceptDomainList[0] as unknown as Domain,
      nameTouched: false,
      saving: false,
      savingError: false
    };
  }

  async saveConceptSet() {
    try {
      const {name, description, domain} = this.state;
      const {workspace} = this.props;
      const request = {conceptSet: {name, description, domain}};
      this.setState({saving: true});
      await conceptSetsApi().createConceptSet(workspace.namespace, workspace.id, request);
      this.props.onCreate();
      this.props.onClose();
    } catch (e) {
      console.error(e);
      this.setState({savingError: true});
    } finally {
      this.setState({saving: false});
    }
  }

  render() {
    const {onClose, conceptDomainList, existingConceptSets} = this.props;
    const {name, description, nameTouched, saving, savingError} = this.state;
    const errors = validate({name}, {
      name: {
        presence: {allowEmpty: false},
        exclusion: {
          within: existingConceptSets.map((concept: ConceptSet) => concept.name),
          message: 'already exists'
        }
      }
    });

    return <Modal>
      <ModalTitle>Create a Concept Set</ModalTitle>
      <ModalBody>
        <TextInput placeholder='Name' value={name} data-test-id='concept-set-name'
                   onChange={(v) => {this.setState({name: v, nameTouched: true}); }}/>
        <ValidationError>
          {summarizeErrors(nameTouched && errors && errors.name)}
        </ValidationError>
        <TextInput style={{marginTop: '1rem'}} placeholder='Description'
                   value={description} onChange={(v) => {this.setState({description: v}); }}/>
        <select style={{marginTop: '1rem', height: '1.5rem', width: '100%'}}
                data-test-id='domain-options'
                onChange={(e) => {this.setState({domain: e.target.value as unknown as Domain}); }}>
          {conceptDomainList.map((concept, i) => <option value={concept.domain} key={i}>
            {concept.domain}
          </option>)}
        </select>
        {savingError && <AlertDanger>Error creating Concept Set.</AlertDanger>}
        <ModalFooter>
          <Button type='secondary' onClick={onClose}>Cancel</Button>
          <Button style={{marginLeft: '0.5rem'}}
                  disabled={!!errors || saving}
                  data-test-id='save-concept-set'
                  onClick={() => this.saveConceptSet()}>Save</Button>
        </ModalFooter>
      </ModalBody>
    </Modal>;
  }
});

@Component({
  selector: 'app-create-concept-modal',
  template: '<div #root></div>',
})
export class CreateConceptSetModalComponent extends ReactWrapperBase {
  @Input('onCreate') onCreate: Function;
  @Input('onClose') onClose: Function;
  @Input('conceptDomainList') conceptDomainList: Array<DomainInfo> = [];
  @Input('existingConceptSets') existingConceptSets: ConceptSet[];
  constructor() {
    super(CreateConceptSetModal,
      ['onCreate', 'onClose', 'conceptDomainList', 'existingConceptSets']);
  }
}
