import * as fp from 'lodash/fp';
import * as React from 'react';

import {AlertDanger} from 'app/components/alert';
import {Button} from 'app/components/buttons';
import {RadioButton, TextInput, ValidationError} from 'app/components/inputs';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {Spinner, SpinnerOverlay} from 'app/components/spinners';
import {conceptSetsApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {reactStyles, summarizeErrors, withCurrentWorkspace} from 'app/utils';
import {WorkspaceData} from 'app/utils/workspace-data';
import {
  Concept,
  ConceptSet,
  CreateConceptSetRequest,
  Domain,
  DomainCount,
  SurveyQuestionsResponse,
  Surveys,
  UpdateConceptSetRequest
} from 'generated/fetch';
import {validate} from 'validate.js';


const styles = reactStyles({
  label: {
    color: colors.primary,
    paddingLeft: '0.5rem',
    lineHeight: '19px',
    fontSize: '14px',
    fontWeight: 400
  }
});

interface Props {
  workspace: WorkspaceData;
  selectedDomain?: DomainCount;
  selectedConcepts?: Concept[];
  selectedSurvey?: Array<SurveyQuestionsResponse>;
  onSave: Function;
  onClose: Function;
  surveyName?: string;
}

interface State {
  conceptSets: ConceptSet[];
  errorSaving: boolean;
  errorMessage: string;
  addingToExistingSet: boolean;
  loading: boolean;
  nameTouched: boolean;
  newSetDescription: string;
  name: string;
  saving: boolean;
  selectedSet: ConceptSet;
  selectedConceptsInDomain: Concept[];
}

export const ConceptAddModal = withCurrentWorkspace()
(class extends React.Component<Props, State> {
  constructor(props) {
    super(props);
    this.state = {
      conceptSets: [],
      errorMessage: null,
      errorSaving: false,
      addingToExistingSet: true,
      loading: true,
      nameTouched: false,
      newSetDescription: '',
      name: '',
      saving: false,
      selectedSet: null,
      selectedConceptsInDomain: props.selectedConcepts ? props.selectedConcepts
          .filter((concept: Concept) =>
              concept.domainId === fp.capitalize(props.selectedDomain.domain)) : null
    };
  }

  componentDidMount() {
    this.getExistingConceptSets();
  }

  async getExistingConceptSets() {
    try {
      const {workspace: {namespace, id}, selectedDomain, surveyName} = this.props;
      let conceptSetResponse;
      if (this.isSurvey()) {
        conceptSetResponse = await conceptSetsApi().getSurveyConceptSetsInWorkspace(namespace, id,
          surveyName);
      } else {
        conceptSetResponse = await conceptSetsApi().getConceptSetsInWorkspace(namespace, id);
      }
      const conceptSetsInDomain = conceptSetResponse.items
          .filter((conceptset) => conceptset.domain === selectedDomain.domain);

      this.setState({
        conceptSets: conceptSetsInDomain,
        addingToExistingSet: conceptSetsInDomain.length > 0,
        loading: false,
      });
      if (conceptSetsInDomain) {
        this.setState({selectedSet: conceptSetsInDomain[0]});
      }
    } catch (error) {
      console.error(error);
    }
  }

  isSurvey() {
    return this.props.selectedDomain.domain === Domain.OBSERVATION;
  }

  async saveConcepts() {
    const {workspace: {namespace, id}, selectedSurvey, surveyName} = this.props;
    const {onSave, selectedDomain} = this.props;
    const {
      selectedSet, addingToExistingSet, newSetDescription,
      name, selectedConceptsInDomain
    } = this.state;
    this.setState({saving: true});
    let conceptIds = fp.map(selected => selected.conceptId, selectedConceptsInDomain);
    let survey = Surveys.THEBASICS;
    if (this.isSurvey()) {
      conceptIds = selectedSurvey.map((surveys) => surveys.conceptId);

      switch (surveyName) {
        case 'Lifestyle':
          survey = Surveys.LIFESTYLE;
          break;
        case 'Overall Health':
          survey = Surveys.OVERALLHEALTH;
          break;
        case 'The Basics':
          survey = Surveys.THEBASICS;
          break;
        default: {
          console.error('Survey name not found');
          return;
        }
      }
    }

    // This is added temporary until users can create concept sets of Domain PERSON,
    // in the meantime there will be default Demogrpahics Concept Set on DATA SET PAGE

    if (name === 'Demographics') {
      this.setState({
        errorMessage: 'Name Demographics cannot be used for creating a concept set',
        saving: false});
      return;
    }
    if (addingToExistingSet) {
      const updateConceptSetReq: UpdateConceptSetRequest = {
        etag: selectedSet.etag,
        addedIds: conceptIds
      };
      try {
        const conceptSet = await conceptSetsApi().updateConceptSetConcepts(namespace, id,
          selectedSet.id, updateConceptSetReq);
        this.setState({saving: false});
        onSave(conceptSet);
      } catch (error) {
        console.error(error);
      }
    } else {
      let conceptSet: ConceptSet = {
        name: name,
        description: newSetDescription,
        domain: selectedDomain.domain
      };
      if (this.isSurvey()) {
        conceptSet = {
          ...conceptSet,
          survey: survey
        };
      }
      const request: CreateConceptSetRequest = {
        conceptSet: conceptSet,
        addedIds: conceptIds
      };
      try {
        const createdConceptSet =
            await conceptSetsApi().createConceptSet(namespace, id, request);
        this.setState({saving: false});
        onSave(createdConceptSet);
      } catch (error) {
        console.error(error);
        this.setState({errorSaving: true});
      }
    }
  }

  get Title() {
    const {selectedDomain, selectedSurvey, surveyName} = this.props;
    const {selectedConceptsInDomain} = this.state;

    return !this.isSurvey() ? 'Add ' + selectedConceptsInDomain.length + ' Concepts to ' +
      selectedDomain.name  + ' Concept Set' : 'Add ' + selectedSurvey.length +
        ' Survey to\n      ' + surveyName + ' Concept Set';
  }

  render() {
    const {selectedDomain, surveyName, onClose} = this.props;
    const {conceptSets, errorMessage, loading, nameTouched, saving, addingToExistingSet,
      newSetDescription, name, errorSaving} = this.state;
    const errors = validate({name}, {
      name: {
        presence: {allowEmpty: false},
        exclusion: {
          within: conceptSets.map((concept: ConceptSet) => concept.name),
          message: 'already exists'
        }
      }
    });

    return <Modal>
      <ModalTitle data-test-id='add-concept-title'>{this.Title}</ModalTitle>
      {loading ?
          <div style={{display: 'flex', justifyContent: 'center'}}>
            <Spinner style={{alignContent: 'center'}}/>
          </div> :
      <ModalBody>
        <ModalBody>
          <div style={{display: 'flex', flexDirection: 'row'}}>
            <TooltipTrigger content={
              <div>No concept sets in
                {this.isSurvey() ? ` survey ${surveyName}` : ` domain ${selectedDomain.name}`}
              </div>}
                            disabled={conceptSets.length > 0}>
              <div>
                <RadioButton value={addingToExistingSet}
                            checked={addingToExistingSet}
                            disabled={conceptSets.length === 0}
                            data-test-id='toggle-existing-set'
                            onChange={() => {this.setState({
                              addingToExistingSet: true,
                              nameTouched: false}); }}/>
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
            <TextInput placeholder='Name' value={name}
                       data-test-id='create-new-set-name'
                       onChange={(v) => {
                         this.setState({name: v, nameTouched: true});
                       }}/>
            <ValidationError>
              {summarizeErrors(nameTouched && errors && errors.name)}
            </ValidationError>
            <textarea style={{marginTop: '1rem'}} placeholder='Add a Description'
                      value={newSetDescription}
                      onChange={(v) => {
                        this.setState({newSetDescription: v.target.value});
                      }}/>
          </ModalBody>)}
        {errorSaving &&
          <AlertDanger>Error saving concepts to set; please try again!</AlertDanger>}
        {errorMessage && <AlertDanger>{errorMessage}</AlertDanger>}
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

