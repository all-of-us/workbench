import {Component} from '@angular/core';
import * as fp from 'lodash/fp';
import * as React from 'react';
import * as validate from 'validate.js';

import {Button, Clickable, MenuItem} from 'app/components/buttons';
import {SlidingFabReact} from 'app/components/buttons';
import {ConfirmDeleteModal} from 'app/components/confirm-delete-modal';
import {FadeBox} from 'app/components/containers';
import {CopyModal} from 'app/components/copy-modal';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {ClrIcon, SnowmanIcon} from 'app/components/icons';
import {TextArea, TextInput, ValidationError} from 'app/components/inputs';
import {Modal, ModalFooter, ModalTitle} from 'app/components/modals';
import {PopupTrigger, TooltipTrigger} from 'app/components/popups';
import {SpinnerOverlay} from 'app/components/spinners';
import {EditComponentReact} from 'app/icons/edit';
import {ConceptTable} from 'app/pages/data/concept/concept-table';
import {conceptSetsApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {
  reactStyles,
  ReactWrapperBase, summarizeErrors,
  withCurrentWorkspace,
  withUrlParams
} from 'app/utils';
import {currentConceptSetStore, navigate, navigateByUrl} from 'app/utils/navigation';
import {ResourceType} from 'app/utils/resourceActions';
import {WorkspaceData} from 'app/utils/workspace-data';
import {WorkspacePermissionsUtil} from 'app/utils/workspace-permissions';
import {Concept, ConceptSet, CopyRequest, WorkspaceAccessLevel} from 'generated/fetch';

const styles = reactStyles({
  conceptSetHeader: {
    display: 'flex', flexDirection: 'row', justifyContent: 'space-between', paddingBottom: '1.5rem'
  },
  conceptSetTitle: {
    color: colors.primary, fontSize: 20, fontWeight: 600, marginBottom: '0.5rem',
    display: 'flex', flexDirection: 'row'
  },
  conceptSetMetadataWrapper: {
    flexDirection: 'column', alignItems: 'space-between', marginLeft: '0.5rem'
  },
  conceptSetData: {
    display: 'flex', flexDirection: 'row', color: colors.primary, fontWeight: 600
  },
  buttonBoxes: {
    color: colors.accent, borderColor: colors.accent, marginBottom: '0.3rem'
  }
});

export interface ConceptSetMenuProps {
  canDelete: boolean;
  canEdit: boolean;
  onEdit: Function;
  onDelete: Function;
  onCopy: Function;
}

const ConceptSetMenu: React.FunctionComponent<ConceptSetMenuProps> = (
  {canDelete, canEdit, onEdit, onDelete, onCopy}
) => {

  return <PopupTrigger
    side='right'
    closeOnClick
    content={<React.Fragment>
      <TooltipTrigger>
        <MenuItem icon='copy'
                  onClick={onCopy}>
          Copy to another Workspace
        </MenuItem>
      </TooltipTrigger>
      <TooltipTrigger content={<div>Requires Write Permission</div>}
                      disabled={canEdit}>
        <MenuItem icon='pencil'
                  onClick={onEdit}
                  disabled={!canEdit}>
          Edit
        </MenuItem>
      </TooltipTrigger>
      <TooltipTrigger content={<div>Requires Owner Permission</div>}
                      disabled={canDelete}>
        <MenuItem icon='trash' onClick={onDelete} disabled={!canDelete}>
          Delete
        </MenuItem>
      </TooltipTrigger>
    </React.Fragment>}
  >
    <Clickable  data-test-id='workspace-menu'>
      <SnowmanIcon />
    </Clickable>
  </PopupTrigger>;
};

export interface ConceptSetDetailsProps {
  urlParams: any;
  workspace: WorkspaceData;
}

export interface ConceptSetDetailsState {
  copying: boolean;
  copySaving: boolean;
  conceptSet: ConceptSet;
  deleting: boolean;
  editing: boolean;
  editName: string;
  editDescription: string;
  editSaving: boolean;
  error: boolean;
  errorMessage: string;
  loading: boolean;
  removingConcepts: boolean;
  removeSubmitting: boolean;
  selectedConcepts: Concept[];
}

export const ConceptSetDetails = fp.flow(withUrlParams(), withCurrentWorkspace())(
  class extends React.Component<ConceptSetDetailsProps, ConceptSetDetailsState> {
    constructor(props) {
      super(props);
      this.state = {
        copying: false,
        copySaving: false,
        conceptSet: undefined,
        editing: false,
        editName: '',
        editDescription: '',
        editSaving: false,
        error: false,
        errorMessage: '',
        deleting: false,
        loading: true,
        removingConcepts: false,
        removeSubmitting: false,
        selectedConcepts: []
      };
    }

    componentDidMount() {
      this.getConceptSet();
    }

    async getConceptSet() {
      const {urlParams: {ns, wsid, csid}} = this.props;
      try {
        const resp = await conceptSetsApi().getConceptSet(ns, wsid, csid);
        this.setState({conceptSet: resp, editName: resp.name,
          editDescription: resp.description, loading: false});
        currentConceptSetStore.next(resp);
      } catch (error) {
        console.log(error);
        // TODO: what do we do with resources not found?  Currently we just have an endless spinner
        // Maybe want to think about designing an AoU not found page for better UX
      }
    }

    async copyConceptSet(copyRequest: CopyRequest) {
      const {urlParams: {ns, wsid, csid}} = this.props;
      this.setState({copySaving: true});
      return conceptSetsApi().copyConceptSet(
        ns,
        wsid,
        csid,
        copyRequest
      );
    }

    onCopy() {
      this.setState({copySaving: false});
    }

    async submitEdits() {
      const {urlParams: {ns, wsid, csid}} = this.props;
      const {conceptSet, editName, editDescription} = this.state;
      try {
        this.setState({editSaving: true});
        await conceptSetsApi().updateConceptSet(ns, wsid, csid,
          {...conceptSet, name: editName, description: editDescription});
        await this.getConceptSet();
      } catch (error) {
        console.log(error);
      } finally {
        this.setState({editing: false, editSaving: false});
      }
    }

    onSelectConcepts(concepts) {
      this.setState({selectedConcepts: concepts});
    }

    async onRemoveConcepts() {
      const {urlParams: {ns, wsid, csid}} = this.props;
      const {selectedConcepts, conceptSet} = this.state;
      this.setState({removeSubmitting: true});
      try {
        const updatedSet = await conceptSetsApi().updateConceptSetConcepts(ns, wsid, csid,
          {etag: conceptSet.etag, removedIds: selectedConcepts.map(c => c.conceptId)});
        this.setState({conceptSet: updatedSet, selectedConcepts: []});
      } catch (error) {
        console.log(error);
        this.setState({error: true, errorMessage: 'Could not delete concepts.'});
      } finally {
        this.setState({removeSubmitting: false, removingConcepts: false});
      }
    }

    async onDeleteConceptSet() {
      const {urlParams: {ns, wsid, csid}} = this.props;
      try {
        await conceptSetsApi().deleteConceptSet(ns, wsid, csid);
        navigate(['workspaces', ns, wsid, 'data', 'concepts', 'sets']);
      } catch (error) {
        console.log(error);
        this.setState({error: true,
          errorMessage: 'Could not delete concept set \'' + this.state.conceptSet.name + '\''});
      } finally {
        this.setState({deleting: false});
      }
    }

    get selectedConceptsCount(): number {
      return !!this.state.selectedConcepts ? this.state.selectedConcepts.length : 0;
    }

    get conceptSetConceptsCount(): number {
      return !!this.state.conceptSet && this.state.conceptSet.concepts ?
        this.state.conceptSet.concepts.length : 0;
    }

    addToConceptSet() {
      const {workspace} = this.props;
      const {conceptSet} = this.state;
      const queryParams = conceptSet.survey ? '?survey=' +  conceptSet.survey :
          '?domain=' + conceptSet.domain;
      navigateByUrl('workspaces/' + workspace.namespace + '/' +
          workspace.id + '/data/concepts' + queryParams);
    }

    render() {
      const {urlParams: {ns, wsid}, workspace} = this.props;
      const {
        copying, conceptSet, removingConcepts, editing, editDescription, editName,
        error, errorMessage, editSaving, deleting, removeSubmitting, loading,
        selectedConcepts
      } = this.state;
      const errors = validate({editName: editName}, {editName: {
        presence: {allowEmpty: false}
      }});

      return <React.Fragment>
        <FadeBox style={{margin: 'auto', paddingTop: '1rem', width: '95.7%'}}>
          {loading ? <SpinnerOverlay/> :
          <FlexColumn>
            <div style={styles.conceptSetHeader}>
              <FlexRow>
                <ConceptSetMenu canDelete={workspace.accessLevel === WorkspaceAccessLevel.OWNER}
                                canEdit={WorkspacePermissionsUtil.canWrite(workspace.accessLevel)}
                                onDelete={() => this.setState({deleting: true})}
                                onEdit={() => this.setState({editing: true})}
                                onCopy={() => this.setState({copying: true})}/>
                <div style={styles.conceptSetMetadataWrapper}>
                {editing ?
                  <FlexColumn>
                    <TextInput value={editName} disabled={editSaving}
                               id='edit-name'
                               style={{marginBottom: '0.5rem'}} data-test-id='edit-name'
                               onChange={v => this.setState({editName: v})}/>
                    {errors && <ValidationError>
                      {summarizeErrors( errors && errors.editName)}
                    </ValidationError>}
                    <TextArea value={editDescription} disabled={editSaving}
                              style={{marginBottom: '0.5rem'}} data-test-id='edit-description'
                              onChange={v => this.setState({editDescription: v})}/>
                    <div style={{marginBottom: '0.5rem'}}>
                      <Button type='primary' style={{marginRight: '0.5rem'}}
                              data-test-id='save-edit-concept-set'
                              disabled={editSaving || errors}
                              onClick={() => this.submitEdits()}>Save</Button>
                      <Button type='secondary' disabled={editSaving}
                              data-test-id='cancel-edit-concept-set'
                              onClick={() => this.setState({
                                editing: false,
                                editName: conceptSet.name,
                                editDescription: conceptSet.description
                              })}>Cancel</Button>
                    </div>
                  </FlexColumn> :
                  <React.Fragment>
                    <div style={styles.conceptSetTitle} data-test-id='concept-set-title'>
                      {conceptSet.name}
                      <Clickable disabled={!WorkspacePermissionsUtil
                                  .canWrite(workspace.accessLevel)}
                                 data-test-id='edit-concept-set'
                                 onClick={() => this.setState({editing: true})}>
                        <EditComponentReact enableHoverEffect={true}
                                            disabled={
                                              !WorkspacePermissionsUtil.canWrite(
                                                workspace.accessLevel
                                              )
                                            }
                                            style={{marginTop: '0.1rem'}}/>
                      </Clickable>
                    </div>
                    <div style={{marginBottom: '1.5rem', color: colors.primary}}
                         data-test-id='concept-set-description'>
                      {conceptSet.description}</div>
                  </React.Fragment>}
                  <div style={styles.conceptSetData}>
                      <div data-test-id='participant-count'>
                        Participant Count: {conceptSet.participantCount}</div>
                      <div style={{marginLeft: '2rem'}} data-test-id='concept-set-domain'>
                          Domain: {fp.capitalize(conceptSet.domain.toString())}</div>
                  </div>
                </div>
              </FlexRow>
              <FlexColumn>
                <Button type='secondaryLight' style={styles.buttonBoxes}
                        onClick={() => this.addToConceptSet()}>
                  <ClrIcon shape='search' style={{marginRight: '0.3rem'}}/>Add concepts to set
                </Button>
              </FlexColumn>
            </div>
            {!!conceptSet.concepts ?
            <ConceptTable concepts={conceptSet.concepts} loading={loading}
                          reactKey={conceptSet.domain.toString()}
                          onSelectConcepts={this.onSelectConcepts.bind(this)}
                          placeholderValue={'No Concepts Found'}
                          selectedConcepts={selectedConcepts} nextPage={(page) => {}}/> :
            <Button type='secondaryLight' data-test-id='add-concepts'
                    style={{...styles.buttonBoxes, marginLeft: '0.5rem', maxWidth: '22%'}}
                    onClick={() => navigateByUrl('workspaces/' + ns + '/' +
                      wsid + '/data/concepts' + conceptSet.survey ?
                        ('?survey=' + conceptSet.survey) : ('?domain=' + conceptSet.domain))}>
              <ClrIcon shape='search' style={{marginRight: '0.3rem'}}/>Add concepts to set
            </Button>}
            {WorkspacePermissionsUtil.canWrite(workspace.accessLevel) &&
                this.selectedConceptsCount > 0 &&
                <SlidingFabReact submitFunction={() => this.setState({removingConcepts: true})}
                               iconShape='trash' expanded='Remove from set'
                               tooltip={this.conceptSetConceptsCount === this.selectedConceptsCount}
                               tooltipContent={
                                 <div>Concept Sets must include at least one concept</div>}
                               disable={!WorkspacePermissionsUtil.canWrite(workspace.accessLevel) ||
                                 this.selectedConceptsCount === 0 ||
                               this.conceptSetConceptsCount === this.selectedConceptsCount}/>}
          </FlexColumn>}
          {!loading && deleting &&
          <ConfirmDeleteModal closeFunction={() => this.setState({deleting: false})}
                              receiveDelete={() => this.onDeleteConceptSet()}
                              resourceName={conceptSet.name}
                              resourceType={ResourceType.CONCEPT_SET}/>}
          {error && <Modal>
              <ModalTitle>Error: {errorMessage}</ModalTitle>
              <ModalFooter>
                  <Button type='secondary'
                          onClick={() =>
                            this.setState({error: false})}>Close</Button>
              </ModalFooter>
          </Modal>}
          {removingConcepts && <Modal>
            <ModalTitle>Are you sure you want to remove {this.selectedConceptsCount}
            {this.selectedConceptsCount > 1 ? ' concepts' : ' concept'} from this set?</ModalTitle>
            <ModalFooter>
                <Button type='secondary' style={{marginRight: '0.5rem'}} disabled={removeSubmitting}
                        onClick={() => this.setState({removingConcepts: false})}>
                    Cancel</Button>
                <Button type='primary' onClick={() => this.onRemoveConcepts()}
                        disabled={removeSubmitting} data-test-id='confirm-remove-concept'>
                    Remove concepts</Button>
            </ModalFooter>
          </Modal>}
          {copying && <CopyModal
            fromWorkspaceNamespace={workspace.namespace}
            fromWorkspaceName={workspace.id}
            fromResourceName={conceptSet.name}
            resourceType={ResourceType.CONCEPT_SET}
            onClose={() => this.setState({copying: false})}
            onCopy={() => this.onCopy()}
            saveFunction={(copyRequest: CopyRequest) => this.copyConceptSet(copyRequest)}/>
          }
        </FadeBox>
      </React.Fragment>;
    }
  });


@Component({
  template: '<div #root style="position: relative; margin-right: 45px;"></div>'
})
export class ConceptSetDetailsComponent extends ReactWrapperBase {
  constructor() {
    super(ConceptSetDetails, []);
  }
}
