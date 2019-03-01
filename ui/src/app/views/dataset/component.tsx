import {Component} from '@angular/core';
import * as fp from 'lodash/fp';
import * as React from 'react';

import {Button} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {ClrIcon} from 'app/components/icons';
import {ResourceListItem} from 'app/components/resources';
import {SpinnerOverlay} from 'app/components/spinners';
import {WorkspaceData} from 'app/resolvers/workspace';
import {cohortsApi, conceptsApi, conceptSetsApi} from 'app/services/swagger-fetch-clients';
import {ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {navigate} from 'app/utils/navigation';
import {convertToResource, ResourceType} from 'app/utils/resourceActionsReact';
import {CreateConceptSetModal} from 'app/views/conceptset-create-modal/component';
import {ConfirmDeleteModal} from 'app/views/confirm-delete-modal/component';
import {EditModal} from 'app/views/edit-modal/component';
import {
  Cohort,
  ConceptSet,
  DomainInfo,
  RecentResource,
  WorkspaceAccessLevel,
} from 'generated/fetch';

export const styles = {
  selectBoxHeader: {
    fontSize: '16px',
    height: '2rem',
    lineHeight: '2rem',
    paddingLeft: '13px',
    color: '#2F2E7E',
    borderBottom: '1px solid #E5E5E5'
  },

  addIcon: {
    marginLeft: 19,
    fill: '#2691D0',
    verticalAlign: '-6%'
  }
};

export const DataSet = withCurrentWorkspace()(class extends React.Component<
  {workspace: WorkspaceData},
  {creatingConceptSet: boolean, conceptDomainList: DomainInfo[],
    conceptSetList: ConceptSet[], loadingConceptSets: boolean,
    confirmDeleting: boolean, editing: boolean, resource: RecentResource,
    rType: ResourceType, selectedConceptSets: ConceptSet[]
  }> {

  constructor(props) {
    super(props);
    this.state = {
      creatingConceptSet: false,
      conceptDomainList: undefined,
      conceptSetList: [],
      loadingConceptSets: true,
      confirmDeleting: false,
      editing: false,
      resource: undefined,
      rType: undefined,
      selectedConceptSets: []
    };
  }

  componentDidMount() {
    const {namespace, id} = this.props.workspace;
    this.loadResources();
    conceptsApi().getDomainInfo(namespace, id).then((response) => {
      this.setState({conceptDomainList: response.items});
    });
  }

  async loadResources() {
    try {
      const {namespace, id} = this.props.workspace;
      const conceptSets = await Promise.resolve(conceptSetsApi()
        .getConceptSetsInWorkspace(namespace, id));
      this.setState({conceptSetList: conceptSets.items, loadingConceptSets: false});
    } catch (error) {
      console.log(error);
    }
  }

  convertResource(r: ConceptSet | Cohort, rType: ResourceType): RecentResource {
    const {workspace} = this.props;
    this.setState({rType: rType});
    return convertToResource(r, workspace.namespace, workspace.id,
      workspace.accessLevel as unknown as WorkspaceAccessLevel, rType);
  }

  openConfirmDelete(r: ConceptSet, rType: ResourceType): void {
    const rc = this.convertResource(r, rType);
    this.setState({confirmDeleting: true, resource: rc});
  }

  closeConfirmDelete(): void {
    this.setState({confirmDeleting: false});
  }

  edit(r: Cohort | ConceptSet, rType: ResourceType): void {
    const rc = this.convertResource(r, rType);
    this.setState({editing: true, resource: rc});
  }

  receiveDelete() {
    const {resource, rType} = this.state;
    let call;
    const id = this.getCurrentResource().id;
    const {workspaceNamespace, workspaceFirecloudName} = resource;
    if (rType === ResourceType.CONCEPT_SET) {
      call = conceptSetsApi().deleteConceptSet(workspaceNamespace, workspaceFirecloudName, id);
    } else {
      call = cohortsApi().deleteCohort(workspaceNamespace, workspaceFirecloudName, id);
    }
    if (this.state.rType === ResourceType.CONCEPT_SET) {
      const deleted = this.state.resource.conceptSet;
      const updatedList = fp.filter(conceptSet => conceptSet.id !== deleted.id,
        this.state.conceptSetList);
      this.setState({conceptSetList: updatedList});
    }
    this.setState({resource: undefined, rType: undefined});
    call.then(() => this.closeConfirmDelete());
  }

  receiveEdit(resource: RecentResource): void {
    const updatedResource = this.getCurrentResource();
    const {workspaceNamespace, workspaceFirecloudName} = resource;
    const {id} = updatedResource;
    let call;
    if (resource.cohort) {
      call = cohortsApi().updateCohort(workspaceNamespace, workspaceFirecloudName,
        id, updatedResource as Cohort);
    } else if (resource.conceptSet) {
      call = conceptSetsApi().updateConceptSet(workspaceNamespace, workspaceFirecloudName,
        id, updatedResource);
    }
    const edited = this.state.resource.conceptSet;
    const updatedList = this.state.conceptSetList.map((conceptSet) => {
      return edited.id === conceptSet.id ? edited : conceptSet;
    });
    this.setState({conceptSetList: updatedList, resource: undefined, rType: undefined});
    call.then(() => this.closeEditModal());
  }

  closeEditModal(): void {
    this.setState({editing: false});
  }

  select(resource: ConceptSet | Cohort, rtype: ResourceType): void {
    if (rtype === ResourceType.CONCEPT_SET) {
      const origSelected = this.state.selectedConceptSets;
      const selected = fp.filter((c) => c.id === resource.id, origSelected);
      if (selected.length > 0) {
        this.setState({selectedConceptSets: fp.remove((c) => c.id === resource.id, origSelected)});
      } else {
        this.setState({selectedConceptSets: origSelected.concat(origSelected)});
      }
    } else {
      // else = cohort
      //  determine if already in selected
      //  remove if it is
      //  add if it isn't
    }
  }

  getCurrentResource(): Cohort | ConceptSet {
    if (this.state.resource) {
      return fp.compact([this.state.resource.cohort, this.state.resource.conceptSet])[0];
    }
  }

  render() {
    const {namespace, id} = this.props.workspace;
    const {
      creatingConceptSet,
      conceptDomainList,
      conceptSetList,
      loadingConceptSets,
      resource,
      rType
    } = this.state;
    const currentResource = this.getCurrentResource();
    return <React.Fragment>
      <FadeBox style={{marginTop: '1rem'}}>
        <h2 style={{marginTop: 0}}>Datasets</h2>
        <div style={{color: '#000000', fontSize: '14px'}}>Build a dataset by selecting the
          variables and values for one or more of your cohorts. Then export the completed dataset
          to Notebooks where you can perform your analysis</div>
        <div style={{display: 'flex'}}>
          <div style={{marginLeft: '1.5rem', marginRight: '1.5rem', width: '33%'}}>
            <h2>Select Cohorts</h2>
            <div style={{backgroundColor: 'white', border: '1px solid #E5E5E5'}}>
              <div style={styles.selectBoxHeader}>
                Cohorts
                <ClrIcon shape='plus-circle' class='is-solid' style={styles.addIcon}
                  onClick={() => navigate(['workspaces', namespace, id,  'cohorts', 'build'])}/>
              </div>
              {/*TODO: load cohorts and display here*/}
              <div style={{height: '8rem'}}/>
            </div>
          </div>
          <div style={{width: '58%'}}>
            <h2>Select Concept Sets</h2>
            <div style={{display: 'flex', backgroundColor: 'white', border: '1px solid #E5E5E5'}}>
              <div style={{flexGrow: 1, borderRight: '1px solid #E5E5E5'}}>
                <div style={{...styles.selectBoxHeader}}>
                  Concept Sets
                  <ClrIcon shape='plus-circle' class='is-solid' style={styles.addIcon}
                           onClick={() => this.setState({creatingConceptSet: true})}/>
                </div>
                {this.state.conceptSetList.length > 0 && !loadingConceptSets &&
                  this.state.conceptSetList.map(conceptSet =>
                    <ResourceListItem key={conceptSet.id} conceptSet={conceptSet}
                                      openConfirmDelete={
                                        () => {
                                          return this.openConfirmDelete(conceptSet,
                                            ResourceType.CONCEPT_SET);
                                        }
                                      }
                                      onSelect={
                                        () => this.select(conceptSet, ResourceType.CONCEPT_SET)
                                      }
                                      edit={
                                        () => this.edit(conceptSet, ResourceType.CONCEPT_SET)
                                      }/>)
                }
                {loadingConceptSets && <SpinnerOverlay/>}
              </div>
              <div style={{flexGrow: 1}}>
                <div style={styles.selectBoxHeader}>
                  Values
                </div>
                {/*TODO: load values and display here*/}
                <div style={{height: '8rem'}}/>
              </div>
            </div>
          </div>
        </div>
      </FadeBox>
      <FadeBox style={{marginTop: '1rem'}}>
        <div style={{backgroundColor: 'white', border: '1px solid #E5E5E5'}}>
          <div style={{...styles.selectBoxHeader, display: 'flex', position: 'relative'}}>
            <div>Preview Dataset</div>
            <div style={{marginLeft: '1rem', color: '#000000', fontSize: '14px'}}>A visualization
              of your data table based on the variable and value you selected above</div>
            {/* Button disabled until this functionality added*/}
            <Button style={{position: 'absolute', right: '1rem', top: '.25rem'}} disabled={true}>
              SAVE DATASET
            </Button>
          </div>
          {/*TODO: Display dataset preview*/}
          <div style={{height: '8rem'}}/>
        </div>
      </FadeBox>
      {creatingConceptSet &&
      <CreateConceptSetModal onCreate={() => {
        this.loadResources().then(() => {
          this.setState({creatingConceptSet: false});
        });
      }}
      onClose={() => {this.setState({ creatingConceptSet: false}); }}
      conceptDomainList={conceptDomainList}
      existingConceptSets={conceptSetList}/>}
      {this.state.confirmDeleting && currentResource &&
      <ConfirmDeleteModal resourceName={currentResource.name}
                          resourceType={rType}
                          receiveDelete={() => this.receiveDelete()}
                          closeFunction={() => this.closeConfirmDelete()}/>}
      {this.state.editing && resource &&
      <EditModal resource={resource}
                 onEdit={e => this.receiveEdit(e)}
                 onCancel={() => this.closeEditModal()}/>}
    </React.Fragment>;
  }

});

@Component({
  template: '<div #root></div>'
})
export class DataSetComponent extends ReactWrapperBase {
  constructor() {
    super(DataSet, []);
  }
}
