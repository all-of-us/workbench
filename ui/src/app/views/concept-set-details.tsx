import {Component} from '@angular/core';
import * as fp from 'lodash/fp';
import * as React from 'react';
import * as validate from 'validate.js';

import {Button, Clickable, MenuItem} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {ClrIcon} from 'app/components/icons';
import {TextArea, TextInput, ValidationError} from 'app/components/inputs';
import {Modal, ModalFooter, ModalTitle} from 'app/components/modals';
import {PopupTrigger, TooltipTrigger} from 'app/components/popups';
import {SpinnerOverlay} from 'app/components/spinners';
import {EditComponentReact} from 'app/icons/edit';
import {conceptSetsApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {
  reactStyles,
  ReactWrapperBase, summarizeErrors,
  withCurrentWorkspace,
  withUrlParams
} from 'app/utils';
import {currentConceptSetStore, navigate, navigateByUrl} from 'app/utils/navigation';
import {ResourceType} from 'app/utils/resourceActionsReact';
import {WorkspaceData} from 'app/utils/workspace-data';
import {WorkspacePermissions} from 'app/utils/workspace-permissions';
import {ConceptTable} from 'app/views/concept-table';
import {ConfirmDeleteModal} from 'app/views/confirm-delete-modal';
import {SlidingFabReact} from 'app/views/sliding-fab';
import {Concept, ConceptSet} from 'generated/fetch';

const styles = reactStyles({
  conceptSetHeader: {
    display: 'flex', flexDirection: 'row', justifyContent: 'space-between', paddingBottom: '1.5rem'
  },
  conceptSetTitle: {
    color: colors.blue[7], fontSize: 20, fontWeight: 600, marginBottom: '0.5rem',
    display: 'flex', flexDirection: 'row'
  },
  conceptSetMetadataWrapper: {
    flexDirection: 'column', alignItems: 'space-between', marginLeft: '0.5rem'
  },
  conceptSetData: {
    display: 'flex', flexDirection: 'row', color: colors.black[0], fontWeight: 600
  },
  buttonBoxes: {
    color: colors.blue[0], borderColor: colors.blue[0], marginBottom: '0.3rem'
  }
});

const ConceptSetMenu: React.FunctionComponent<{
  canDelete: boolean, canEdit: boolean, onEdit: Function, onDelete: Function
}> = ({canDelete, canEdit, onEdit, onDelete}) => {

  return <PopupTrigger
    side='right'
    closeOnClick
    content={ <React.Fragment>
      <TooltipTrigger content={<div>Requires Write Permission</div>}
                      disabled={canEdit}>
        <MenuItem onClick={() => onEdit}
                  disabled={!canEdit}>
          Edit
        </MenuItem>
      </TooltipTrigger>
      <TooltipTrigger content={<div>Requires Owner Permission</div>}
                      disabled={canDelete}>
        <MenuItem onClick={onDelete} disabled={!canDelete}>
          Delete
        </MenuItem>
      </TooltipTrigger>
    </React.Fragment>}
  >
    <Clickable  data-test-id='workspace-menu'>
      <ClrIcon shape='ellipsis-vertical' size={21}
               style={{color: '#216FB4', marginLeft: -9,
                 cursor: 'pointer'}}/>
    </Clickable>
  </PopupTrigger>;
};

export const ConceptSetDetails =
  fp.flow(withUrlParams(), withCurrentWorkspace())(class extends React.Component<{
    urlParams: any, workspace: WorkspaceData}, {
      conceptSet: ConceptSet, deleting: boolean, editName: string, editDescription: string,
      editSaving: boolean, error: boolean, errorMessage: string, editing: boolean,
      loading: boolean, removingConcepts: boolean, removeSubmitting: boolean,
      selectedConcepts: Concept[], workspacePermissions: WorkspacePermissions}> {
    constructor(props) {
      super(props);
      this.state = {
        conceptSet: undefined,
        editName: '',
        editDescription: '',
        editSaving: false,
        error: false,
        errorMessage: '',
        deleting: false,
        editing: false,
        loading: true,
        removingConcepts: false,
        removeSubmitting: false,
        selectedConcepts: [],
        workspacePermissions: new WorkspacePermissions(props.workspace)
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
        navigate(['workspaces', ns, wsid, 'concepts', 'sets']);
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

    render() {
      const {urlParams: {ns, wsid}} = this.props;
      const {conceptSet, removingConcepts, editing, editDescription, editName,
        error, errorMessage, editSaving, deleting, removeSubmitting, loading,
        selectedConcepts, workspacePermissions} = this.state;
      const errors = validate({editName: editName}, {editName: {
        presence: {allowEmpty: false}
      }});

      return <FadeBox style={{margin: 'auto', marginTop: '1rem', width: '95.7%'}}>
        {loading ? <SpinnerOverlay/> :
        <div style={{display: 'flex', flexDirection: 'column'}}>
          <div style={styles.conceptSetHeader}>
            <div style={{display: 'flex', flexDirection: 'row'}}>
              <ConceptSetMenu canDelete={workspacePermissions.isOwner}
                              canEdit={workspacePermissions.canWrite}
                              onDelete={() => this.setState({deleting: true})}
                              onEdit={() => this.setState({editing: true})}/>
              <div style={styles.conceptSetMetadataWrapper}>
              {editing ?
                <div style={{display: 'flex', flexDirection: 'column'}}>
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
                </div> :
                <React.Fragment>
                  <div style={styles.conceptSetTitle} data-test-id='concept-set-title'>
                    {conceptSet.name}
                    <Clickable disabled={!workspacePermissions.canWrite}
                               data-test-id='edit-concept-set'
                               onClick={() => this.setState({editing: true})}>
                      <EditComponentReact disabled={!workspacePermissions.canWrite}
                                          style={{marginTop: '0.1rem'}}/>
                    </Clickable>
                  </div>
                  <div style={{marginBottom: '1.5rem', color: colors.black[0]}}
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
            </div>
            <div style={{display: 'flex', flexDirection: 'column'}}>
              <Button type='secondaryLight' style={styles.buttonBoxes}
                      onClick={() => navigateByUrl('workspaces/' + ns + '/' +
                        wsid + '/concepts' + '?domain=' + conceptSet.domain)}>
                <ClrIcon shape='search' style={{marginRight: '0.3rem'}}/>Add concepts to set
              </Button>
              <Button type='secondaryLight' style={styles.buttonBoxes}
                      onClick={() => navigate(['workspaces', ns, wsid, 'concepts', 'sets'])}>
                <ClrIcon shape='grid-view' style={{marginRight: '0.3rem'}}/>Return to concept sets
              </Button>
            </div>
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
                    wsid + '/concepts' + '?domain=' + conceptSet.domain)}>
            <ClrIcon shape='search' style={{marginRight: '0.3rem'}}/>Add concepts to set
          </Button>}
          {workspacePermissions.canWrite && this.selectedConceptsCount > 0 &&
              <SlidingFabReact submitFunction={() => this.setState({removingConcepts: true})}
                               iconShape='trash' expanded='Remove from set'
                               tooltip={this.conceptSetConceptsCount === this.selectedConceptsCount}
                               tooltipContent={
                                 <div>Concept Sets must include at least one concept</div>}
                               disable={!workspacePermissions.canWrite ||
                                 this.selectedConceptsCount === 0 ||
                               this.conceptSetConceptsCount === this.selectedConceptsCount}/>}
        </div>}
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
      </FadeBox>;
    }
  });


@Component({
  template: '<div #root></div>'
})
export class ConceptSetDetailsComponent extends ReactWrapperBase {
  constructor() {
    super(ConceptSetDetails, []);
  }
}
