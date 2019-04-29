import {Component} from '@angular/core';
import * as fp from 'lodash/fp';
import * as React from 'react';

import {AlertDanger} from 'app/components/alert';
import {TextInput, ValidationError} from 'app/components/inputs';
import {Modal, ModalBody, ModalFooter, ModalTitle} from 'app/components/modals';

import {Button} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {ClrIcon} from 'app/components/icons';
import {ImmutableListItem, ResourceListItem} from 'app/components/resources';
import {Spinner} from 'app/components/spinners';
import {
  cohortsApi,
  conceptsApi,
  conceptSetsApi,
  dataSetApi
} from 'app/services/swagger-fetch-clients';
import {WorkspaceData} from 'app/services/workspace-storage.service';
import {ReactWrapperBase, toggleIncludes, withCurrentWorkspace} from 'app/utils';
import {summarizeErrors} from 'app/utils';
import {navigate, navigateByUrl} from 'app/utils/navigation';
import {convertToResource, ResourceType} from 'app/utils/resourceActionsReact';
import {CreateConceptSetModal} from 'app/views/conceptset-create-modal/component';
import {ConfirmDeleteModal} from 'app/views/confirm-delete-modal/component';
import {EditModal} from 'app/views/edit-modal/component';
import {
  Cohort,
  ConceptSet,
  DataSetQuery,
  DataSetRequest,
  Domain,
  DomainInfo,
  DomainValue,
  DomainValuesResponse,
  NamedParameterEntry,
  RecentResource,
  ValueSet,
  WorkspaceAccessLevel,
} from 'generated/fetch';
import colors from 'app/styles/colors';

import {validate} from 'validate.js';

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
  },

  valueListItemCheckboxStyling: {
    height: 17,
    width: 17,
    marginLeft: 10,
    marginTop: 10,
    marginRight: 10,
    backgroundColor: '#7CC79B'
  },

  subheader: {
    fontWeight: 400,
    fontSize: '0.6rem',
    marginTop: '0.5rem',
    color: colors.purple[0]
  }
};

interface DomainValuePair {
  domain: Domain;
  value: string;
}

const Subheader = (props) => {
  return <div style={{...styles.subheader, ...props.style}}>{props.children}</div>;
};

export const ValueListItem: React.FunctionComponent <
  {domainValue: DomainValue, onSelect: Function, checked: boolean}> =
  ({domainValue, onSelect, checked}) => {
    return <div style={{display: 'flex', color: 'black', height: '1.2rem'}}>
      <input type='checkbox' value={domainValue.value} onChange={() => onSelect()}
             style={styles.valueListItemCheckboxStyling}
             checked={checked}/>
      <div style={{lineHeight: '1.5rem'}}>{domainValue.value}</div>
    </div>;
  };

export const DataSetPage = withCurrentWorkspace()(class extends React.Component<
  {workspace: WorkspaceData},
  {name: string, creatingConceptSet: boolean, conceptDomainList: DomainInfo[],
    conceptSetList: ConceptSet[], cohortList: Cohort[], loadingResources: boolean,
    confirmDeleting: boolean, editing: boolean, resource: RecentResource,
    rType: ResourceType, selectedConceptSetIds: number[], selectedCohortIds: number[],
    valueSets: ValueSet[], selectedValues: DomainValuePair[], openSaveModal: boolean,
    nameRequired: boolean, conflictDataSetName: boolean, missingDataSetInfo: boolean,
    nameTouched: boolean, queries: Array<DataSetQuery>, includesAllParticipants: boolean
  }> {

  constructor(props) {
    super(props);
    this.state = {
      name: '',
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
      selectedCohortIds: [],
      valueSets: [],
      selectedValues: [],
      openSaveModal: false,
      nameRequired: false,
      conflictDataSetName: false,
      missingDataSetInfo: false,
      nameTouched: false,
      queries: [],
      includesAllParticipants: false
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

  getDomainsFromConceptIds(selectedConceptSetIds: number[]): Domain[] {
    const {conceptSetList} = this.state;
    return fp.uniq(conceptSetList.filter((conceptSet: ConceptSet) =>
      selectedConceptSetIds.includes(conceptSet.id))
      .map((conceptSet: ConceptSet) => conceptSet.domain));
  }

  async getValuesList(domains: Domain[]): Promise<ValueSet[]> {
    const {namespace, id} = this.props.workspace;
    const valueSets = fp.zipWith((domain: Domain, valueSet: DomainValuesResponse) =>
        ({domain: domain, values: valueSet}),
      domains,
      await Promise.all(domains.map((domain) =>
        conceptsApi().getValuesFromDomain(namespace, id, domain.toString()))));
    return valueSets;
  }

  select(resource: ConceptSet | Cohort, rtype: ResourceType): void {
    if (rtype === ResourceType.CONCEPT_SET) {
      const {valueSets, selectedValues} = this.state;
      const origSelected = this.state.selectedConceptSetIds;
      const newSelectedConceptSets =
        toggleIncludes(resource.id, origSelected)as unknown as number[];
      const currentDomains = this.getDomainsFromConceptIds(newSelectedConceptSets);
      const origDomains = valueSets.map(valueSet => valueSet.domain);
      const newDomains = fp.without(origDomains, currentDomains) as unknown as Domain[];
      const removedDomains = fp.without(currentDomains, origDomains);
      const updatedValueSets =
        valueSets.filter(valueSet => !(fp.contains(valueSet.domain, removedDomains)));
      const updatedSelectedValues =
        selectedValues.filter(selectedValue =>
          !fp.contains(selectedValue.domain, removedDomains));
      if (newDomains.length > 0) {
        this.getValuesList(newDomains)
          .then(newValueSets => this.setState({
            selectedConceptSetIds: newSelectedConceptSets,
            valueSets: updatedValueSets.concat(newValueSets),
            selectedValues: updatedSelectedValues
          }));
      } else {
        this.setState({selectedConceptSetIds: newSelectedConceptSets,
          valueSets: updatedValueSets,
          selectedValues: updatedSelectedValues});
      }
    } else {
      this.setState({selectedCohortIds: toggleIncludes(resource.id,
        this.state.selectedCohortIds) as unknown as number[]});
    }
  }

  selectDomainValue(domain: Domain, domainValue: DomainValue): void {
    const origSelected = this.state.selectedValues;
    const selectObj = {domain: domain, value: domainValue.value};
    if (fp.some(selectObj, origSelected)) {
      this.setState({selectedValues:
        fp.remove((dv) => dv.domain === selectObj.domain
        && dv.value === selectObj.value, origSelected)});
    } else {
      this.setState({selectedValues: (origSelected).concat(selectObj)});
    }
  }

  getCurrentResource(): Cohort | ConceptSet {
    if (this.state.resource) {
      return fp.compact([this.state.resource.cohort, this.state.resource.conceptSet])[0];
    }
  }

  async saveDataSet() {
    this.setState({nameTouched: true});
    if (!this.state.name) {
      return;
    }
    this.setState({conflictDataSetName: false, missingDataSetInfo: false });
    const request = {
      name: this.state.name,
      includesAllParticipants: this.state.includesAllParticipants,
      description: '',
      conceptSetIds: this.state.selectedConceptSetIds,
      cohortIds: this.state.selectedCohortIds,
      values: this.state.selectedValues
    };
    try {
      await dataSetApi().createDataSet(
        this.props.workspace.namespace, this.props.workspace.id, request);
      this.setState({openSaveModal: false});
    } catch (e) {
      if (e.status === 409) {
        this.setState({conflictDataSetName: true});
      } else if (e.status === 400) {
        this.setState({missingDataSetInfo: true});
      }
    }
  }

  disableSave() {
    return !this.state.selectedConceptSetIds || this.state.selectedConceptSetIds.length === 0 ||
        ((!this.state.selectedCohortIds ||
        this.state.selectedCohortIds.length === 0) && !this.state.includesAllParticipants) ||
        !this.state.selectedValues || this.state.selectedValues.length === 0;
  }

  async generateCode() {
    const {namespace, id} = this.props.workspace;
    const dataSet: DataSetRequest = {
      name: '',
      conceptSetIds: this.state.selectedConceptSetIds,
      cohortIds: this.state.selectedCohortIds,
      values: this.state.selectedValues,
      includesAllParticipants: this.state.includesAllParticipants
    };
    const sqlQueries = await dataSetApi().generateQuery(namespace, id, dataSet);
    this.setState({queries: sqlQueries.queryList});
  }

  buildQueryConfig(np: NamedParameterEntry) {
    if (np.value) {
      return <div>
        &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;{'{'}
        {'\'name\': "' + np.value.name + '"'},
        {'\'parameterType\': {\'type\': "' + np.value.parameterType + '"' +
          (np.value.parameterValue instanceof Object ?
            ',\'arrayType\': {\'type\': "' + np.value.arrayType + '"},' :
            '') + '}'},
        {'\'parameterValue\': {'}{((np.value.parameterValue instanceof Object) ?
          '\'arrayValues\': [' + np.value.parameterValue.map(
            npv => '{\'value\': ' + npv.parameterValue + '}') + ']' :
          '\'value\': "' + np.value.parameterValue + '"')}{'}},'}
      </div>;
    } else {
      return;
    }
  }

  render() {
    const {namespace, id} = this.props.workspace;
    const {
      name,
      creatingConceptSet,
      conceptDomainList,
      conceptSetList,
      loadingResources,
      queries,
      resource,
      rType,
      selectedValues,
      valueSets,
      openSaveModal,
      nameTouched,
      conflictDataSetName,
      includesAllParticipants
    } = this.state;
    const currentResource = this.getCurrentResource();
    const errors = validate({name}, {
      name: {
        presence: {allowEmpty: false}
      }
    });
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
                <Subheader>Prepackaged Cohorts</Subheader>
                <ImmutableListItem name='All AoU Participants' onSelect={
                  () => this.setState({includesAllParticipants: !includesAllParticipants})
                }/>
                <Subheader>Workspace Cohorts</Subheader>
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
              <div style={{width: '60%', borderRight: '1px solid #E5E5E5'}}>
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
              <div style={{width: '40%'}}>
                <div style={styles.selectBoxHeader}>
                  Values
                </div>
                <div style={{height: '10rem', overflowY: 'auto'}}>
                  {valueSets.map(valueSet =>
                    <div key={valueSet.domain} style={{marginLeft: '0.5rem'}}>
                      <div style={{fontSize: '13px', fontWeight: 600, color: 'black'}}>
                        {fp.capitalize(valueSet.domain.toString())}
                      </div>
                      {valueSet.values.items.map(domainValue =>
                        <ValueListItem key={domainValue.value} domainValue={domainValue} onSelect={
                          () => this.selectDomainValue(valueSet.domain, domainValue)}
                          checked={fp.some({domain: valueSet.domain, value: domainValue.value},
                            selectedValues)}/>
                      )}
                    </div>)
                  }
                </div>
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
            <Button style={{position: 'absolute', right: '8rem', top: '.25rem'}}
                    onClick={() => {
                      this.generateCode();
                    }}>
              GENERATE CODE
            </Button>
            {/* Button disabled until this functionality added*/}
            <Button style={{position: 'absolute', right: '1rem', top: '.25rem'}}
                    onClick ={() => this.setState({openSaveModal: true, nameRequired: false,
                      conflictDataSetName: false, missingDataSetInfo: false})}
                    disabled={this.disableSave()}>
              SAVE DATASET
            </Button>
          </div>
          {/*TODO: Display dataset preview*/}
          <div style={{height: '8rem'}}>
            {queries.map(query =>
              <React.Fragment>
                <div>sql={'"' + query.query + '"'}</div>
                <div>
                  query_config = {'{'} <br />
                  &nbsp;&nbsp;{'\''}query{'\''}: {'{'} <br />
                  &nbsp;&nbsp;&nbsp;&nbsp;{'\''}parameterMode{'\''}: {'\''}NAMED{'\''}, <br />
                  &nbsp;&nbsp;&nbsp;&nbsp;{'\''}queryParameters{'\''}: [
                {query.namedParameters.map((np) => {
                  return this.buildQueryConfig(np);
                }
                )
                }
                  &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;]
                  {'}'}
                  {'}'}
                </div>
                <div>
                  df = pandas.read_gbq(sql, dialect="standard", configuration=query_config)
                </div>
              </React.Fragment>)}
          </div>
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
      {openSaveModal && <Modal>
        <ModalTitle>Save Dataset</ModalTitle>
        <ModalBody>
          <div>
             <ValidationError>
              {summarizeErrors(nameTouched && errors && errors.name)}
            </ValidationError>
            {conflictDataSetName &&
            <AlertDanger>DataSet with same name exist</AlertDanger>
            }
            {this.state.missingDataSetInfo &&
            <AlertDanger> Data state cannot save as some information is missing</AlertDanger>
            }
            <TextInput type='text' autoFocus placeholder='Dataset Name'
                       value = {this.state.name}
                       onChange={v => this.setState({name: v, nameTouched: true,
                         conflictDataSetName: false})}/>
          </div>
        </ModalBody>
        <ModalFooter>
          <Button onClick = {() => this.setState({openSaveModal: false})}
                  type='secondary' style={{marginRight: '2rem'}}>
            Cancel
          </Button>
          <Button type='primary' onClick={() => this.saveDataSet()}>SAVE</Button>
        </ModalFooter>
      </Modal>}
    </React.Fragment>;
  }


});

@Component({
  template: '<div #root></div>'
})
export class DataSetPageComponent extends ReactWrapperBase {
  constructor() {
    super(DataSetPage, []);
  }
}
