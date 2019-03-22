import {Component} from '@angular/core';
import * as fp from 'lodash/fp';
import * as React from 'react';

import {Button} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {ClrIcon} from 'app/components/icons';
import {ResourceListItem} from 'app/components/resources';
import {Spinner} from 'app/components/spinners';
import {cohortsApi, conceptsApi, conceptSetsApi} from 'app/services/swagger-fetch-clients';
import {WorkspaceData} from 'app/services/workspace-storage.service';
import {ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {navigate, navigateByUrl} from 'app/utils/navigation';
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
    conceptSetList: ConceptSet[], cohortList: Cohort[], loadingResources: boolean,
    confirmDeleting: boolean, editing: boolean, resource: RecentResource,
    rType: ResourceType, selectedConceptSetIds: number[], selectedCohortIds: number[]
  }> {

  constructor(props) {
    super(props);
    this.state = {
      creatingConceptSet: false,
      conceptDomainList: undefined,
      conceptSetList: [],
      cohortList: [],
      loadingResources: true,
      confirmDeleting: false,
      editing: false,
      resource: undefined,
      rType: undefined,
      selectedConceptSetIds: [],
      selectedCohortIds: []
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
      const [conceptSets, cohorts] = await Promise.all([
        conceptSetsApi().getConceptSetsInWorkspace(namespace, id),
        cohortsApi().getCohortsInWorkspace(namespace, id)]);
      this.setState({conceptSetList: conceptSets.items, cohortList: cohorts.items,
        loadingResources: false});
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
    const {rType} = this.state;
    const {namespace, id} = this.props.workspace;
    let call;
    const resourceId = this.getCurrentResource().id;
    if (rType === ResourceType.CONCEPT_SET) {
      call = conceptSetsApi().deleteConceptSet(namespace, id, resourceId);
    } else {
      call = cohortsApi().deleteCohort(namespace, id, resourceId);
    }
    if (this.state.rType === ResourceType.CONCEPT_SET) {
      this.setState({conceptSetList: fp.without([this.getCurrentResource() as ConceptSet],
        this.state.conceptSetList), selectedConceptSetIds: fp.without([resourceId],
          this.state.selectedConceptSetIds)});
    } else {
      this.setState({cohortList: fp.without([this.getCurrentResource() as Cohort],
        this.state.cohortList), selectedCohortIds: fp.without([resourceId],
          this.state.selectedCohortIds)});
    }
    this.setState({resource: undefined, rType: undefined});
    call.then(() => this.closeConfirmDelete());
  }

  receiveEdit(resource: RecentResource): void {
    const updatedResource = this.getCurrentResource();
    const {workspaceNamespace, workspaceFirecloudName} = resource;
    const {id} = updatedResource;
    let call;
    let updatedConceptSetList = this.state.conceptSetList;
    let updatedCohortList = this.state.cohortList;
    if (resource.cohort) {
      call = cohortsApi().updateCohort(workspaceNamespace, workspaceFirecloudName,
        id, updatedResource as Cohort);
      updatedCohortList = this.state.cohortList.map((cohort) => {
        return updatedResource.id === cohort.id ? (updatedResource as Cohort) : cohort;
      });
    } else if (resource.conceptSet) {
      call = conceptSetsApi().updateConceptSet(workspaceNamespace, workspaceFirecloudName,
        id, updatedResource);
      updatedConceptSetList = this.state.conceptSetList.map((conceptSet) => {
        return updatedResource.id === conceptSet.id ? (updatedResource as ConceptSet) : conceptSet;
      });
    }
    this.setState({conceptSetList: updatedConceptSetList, cohortList: updatedCohortList,
      resource: undefined, rType: undefined});
    call.then(() => this.closeEditModal());
  }

  closeEditModal(): void {
    this.setState({editing: false});
  }

  clone(cohort: Cohort): void {
    const {namespace, id} = this.props.workspace;
    navigateByUrl(`/workspaces/${namespace}/${id}/cohorts/build?cohortId=${cohort.id}`);
  }

  review(cohort: Cohort): void {
    const {namespace, id} = this.props.workspace;
    navigateByUrl(`/workspaces/${namespace}/${id}/cohorts/${cohort.id}/review`);
  }

  select(resource: ConceptSet | Cohort, rtype: ResourceType): void {
    if (rtype === ResourceType.CONCEPT_SET) {
      const origSelected = this.state.selectedConceptSetIds;
      if (fp.includes(resource.id, origSelected)) {
        this.setState({selectedConceptSetIds: fp.remove((c) => c === resource.id, origSelected)});
      } else {
        this.setState({selectedConceptSetIds: (origSelected).concat(resource.id)});
      }
    } else {
      const origSelected = this.state.selectedCohortIds;
      if (fp.includes(resource.id, origSelected)) {
        this.setState({selectedCohortIds: fp.remove((c) => c === resource.id, origSelected)});
      } else {
        this.setState({selectedCohortIds: (origSelected).concat(resource.id)});
      }
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
      loadingResources,
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
              <div style={{height: '10rem', overflowY: 'auto'}}>
                {!loadingResources && this.state.cohortList.map(cohort =>
                  <ResourceListItem key={cohort.id} resource={cohort} rType={ResourceType.COHORT}
                                    data-test-id='cohort-list-item'
                                    openConfirmDelete={
                                      () => {
                                        return this.openConfirmDelete(cohort, ResourceType.COHORT);
                                      }
                                    }
                                    onSelect={
                                      () => this.select(cohort, ResourceType.COHORT)
                                    }
                                    edit={
                                      () => this.edit(cohort, ResourceType.COHORT)
                                    }
                                    onClone={
                                      () => this.clone(cohort)
                                    }
                                    onReview={
                                      () => this.review(cohort)
                                    }/>
                  )
                }
                {loadingResources && <Spinner style={{position: 'relative', top: '2rem',
                  left: '10rem'}}/>}
              </div>
            </div>
          </div>
          <div style={{width: '58%'}}>
            <h2>Select Concept Sets</h2>
            <div style={{display: 'flex', backgroundColor: 'white', border: '1px solid #E5E5E5'}}>
              <div style={{flexGrow: 1, borderRight: '1px solid #E5E5E5'}}>
                <div style={styles.selectBoxHeader}>
                  Concept Sets
                  <ClrIcon shape='plus-circle' class='is-solid' style={styles.addIcon}
                           onClick={() => navigate(['workspaces', namespace, id,  'concepts'])}/>
                </div>
                <div style={{height: '10rem', overflowY: 'auto'}}>
                  {!loadingResources && this.state.conceptSetList.map(conceptSet =>
                      <ResourceListItem key={conceptSet.id} resource={conceptSet}
                                        data-test-id='concept-set-list-item'
                                        rType={ResourceType.CONCEPT_SET}
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
                  {loadingResources && <Spinner style={{position: 'relative', top: '2rem',
                    left: '10rem'}}/>}
                </div>
              </div>
              <div style={{flexGrow: 1}}>
                <div style={styles.selectBoxHeader}>
                  Values
                </div>
                {/*TODO: load values and display here*/}
                <div style={{height: '10rem', overflowY: 'auto'}}/>
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
