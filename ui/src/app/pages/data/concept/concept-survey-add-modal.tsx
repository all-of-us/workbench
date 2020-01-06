import * as React from 'react';

import {Button} from 'app/components/buttons';
import {FlexRow} from 'app/components/flex';
import {RadioButton, TextInput, ValidationError} from 'app/components/inputs';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {Spinner} from 'app/components/spinners';
import {conceptSetsApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {reactStyles, summarizeErrors, withCurrentWorkspace} from 'app/utils';
import {WorkspaceData} from 'app/utils/workspace-data';
import {
  ConceptSet,
  CreateConceptSetRequest,
  Domain,
  SurveyQuestions,
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


export const ConceptSurveyAddModal = withCurrentWorkspace()
(class extends React.Component<{
  workspace: WorkspaceData,
  selectedSurvey: Array<SurveyQuestions>,
  onSave: Function,
  onClose: Function,
  surveyName: string
}, {
  addingToExistingSet: boolean;
  conceptSets: ConceptSet[];
  errorSaving: boolean;
  loading: boolean;
  name: string;
  nameTouched: boolean;
  newSetDescription: string;
  saving: boolean;
  selectedSet: ConceptSet;
}> {

  constructor(props) {
    super(props);
    this.state = {
      addingToExistingSet: true,
      conceptSets: [],
      errorSaving: false,
      loading: true,
      name: '',
      nameTouched: false,
      newSetDescription: '',
      saving: false,
      selectedSet: null
    };
  }

  componentDidMount() {
    this.getExistingSurveyConceptSets();
  }

  async getExistingSurveyConceptSets() {
    try {
      const {workspace: {namespace, id}} = this.props;
      const conceptSets = await conceptSetsApi().getSurveyConceptSetsInWorkspace(namespace, id,
        this.props.surveyName);
      const conceptSetsInSurveys = conceptSets.items
          .filter((conceptset) => conceptset.domain === Domain.OBSERVATION);

      this.setState({
        conceptSets: conceptSetsInSurveys,
        addingToExistingSet: conceptSetsInSurveys.length > 0,
        loading: false,
      });
      if (conceptSetsInSurveys) {
        this.setState({selectedSet: conceptSetsInSurveys[0]});
      }
    } catch (error) {
      console.error(error);
    }
  }

  async saveConcepts() {
    const {workspace: {namespace, id}, onSave} = this.props;
    const {selectedSet, addingToExistingSet, newSetDescription, name} = this.state;
    this.setState({saving: true});
    const conceptIds = this.props.selectedSurvey.map((surveys) => surveys.conceptId);
    let survey = Surveys.THEBASICS;
    switch (this.props.surveyName) {
      case 'Lifestyle': survey = Surveys.LIFESTYLE; break;
      case 'Overall Health': survey = Surveys.OVERALLHEALTH; break;
      case 'The Basics': survey = Surveys.THEBASICS; break;
      default: {console.error('Survey name not found'); return; }
    }
    if (addingToExistingSet) {
      const updateConceptSetReq: UpdateConceptSetRequest = {
        etag: selectedSet.etag,
        addedIds: conceptIds
      };
      try {
        const conceptSet = await conceptSetsApi().updateConceptSetConcepts(
          namespace, id, selectedSet.id, updateConceptSetReq);
        this.setState({saving: false});
        onSave(conceptSet);
      } catch (error) {
        console.error(error);
      }
    } else {
      const conceptSet: ConceptSet = {
        name: name,
        description: newSetDescription,
        domain: Domain.SURVEY,
        survey: survey
      };
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


  render() {
    const {onClose, selectedSurvey, surveyName} = this.props;
    const {conceptSets, loading, name, nameTouched, addingToExistingSet} = this.state;
    const errors = validate({name}, {
      name: {
        presence: {allowEmpty: false},
        exclusion: {
          within: conceptSets.map((concept: ConceptSet) => concept.name),
          message: 'already exists'
        }
      }
    });
    return <Modal><ModalTitle data-test-id='add-concept-title'>
      Add {selectedSurvey.length} Survey to
      {' '}{surveyName} Concept Set</ModalTitle>
      {loading ?
          <div style={{display: 'flex', justifyContent: 'center'}}>
            <Spinner style={{alignContent: 'center'}}/>
          </div> :
          <ModalBody>
            <FlexRow>
              <TooltipTrigger content={
                <div>No concept sets in survey {surveyName} </div>}
                              disabled={conceptSets.length > 0}>
                <div>
                  <RadioButton value={addingToExistingSet}
                               checked={addingToExistingSet}
                               disabled={conceptSets.length === 0}
                               data-test-id='toggle-existing-set'
                               onChange={() => {
                                 this.setState({
                                   addingToExistingSet: true,
                                   nameTouched: false
                                 });
                               }}/>
                  <label style={styles.label}>Choose existing set</label>
                </div>
              </TooltipTrigger>
              <div>
                <RadioButton value={!addingToExistingSet}
                             checked={!addingToExistingSet}
                             style={{marginLeft: '0.7rem'}}
                             data-test-id='toggle-new-set'
                             onChange={() => {
                               this.setState({addingToExistingSet: false});
                             }}/>
                <label style={styles.label}>Create new set</label>
              </div>
            </FlexRow>
            {addingToExistingSet ? (
                    <ModalBody data-test-id='add-to-existing'>
                      <select style={{marginTop: '1rem', height: '1.5rem', width: '100%'}}
                              onChange={
                                (e) => this.setState({selectedSet: conceptSets[e.target.value]})}>
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
                            value={this.state.newSetDescription}
                            onChange={(v) => {
                              this.setState({newSetDescription: v.target.value});
                            }}/>
                </ModalBody>)}

            <ModalFooter>
              <Button type='secondary' onClick={onClose}>Cancel</Button>
              <Button style={{marginLeft: '0.5rem'}}
                      disabled={(!addingToExistingSet && !!errors)}
                      data-test-id='save-concept-set'
                      onClick={() => this.saveConcepts()}>Save</Button>
            </ModalFooter>
          </ModalBody>

      }
    </Modal>;
  }

});
