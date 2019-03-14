import {Component, Input} from '@angular/core';
import * as fp from 'lodash/fp';
import * as React from 'react';

import {AlertDanger} from 'app/components/alert';
import {Button} from 'app/components/buttons';
import {RadioButton, TextInput, ValidationError} from 'app/components/inputs';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {Spinner, SpinnerOverlay} from 'app/components/spinners';
import {WorkspaceData} from 'app/resolvers/workspace';
import {conceptSetsApi} from 'app/services/swagger-fetch-clients';
import {reactStyles, ReactWrapperBase, summarizeErrors, withCurrentWorkspace} from 'app/utils/index';
import {
  Concept,
  ConceptSet,
  CreateConceptSetRequest,
  DomainCount,
  UpdateConceptSetRequest
} from 'generated/fetch';
import {validate} from 'validate.js';


const styles = reactStyles({
  label: {
    color: '#000',
    paddingLeft: '0.5rem',
    lineHeight: '19px',
    fontSize: '14px',
    fontWeight: 400
  }
});


export const ConceptAddModal = withCurrentWorkspace()
(class extends React.Component<{
  workspace: WorkspaceData,
  selectedDomain: DomainCount,
  selectedConcepts: Concept[],
  onSave: Function,
  onClose: Function,
}, {
  conceptSets: ConceptSet[],
  errorSaving: boolean,
  addingToExistingSet: boolean,
  loading: boolean,
  nameTouched: boolean,
  newSetDescription: string,
  newSetName: string,
  saving: boolean,
  selectedSet: ConceptSet,
  selectedConceptsInDomain: Concept[]
}> {

  constructor(props) {
    super(props);
    this.state = {
      conceptSets: [],
      errorSaving: false,
      addingToExistingSet: true,
      loading: true,
      nameTouched: false,
      newSetDescription: '',
      newSetName: '',
      saving: false,
      selectedSet: null,
      selectedConceptsInDomain: props.selectedConcepts
          .filter((concept: Concept) =>
              concept.domainId === fp.capitalize(props.selectedDomain.domain))
    };
  }

  componentDidMount() {
    this.getExistingConceptSets();
  }

  async getExistingConceptSets() {
    try {
      const {workspace: {namespace, id}} = this.props;
      const conceptSets = await conceptSetsApi().getConceptSetsInWorkspace(namespace, id);
      const conceptSetsInDomain = conceptSets.items
          .filter((conceptset) => conceptset.domain === this.props.selectedDomain.domain);

      this.setState({
        conceptSets: conceptSetsInDomain,
        addingToExistingSet: (conceptSetsInDomain.length > 0),
        loading: false,
      });
      if (conceptSetsInDomain) {
        this.setState({selectedSet: conceptSetsInDomain[0]});
      }
    } catch (error) {
      console.error(error);
    }
  }

  async saveConcepts() {
    const {workspace: {namespace, id}} = this.props;
    const {onSave, selectedDomain} = this.props;
    const {selectedSet, addingToExistingSet, newSetDescription,
      newSetName, selectedConceptsInDomain} = this.state;
    this.setState({saving: true});
    const conceptIds = fp.map(selected => selected.conceptId, selectedConceptsInDomain);
    if (addingToExistingSet) {
      const updateConceptSetReq: UpdateConceptSetRequest = {
        etag: selectedSet.etag,
        addedIds: conceptIds
      };
      try {
        await conceptSetsApi().updateConceptSetConcepts(
          namespace, id, selectedSet.id, updateConceptSetReq);
        this.setState({saving: false});
        onSave();
      } catch (error) {
        console.error(error);
      }
    } else {
      const conceptSet: ConceptSet = {
        name: newSetName,
        description: newSetDescription,
        domain: selectedDomain.domain
      };
      const request: CreateConceptSetRequest = {
        conceptSet: conceptSet,
        addedIds: conceptIds
      };
      try {
        await conceptSetsApi().createConceptSet(namespace, id, request);
        this.setState({saving: false});
        onSave();
      } catch (error) {
        console.error(error);
        this.setState({errorSaving: true});
      }
    }
  }


  render() {
    const {selectedDomain, onClose} = this.props;
    const {conceptSets, loading, nameTouched, saving, addingToExistingSet,
      newSetDescription, newSetName, errorSaving, selectedConceptsInDomain} = this.state;
    const errors = validate({newSetName}, {
      newSetName: {
        presence: {allowEmpty: false},
        exclusion: {
          within: conceptSets.map((concept: ConceptSet) => concept.name),
          message: 'already exists'
        }
      }
    });

    return <Modal>
      <ModalTitle data-test-id='add-concept-title'>
        Add {selectedConceptsInDomain.length} Concepts to
        {' '}{selectedDomain.name} Concept Set</ModalTitle>
      {loading ?
          <div style={{display: 'flex', justifyContent: 'center'}}>
            <Spinner style={{alignContent: 'center'}}/>
          </div> :
      <ModalBody>
        <ModalBody>
          <div style={{display: 'flex', flexDirection: 'row'}}>
            <TooltipTrigger content={
              <div>No concept sets in domain '{selectedDomain.name}'</div>}
                            disabled={conceptSets.length > 0}>
              <div>
                <RadioButton value={addingToExistingSet}
                            checked={addingToExistingSet}
                            disabled={conceptSets.length === 0}
                            data-test-id='toggle-existing-set'
                            onChange={() => {this.setState({addingToExistingSet: true}); }}/>
                <label style={styles.label}>Choose existing set</label>
              </div>
            </TooltipTrigger>
            <div>
              <RadioButton value={!addingToExistingSet}
                         checked={!addingToExistingSet}
                         style={{marginLeft: '0.7rem'}}
                         data-test-id='toggle-new-set'
                         onChange={() => {this.setState({addingToExistingSet: false}); }}/>
              <label style={styles.label}>Create new set</label>
            </div>
          </div>
        </ModalBody>
        {addingToExistingSet ? (
            <ModalBody data-test-id='add-to-existing'>
              <select style={{marginTop: '1rem', height: '1.5rem', width: '100%'}}
                      onChange={(e) => this.setState({selectedSet: conceptSets[e.target.value]})}>
                {conceptSets.map((set: ConceptSet, i) =>
                    <option data-test-id='existing-set' key={i} value={i}>
                      {set.name}
                    </option>)}
              </select>
            </ModalBody>
            ) :
          (<ModalBody data-test-id='create-new-set'>
            <TextInput placeholder='Name' value={newSetName}
                       data-test-id='create-new-set-name'
                       onChange={(v) => {
                         this.setState({newSetName: v, nameTouched: true});
                       }}/>
            <ValidationError>
              {summarizeErrors(nameTouched && errors && errors.newSetName)}
            </ValidationError>
            <textarea style={{marginTop: '1rem'}} placeholder='Add a Description'
                      value={newSetDescription}
                      onChange={(v) => {
                        this.setState({newSetDescription: v.target.value});
                      }}/>
          </ModalBody>)}
        {errorSaving &&
          <AlertDanger>Error saving concepts to set; please try again!</AlertDanger>}
        <ModalFooter>
          <Button type='secondary' onClick={onClose}>Cancel</Button>
          <Button style={{marginLeft: '0.5rem'}}
                  disabled={(!addingToExistingSet && !!errors) || saving}
                  data-test-id='save-concept-set'
                  onClick={() => this.saveConcepts()}>Save</Button>
        </ModalFooter>
      </ModalBody>}
      {saving && <SpinnerOverlay/>}
    </Modal>;
  }

});


@Component({
  selector: 'app-concept-add-modal',
  template: '<div #root></div>',
})
export class ConceptAddModalComponent extends ReactWrapperBase {
  @Input('selectedDomain') selectedDomain: DomainCount;
  @Input('selectedConcepts') selectedConcepts: Concept[];
  @Input('onSave') onSave: Function;
  @Input('onClose') onClose: Function;
  constructor() {
    super(ConceptAddModal, ['selectedDomain', 'selectedConcepts', 'onSave', 'onClose']);
  }
}
