import * as React from 'react';
import * as fp from 'lodash/fp';
import { validate } from 'validate.js';

import {
  CardCount,
  ConceptSet,
  CreateConceptSetRequest,
  Criteria,
  Domain,
  ResourceType,
  UpdateConceptSetRequest,
} from 'generated/fetch';

import { AlertDanger } from 'app/components/alert';
import { Button } from 'app/components/buttons';
import { FlexRow } from 'app/components/flex';
import { RadioButton, TextInput, ValidationError } from 'app/components/inputs';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import { TooltipTrigger } from 'app/components/popups';
import { Spinner, SpinnerOverlay } from 'app/components/spinners';
import { conceptSetsApi } from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import { reactStyles, summarizeErrors, withCurrentWorkspace } from 'app/utils';
import { conceptSetUpdating } from 'app/utils/navigation';
import { nameValidationFormat } from 'app/utils/resources';
import { WorkspaceData } from 'app/utils/workspace-data';

const styles = reactStyles({
  label: {
    color: colors.primary,
    paddingLeft: '0.75rem',
    lineHeight: '19px',
    fontSize: '14px',
    fontWeight: 400,
  },
});

const filterConcepts = (concepts: any[], domain: Domain) => {
  return domain === Domain.SURVEY
    ? concepts.filter((concept) => concept.subtype === 'QUESTION')
    : concepts;
};

export const CONCEPT_SET_CONCEPT_LIMIT = 1000;

export const ConceptAddModal = withCurrentWorkspace()(
  class extends React.Component<
    {
      workspace: WorkspaceData;
      activeDomainTab: CardCount;
      selectedConcepts: Criteria[];
      onSave: Function;
      onClose: Function;
    },
    {
      conceptSets: ConceptSet[];
      errorMessage: string;
      errorSaving: boolean;
      addingToExistingSet: boolean;
      loading: boolean;
      loadingSelectedSet: boolean;
      nameTouched: boolean;
      newSetDescription: string;
      name: string;
      saving: boolean;
      selectedSet: ConceptSet;
      selectedSetCount: number;
      selectedConceptsInDomain: Criteria[];
    }
  > {
    constructor(props) {
      super(props);
      this.state = {
        conceptSets: [],
        errorMessage: null,
        errorSaving: false,
        addingToExistingSet: true,
        loading: true,
        loadingSelectedSet: false,
        nameTouched: false,
        newSetDescription: '',
        name: '',
        saving: false,
        selectedSet: null,
        selectedSetCount: null,
        selectedConceptsInDomain: filterConcepts(
          props.selectedConcepts,
          props.activeDomainTab.domain
        ),
      };
    }

    componentDidMount() {
      this.getExistingConceptSets();
    }

    async getExistingConceptSets() {
      try {
        const {
          activeDomainTab: { domain },
          selectedConcepts,
          workspace: { namespace, id },
        } = this.props;
        const conceptSets = await conceptSetsApi().getConceptSetsInWorkspace(
          namespace,
          id
        );
        const conceptSetsInDomain = conceptSets.items.filter(
          (conceptSet) => conceptSet.domain === domain
        );
        this.setState({
          conceptSets: conceptSetsInDomain,
          addingToExistingSet: conceptSetsInDomain.length > 0,
          selectedConceptsInDomain: filterConcepts(selectedConcepts, domain),
          loading: false,
        });
        if (conceptSetsInDomain?.[0]) {
          this.selectConceptSet(conceptSetsInDomain[0]);
        }
      } catch (error) {
        console.error(error);
      }
    }

    async saveConcepts() {
      const {
        activeDomainTab: { domain },
        onSave,
        workspace: { namespace, id },
      } = this.props;
      const {
        selectedSet,
        addingToExistingSet,
        newSetDescription,
        name,
        selectedConceptsInDomain,
      } = this.state;
      conceptSetUpdating.next(true);
      this.setState({ saving: true });
      const conceptIds = fp.map(
        (selected) => ({
          conceptId: selected.conceptId,
          standard: selected.isStandard,
        }),
        selectedConceptsInDomain
      );

      // This is added temporary until users can create concept sets of Domain PERSON,
      // in the meantime there will be default Demogrpahics Concept Set on DATASET PAGE

      if (name === 'Demographics') {
        this.setState({
          errorMessage:
            'Name Demographics cannot be used for creating a concept set',
          saving: false,
        });
        return;
      }
      if (addingToExistingSet) {
        const updateConceptSetReq: UpdateConceptSetRequest = {
          etag: selectedSet.etag,
          addedConceptSetConceptIds: conceptIds,
        };
        try {
          const conceptSet = await conceptSetsApi().updateConceptSetConcepts(
            namespace,
            id,
            selectedSet.id,
            updateConceptSetReq
          );
          this.setState({ saving: false });
          onSave(conceptSet);
        } catch (error) {
          console.error(error);
        }
      } else {
        const conceptSet: ConceptSet = {
          name: name.trim(),
          description: newSetDescription,
          domain,
          criteriums: [],
        };
        const request: CreateConceptSetRequest = {
          conceptSet,
          addedConceptSetConceptIds: conceptIds,
        };
        try {
          const createdConceptSet = await conceptSetsApi().createConceptSet(
            namespace,
            id,
            request
          );
          this.setState({ saving: false });
          onSave(createdConceptSet);
        } catch (error) {
          console.error(error);
          this.setState({ errorSaving: true });
        }
      }
    }

    disableSave(errors) {
      const {
        addingToExistingSet,
        loadingSelectedSet,
        saving,
        selectedSetCount,
        selectedConceptsInDomain,
      } = this.state;
      if (addingToExistingSet) {
        return (
          loadingSelectedSet ||
          (selectedSetCount &&
            selectedSetCount + selectedConceptsInDomain.length >
              CONCEPT_SET_CONCEPT_LIMIT)
        );
      }
      return (!addingToExistingSet && !!errors) || saving;
    }

    async selectConceptSet(conceptSet: ConceptSet) {
      this.setState({ loadingSelectedSet: true });
      const {
        workspace: { namespace, id },
      } = this.props;
      const selectedSetCount = await conceptSetsApi().countConceptsInConceptSet(
        namespace,
        id,
        conceptSet.id
      );
      this.setState({
        loadingSelectedSet: false,
        selectedSet: conceptSet,
        selectedSetCount,
      });
    }

    render() {
      const { activeDomainTab, onClose } = this.props;
      const {
        conceptSets,
        loading,
        loadingSelectedSet,
        nameTouched,
        saving,
        addingToExistingSet,
        newSetDescription,
        name,
        errorMessage,
        errorSaving,
        selectedSet,
        selectedConceptsInDomain,
      } = this.state;
      const errors = validate(
        { name: name?.trim() },
        {
          name: nameValidationFormat(
            conceptSets.map((concept: ConceptSet) => concept.name),
            ResourceType.CONCEPTSET
          ),
        }
      );

      return (
        <Modal>
          <ModalTitle data-test-id='add-concept-title'>
            Add {selectedConceptsInDomain.length} Concepts to{' '}
            {activeDomainTab.name} Concept Set
          </ModalTitle>
          {loading ? (
            <div style={{ display: 'flex', justifyContent: 'center' }}>
              <Spinner style={{ alignContent: 'center' }} />
            </div>
          ) : (
            <ModalBody>
              <ModalBody>
                <FlexRow>
                  <TooltipTrigger
                    content={
                      <div>
                        No concept sets in domain '{activeDomainTab.name}'
                      </div>
                    }
                    disabled={conceptSets.length > 0}
                  >
                    <div>
                      <RadioButton
                        value={addingToExistingSet}
                        checked={addingToExistingSet}
                        disabled={conceptSets.length === 0}
                        data-test-id='toggle-existing-set'
                        onChange={() => {
                          this.setState({
                            addingToExistingSet: true,
                            nameTouched: false,
                          });
                        }}
                      />
                      <label style={styles.label}>Choose existing set</label>
                    </div>
                  </TooltipTrigger>
                  <div>
                    <RadioButton
                      value={!addingToExistingSet}
                      checked={!addingToExistingSet}
                      style={{ marginLeft: '1.05rem' }}
                      data-test-id='toggle-new-set'
                      onChange={() => {
                        this.setState({ addingToExistingSet: false });
                      }}
                    />
                    <label style={styles.label}>Create new set</label>
                  </div>
                </FlexRow>
              </ModalBody>
              {addingToExistingSet ? (
                <ModalBody data-test-id='add-to-existing'>
                  <select
                    data-test-id='existing-set-select'
                    style={{
                      marginTop: '1.5rem',
                      height: '2.25rem',
                      width: '100%',
                    }}
                    placeholder='Select Concept Set'
                    onChange={(e) =>
                      this.selectConceptSet(conceptSets[e.target.value])
                    }
                  >
                    {conceptSets.map((set: ConceptSet, i) => (
                      <option data-test-id='existing-set' key={i} value={i}>
                        {set.name}
                      </option>
                    ))}
                  </select>
                </ModalBody>
              ) : (
                <ModalBody data-test-id='create-new-set'>
                  <TextInput
                    placeholder='Name'
                    value={name}
                    data-test-id='create-new-set-name'
                    onChange={(v) => {
                      this.setState({ name: v, nameTouched: true });
                    }}
                  />
                  <ValidationError>
                    {summarizeErrors(nameTouched && errors?.name)}
                  </ValidationError>
                  <textarea
                    style={{ marginTop: '1.5rem' }}
                    placeholder='Add a Description'
                    value={newSetDescription}
                    onChange={(v) => {
                      this.setState({ newSetDescription: v.target.value });
                    }}
                  />
                </ModalBody>
              )}
              {errorSaving && (
                <AlertDanger>
                  Error saving concepts to set; please try again!
                </AlertDanger>
              )}
              {errorMessage && <AlertDanger>{errorMessage}</AlertDanger>}
              <ModalFooter>
                <Button type='secondary' onClick={onClose}>
                  Cancel
                </Button>
                <TooltipTrigger
                  content={
                    addingToExistingSet && (
                      <div>
                        Cannot add more concepts to
                        <b> {selectedSet?.name}. </b> Concept count exceeds{' '}
                        {CONCEPT_SET_CONCEPT_LIMIT}
                      </div>
                    )
                  }
                  disabled={!this.disableSave(errors)}
                >
                  <Button
                    style={{ marginLeft: '0.75rem' }}
                    disabled={this.disableSave(errors)}
                    data-test-id='save-concept-set'
                    onClick={() => this.saveConcepts()}
                  >
                    Save
                  </Button>
                </TooltipTrigger>
              </ModalFooter>
            </ModalBody>
          )}
          {(loadingSelectedSet || saving) && <SpinnerOverlay />}
        </Modal>
      );
    }
  }
);
