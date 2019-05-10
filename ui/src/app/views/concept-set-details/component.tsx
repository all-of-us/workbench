import {Component} from '@angular/core';
import * as fp from 'lodash/fp';
import * as React from 'react';

import {Button, Clickable, MenuItem} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {ClrIcon} from 'app/components/icons';
import {TextArea, TextInput} from 'app/components/inputs';
import {Modal, ModalFooter, ModalTitle} from 'app/components/modals';
import {PopupTrigger, TooltipTrigger} from 'app/components/popups';
import {SpinnerOverlay} from 'app/components/spinners';
import {EditComponentReact} from 'app/icons/edit/component';
import {conceptSetsApi} from 'app/services/swagger-fetch-clients';
import {WorkspaceData} from 'app/services/workspace-storage.service';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace, withRouteConfigData, withUrlParams} from 'app/utils';
import {navigate, navigateByUrl} from 'app/utils/navigation';
import {ResourceType} from 'app/utils/resourceActionsReact';
import {ConceptTable} from 'app/views/concept-table/component';
import {ConfirmDeleteModal} from 'app/views/confirm-delete-modal/component';
import {Concept, ConceptSet, WorkspaceAccessLevel} from 'generated/fetch';

const styles = reactStyles({
  conceptSetHeader: {
    display: 'flex', flexDirection: 'row', justifyContent: 'space-between', paddingBottom: '1.5rem'
  },
  conceptSetTitle: {
    color: '#2F2E7E', fontSize: 20, fontWeight: 600, marginBottom: '0.5rem',
    display: 'flex', flexDirection: 'row'
  },
  conceptSetMetadataWrapper: {
    flexDirection: 'column', alignItems: 'space-between', marginLeft: '0.5rem'
  },
  conceptSetData: {
    display: 'flex', flexDirection: 'row', color: '#000', fontWeight: 600
  },
  buttonBoxes: {
    color: '#2691D0', borderColor: '#2691D0', marginBottom: '0.3rem'
  }
});

const ConceptSetMenu: React.FunctionComponent<{
  disabled: boolean, canDelete: boolean, canEdit: boolean, onEdit: Function, onDelete: Function
}> = ({disabled, canDelete, canEdit, onEdit, onDelete}) => {

  return <PopupTrigger
    side='right'
    closeOnClick
    content={ <React.Fragment>
      <TooltipTrigger content={<div>Requires Write Permission</div>}
                      disabled={canEdit}>
        <MenuItem icon='pencil'
                  onClick={() => onEdit}
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
    <Clickable disabled={disabled} data-test-id='workspace-menu'>
      <ClrIcon shape='ellipsis-vertical' size={21}
               style={{color: disabled ? '#9B9B9B' : '#216FB4', marginLeft: -9,
                 cursor: disabled ? 'auto' : 'pointer'}}/>
    </Clickable>
  </PopupTrigger>;
};

export const ConceptSetDetails = fp.flow(withUrlParams(), withCurrentWorkspace())(
  class extends React.Component<{
    urlParams: any, workspace: WorkspaceData}, {
      conceptSet: ConceptSet, conceptSetDeletionError: boolean, deleting: boolean, editName: string,
      editDescription: string, editSaving: boolean, editing: boolean, loading: boolean,
      selectedConcepts: Concept[]}> {
    constructor(props) {
      super(props);
      this.state = {
        conceptSet: undefined,
        conceptSetDeletionError: false,
        editName: '',
        editDescription: '',
        editSaving: false,
        deleting: false,
        editing: false,
        loading: true,
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
      } catch (error) {
        console.log(error);
        // todo: cannot find concept set
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
        this.setState({editing: false, editSaving: false});
      } catch (error) {
        console.log(error);
        this.setState({editing: false, editSaving: false});
      }
    }

    onSelectConcepts() {
      // TODO
    }

    onDeleteConcept() {
      // TODO
    }

    async onDeleteConceptSet() {
      const {urlParams: {ns, wsid, csid}} = this.props;
      try {
        await conceptSetsApi().deleteConceptSet(ns, wsid, csid);
        this.setState({deleting: false});
        navigate(['workspaces', ns, wsid, 'concepts', 'sets']);
      } catch (error) {
        console.log(error);
        this.setState({deleting: false, conceptSetDeletionError: true});
      }
    }

    get canEdit(): boolean {
      return this.isOwner || this.isWriter;
    }

    get isWriter(): boolean {
      return this.props.workspace.accessLevel as unknown as WorkspaceAccessLevel
        === WorkspaceAccessLevel.WRITER;
    }

    get isOwner(): boolean {
      return this.props.workspace.accessLevel as unknown as WorkspaceAccessLevel
        === WorkspaceAccessLevel.OWNER;
    }

    render() {
      const {urlParams: {ns, wsid}} = this.props;
      const {conceptSet, conceptSetDeletionError, editing, editDescription, editName,
        editSaving, deleting, loading, selectedConcepts} = this.state;
      return <FadeBox style={{margin: 'auto', marginTop: '1rem', width: '95.7%'}}>
        {loading ? <SpinnerOverlay/> :
        <div style={{display: 'flex', flexDirection: 'column'}}>
          <div style={styles.conceptSetHeader}>
            <div style={{display: 'flex', flexDirection: 'row'}}>
              <ConceptSetMenu canDelete={this.isOwner} canEdit={this.canEdit}
                              disabled={!this.canEdit}
                              onDelete={() => this.setState({deleting: true})}
                              onEdit={() => this.setState({editing: true})}/>
              <div style={styles.conceptSetMetadataWrapper}>
              {editing ?
                <div style={{display: 'flex', flexDirection: 'column'}}>
                  <TextInput value={editName} disabled={editSaving}
                             style={{marginBottom: '0.5rem'}}
                             onChange={v => this.setState({editName: v})}/>
                  <TextArea value={editDescription} disabled={editSaving}
                            style={{marginBottom: '0.5rem'}}
                            onChange={v => this.setState({editDescription: v})}/>
                  <div style={{marginBottom: '0.5rem'}}>
                    <Button type='primary' style={{marginRight: '0.5rem'}}
                            disabled={editSaving} onClick={() => this.submitEdits()}>Save</Button>
                    <Button type='secondary' disabled={editSaving}
                            onClick={() => this.setState({
                              editing: false,
                              editName: conceptSet.name,
                              editDescription: conceptSet.description
                            })}>Cancel</Button>
                  </div>
                </div> :
                <React.Fragment>
                  <div style={styles.conceptSetTitle}>{conceptSet.name}
                    <Clickable disabled={!this.canEdit}
                               onClick={() => this.setState({editing: true})}>
                      <EditComponentReact disabled={!this.canEdit} style={{marginTop: '0.1rem'}}/>
                    </Clickable>
                  </div>
                  <div style={{marginBottom: '1.5rem', color: '#000'}}>{conceptSet.description}</div>
                </React.Fragment>}
                <div style={styles.conceptSetData}>
                    <div>Participant Count: {conceptSet.participantCount}</div>
                    <div style={{marginLeft: '2rem'}}>
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
          <ConceptTable concepts={conceptSet.concepts} loading={loading}
                        reactKey={conceptSet.domain.toString()}
                        onSelectConcepts={() => this.onSelectConcepts()}
                        placeholderValue={'No Concepts Found'}
                        selectedConcepts={selectedConcepts}/>
        </div>}
        {!loading && deleting &&
        <ConfirmDeleteModal closeFunction={() => this.setState({deleting: false})}
                            receiveDelete={() => this.onDeleteConceptSet()}
                            resourceName={conceptSet.name}
                            resourceType={ResourceType.CONCEPT_SET}/>}
        {conceptSetDeletionError && <Modal>
            <ModalTitle>Error: Could not concept set '{conceptSet.name}'</ModalTitle>
            <ModalFooter>
                <Button type='secondary'
                        onClick={() =>
                          this.setState({conceptSetDeletionError: false})}>Close</Button>
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
