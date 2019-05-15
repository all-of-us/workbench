import {Component} from '@angular/core';
import * as fp from 'lodash/fp';
import * as React from 'react';


import {Button, Clickable} from 'app/components/buttons';
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
import colors from 'app/styles/colors';
import {ReactWrapperBase, toggleIncludes, withCurrentWorkspace, withUrlParams} from 'app/utils';
import {navigate, navigateByUrl} from 'app/utils/navigation';
import {convertToResource, ResourceType} from 'app/utils/resourceActionsReact';
import {WorkspaceData} from 'app/utils/workspace-data';
import {CreateConceptSetModal} from 'app/views/conceptset-create-modal/component';
import {ConfirmDeleteModal} from 'app/views/confirm-delete-modal/component';
import {EditModal} from 'app/views/edit-modal/component';
import {NewDataSetModal} from 'app/views/new-dataset-modal/component';
import {
  Cohort,
  ConceptSet,
  DataSet,
  DataSetPreviewList,
  Domain,
  DomainInfo,
  DomainValue,
  DomainValuePair,
  DomainValuesResponse,
  RecentResource,
  ValueSet,
  WorkspaceAccessLevel,
} from 'generated/fetch';
import {Column} from 'primereact/column';
import {DataTable} from 'primereact/datatable';

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
    backgroundColor: colors.green[1]
  },

  subheader: {
    fontWeight: 400,
    fontSize: '0.6rem',
    marginTop: '0.5rem',
    color: colors.purple[0]
  }
};

const Subheader = (props) => {
  return <div style={{...styles.subheader, ...props.style}}>{props.children}</div>;
};

export const ValueListItem: React.FunctionComponent <
  {domainValue: DomainValue, onChange: Function, checked: boolean}> =
  ({domainValue, onChange, checked}) => {
    return <div style={{display: 'flex', color: 'black', height: '1.2rem'}}>
      <input type='checkbox' value={domainValue.value} onChange={() => onChange()}
             style={styles.valueListItemCheckboxStyling} checked={checked}/>
      <div style={{lineHeight: '1.5rem', wordWrap: 'break-word'}}>{domainValue.value}</div>
    </div>;
  };

interface Props {
  workspace: WorkspaceData;
  urlParams: any;
}

interface State {
  cohortList: Cohort[];
  confirmDeleting: boolean;
  conceptDomainList: DomainInfo[];
  conceptSetList: ConceptSet[];
  creatingConceptSet: boolean;
  dataSet: DataSet;
  dataSetTouched: boolean;
  editingResource: boolean;
  includesAllParticipants: boolean;
  loadingResources: boolean;
  openSaveModal: boolean;
  previewList: Array<DataSetPreviewList>;
  previewDataLoading: boolean;
  resource: RecentResource;
  rType: ResourceType;
  selectedCohortIds: number[];
  selectedConceptSetIds: number[];
  selectedPreviewDomain: string;
  selectedValues: DomainValuePair[];
  valueSets: ValueSet[];
  valuesLoading: boolean;
}

const DataSetPage = fp.flow(withCurrentWorkspace(), withUrlParams())(
  class extends React.Component<Props, State> {
    constructor(props) {
      super(props);
      this.state = {
        cohortList: [],
        confirmDeleting: false,
        conceptDomainList: undefined,
        conceptSetList: [],
        creatingConceptSet: false,
        dataSet: undefined,
        dataSetTouched: false,
        editing: false,
        includesAllParticipants: false,
        loadingResources: true,
        openSaveModal: false,
        previewList: [],
        previewDataLoading: false,
        resource: undefined,
        rType: undefined,
        selectedCohortIds: [],
        selectedConceptSetIds: [],
        selectedPreviewDomain: '',
        selectedValues: [],
        valueSets: [],
        valuesLoading: false
      };
    }

    get editing() {
      return this.props.urlParams.dataSetId !== undefined;
    }

    componentDidMount() {
      const {namespace, id} = this.props.workspace;
      this.loadResources();
      conceptsApi().getDomainInfo(namespace, id).then((response) => {
        this.setState({conceptDomainList: response.items});
      });
      if (this.editing) {
        dataSetApi().getDataSet(namespace, id, this.props.urlParams.dataSetId).then((response) => {
          this.setState({
            dataSet: response,
            includesAllParticipants: response.includesAllParticipants,
            selectedConceptSetIds: response.conceptSets.map(cs => cs.id),
            selectedCohortIds: response.cohorts.map(c => c.id),
            selectedValues: response.values,
            valuesLoading: true,
          });
          this.getValuesList(this.getDomainsFromConceptIds(response.conceptSets.map(cs => cs.id)))
            .then(valueSets => this.setState({valueSets: valueSets, valuesLoading: false}));
        });
      }
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
      this.setState({editingResource: true, resource: rc});
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
          return updatedResource.id === conceptSet.id ?
            (updatedResource as ConceptSet) : conceptSet;
        });
      }
      this.setState({conceptSetList: updatedConceptSetList, cohortList: updatedCohortList,
        resource: undefined, rType: undefined});
      call.then(() => this.closeEditModal());
    }

    closeEditModal(): void {
      this.setState({editingResource: false});
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
      this.setState({dataSetTouched: true});
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
        this.setState({
          selectedConceptSetIds: newSelectedConceptSets,
          selectedValues: updatedSelectedValues,
        });
        if (newDomains.length > 0) {
          this.setState({valuesLoading: true});
          this.getValuesList(newDomains)
            .then(newValueSets => this.setState({
              valueSets: updatedValueSets.concat(newValueSets),
              valuesLoading: false
            }));
        } else {
          this.setState({valueSets: updatedValueSets});
        }
      } else {
        this.setState({selectedCohortIds: toggleIncludes(resource.id,
          this.state.selectedCohortIds) as unknown as number[]});
      }
    }

    selectDomainValue(domain: Domain, domainValue: DomainValue): void {
      const valueSets = this.state.valueSets
          .filter(value => value.domain === domain)
          .map(valueSet => valueSet.values.items)[0];
      const origSelected = this.state.selectedValues;
      const selectObj = {domain: domain, value: domainValue.value};
      let valuesSelected = [];
      if (fp.some(selectObj, origSelected)) {
        valuesSelected = fp.remove((dv) => dv.domain === selectObj.domain
            && dv.value === selectObj.value, origSelected);

      } else {
        valuesSelected = (origSelected).concat(selectObj);
      }

      // Sort the values selected as per the order display rather than appending top end

      valuesSelected = valuesSelected.sort((a, b) =>
          valueSets.findIndex(({value}) => a.value === value) -
          valueSets.findIndex(({value}) => b.value === value));
      this.setState({selectedValues: valuesSelected, dataSetTouched: true});
    }

    getCurrentResource(): Cohort | ConceptSet {
      if (this.state.resource) {
        return fp.compact([this.state.resource.cohort, this.state.resource.conceptSet])[0];
      }
    }

    disableSave() {
      return !this.state.selectedConceptSetIds || this.state.selectedConceptSetIds.length === 0 ||
          ((!this.state.selectedCohortIds ||
          this.state.selectedCohortIds.length === 0) && !this.state.includesAllParticipants) ||
          !this.state.selectedValues || this.state.selectedValues.length === 0;
    }

    setSelectedPreviewDomain(domain) {
      this.setState({selectedPreviewDomain: domain});
    }

    getDataTableValue(data) {
      // convert data model from api :
      // [{value[0]: '', queryValue: []}, {value[1]: '', queryValue: []}]
      // to compatible with DataTable
      // {value[0]: queryValue[0], value[1]: queryValue[1]}

      const tableData = fp.flow(
        fp.map(({value, queryValue}) => fp.map(v => [value, v], queryValue)),
        fp.unzip,
        fp.map(fp.fromPairs)
      )(data);
      return tableData;
    }

    async getPreviewList() {
      this.setState({previewList: [], previewDataLoading: true});
      const {namespace, id} = this.props.workspace;
      const request = {
        name: '',
        description: '',
        conceptSetIds: this.state.selectedConceptSetIds,
        cohortIds: this.state.selectedCohortIds,
        values: this.state.selectedValues
      };
      try {
        const dataSetPreviewResp = await dataSetApi().previewQuery(namespace, id, request);
        this.setState({previewList: dataSetPreviewResp.domainValue,
          selectedPreviewDomain: dataSetPreviewResp.domainValue[0].domain});
      } catch (ex) {
        console.log(ex);
      } finally {
        this.setState({previewDataLoading: false});
      }
    }

    renderPreviewDataTable() {
      const filteredPreviewData =
          this.state.previewList.filter(
            preview => fp.contains(preview.domain, this.state.selectedPreviewDomain))[0];

      return <DataTable key={this.state.selectedPreviewDomain}
                        value={this.getDataTableValue(filteredPreviewData.values)}>
        {filteredPreviewData.values.map(value =>
            <Column header={value.value} headerStyle={{textAlign: 'left'}} field={value.value}/>
        )}
      </DataTable>;
    }

    updateDataSet() {
      const {namespace, id} = this.props.workspace;
      const {dataSet} = this.state;
      console.log('hello world');
      const request = {
        name: dataSet.name,
        description: dataSet.description,
        includesAllParticipants: this.state.includesAllParticipants,
        conceptSetIds: this.state.selectedConceptSetIds,
        cohortIds: this.state.selectedCohortIds,
        values: this.state.selectedValues,
        etag: dataSet.etag
      };
      dataSetApi().updateDataSet(namespace, id, dataSet.id, request)
        .then(() => window.history.back());
    }

    render() {
      const {namespace, id} = this.props.workspace;
      const {
        creatingConceptSet,
        conceptDomainList,
        conceptSetList,
        dataSet,
        dataSetTouched,
        includesAllParticipants,
        loadingResources,
        openSaveModal,
        previewDataLoading,
        previewList,
        resource,
        rType,
        selectedCohortIds,
        selectedConceptSetIds,
        selectedPreviewDomain,
        selectedValues,
        valuesLoading,
        valueSets
      } = this.state;
      const currentResource = this.getCurrentResource();
      return <React.Fragment>
        <FadeBox style={{marginTop: '1rem'}}>
          <h2 style={{marginTop: 0}}>Datasets{this.editing &&
            dataSet !== undefined && ' - ' + dataSet.name}</h2>
          <div style={{color: '#000000', fontSize: '14px'}}>Build a dataset by selecting the
            variables and values for one or more of your cohorts. Then export the completed dataset
            to Notebooks where you can perform your analysis</div>
          <div style={{display: 'flex'}}>
            <div style={{width: '33%'}}>
              <h2>Select Cohorts</h2>
              <div style={{backgroundColor: 'white', border: '1px solid #E5E5E5'}}>
                <div style={styles.selectBoxHeader}>
                  Cohorts
                  <ClrIcon shape='plus-circle' class='is-solid' style={styles.addIcon}
                    onClick={() => navigate(['workspaces', namespace, id,  'cohorts', 'build'])}/>
                </div>
                <div style={{height: '10rem', overflowY: 'auto'}}>
                  <Subheader>Prepackaged Cohorts</Subheader>
                  <ImmutableListItem name='All AoU Participants' checked={includesAllParticipants}
                                     onChange={
                                       () => this.setState({
                                         includesAllParticipants: !includesAllParticipants,
                                         dataSetTouched: true
                                       })}/>
                  <Subheader>Workspace Cohorts</Subheader>
                  {!loadingResources && this.state.cohortList.map(cohort =>
                    <ResourceListItem key={cohort.id} resource={cohort} rType={ResourceType.COHORT}
                                      data-test-id='cohort-list-item'
                                      openConfirmDelete={
                                        () => {
                                          return this.openConfirmDelete(
                                            cohort, ResourceType.COHORT);
                                        }
                                      }
                                      checked={selectedCohortIds.includes(cohort.id)}
                                      onChange={
                                        () => this.select(cohort, ResourceType.COHORT)
                                      }
                                      onEdit={
                                        () => {
                                          const url =
                                            '/workspaces/' + namespace + '/' +
                                            id + '/cohorts/build?cohortId=';
                                          navigateByUrl(url + cohort.id);
                                        }
                                      }
                                      onRenameCohort={
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
            <div style={{marginLeft: '1.5rem', width: '65%'}}>
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
                                          checked={selectedConceptSetIds.includes(conceptSet.id)}
                                          openConfirmDelete={
                                            () => {
                                              return this.openConfirmDelete(conceptSet,
                                                ResourceType.CONCEPT_SET);
                                            }
                                          }
                                          onChange={
                                            () => this.select(conceptSet, ResourceType.CONCEPT_SET)
                                          }
                                          onEdit={
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
                    {valuesLoading && <Spinner style={{position: 'relative',
                      top: '2rem', left: 'calc(50% - 36px)'}}/>}
                    {valueSets.map(valueSet =>
                      <div key={valueSet.domain} style={{marginLeft: '0.5rem'}}>
                        <div style={{fontSize: '13px', fontWeight: 600, color: 'black'}}>
                          {fp.capitalize(valueSet.domain.toString())}
                        </div>
                        {valueSet.values.items.map(domainValue =>
                          <ValueListItem data-test-id='value-list-items'
                            key={domainValue.value} domainValue={domainValue}
                            onChange={() => this.selectDomainValue(valueSet.domain, domainValue)}
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
            <div style={{...styles.selectBoxHeader, display: 'flex', flexDirection: 'row',
              position: 'relative'}}>
              <div style={{color: '#000000', fontSize: '14px'}}>A visualization
                of your data table based on the variable and value you selected above</div>
              <Button data-test-id='preview-button' style={{position: 'absolute', right: '8rem',
                top: '0.25rem'}}
                      disabled={this.disableSave()} onClick={() => {this.getPreviewList(); }}>
                PREVIEW DATA SET
              </Button>
              <Button data-test-id='save-button' style={{position: 'absolute', right: '1rem',
                top: '.25rem'}} onClick ={this.editing ? () => this.updateDataSet() :
                () => this.setState({openSaveModal: true})}
                disabled={this.disableSave() || (this.editing && !dataSetTouched)}>
                {this.editing ? 'UPDATE DATA SET' : 'SAVE DATA SET'}
              </Button>
            </div>
            {previewDataLoading && <div style={{display: 'flex',
              flexDirection: 'column',
              alignItems: 'center'}}>
              <Spinner style={{position: 'relative', top: '2rem'}} />
              <div style={{top: '3rem', position: 'relative'}}>
                It may take up to a minute to load the data
              </div>
            </div>}
            {previewList.length > 0 &&
              <div style={{display: 'flex', flexDirection: 'column'}}>
                <div style={{display: 'flex', flexDirection: 'row'}}>
                  {previewList.map(previewRow =>
                     <Clickable key={previewRow.domain}
                               onClick={() => this.setSelectedPreviewDomain(previewRow.domain)}
                               style={{
                                 lineHeight: '32px', fontSize: '18px',
                                 fontWeight: (selectedPreviewDomain === previewRow.domain)
                                     ? 600 : 400,
                                 textDecoration:
                                     (selectedPreviewDomain === previewRow.domain) ?
                                       'underline' : ''
                               }}>
                       <div key={previewRow.domain}
                           style={{
                             marginLeft: '0.2rem', color: colors.blue[0], paddingRight: '3rem'
                           }}>
                         {previewRow.domain}
                       </div>
                     </Clickable>
                  )}
                </div>
                {this.renderPreviewDataTable()}
              </div>
            }
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
        {this.state.editingResource && resource &&
        <EditModal resource={resource}
                   onEdit={e => this.receiveEdit(e)}
                   onCancel={() => this.closeEditModal()}/>}
        {openSaveModal && <NewDataSetModal includesAllParticipants={includesAllParticipants}
                                           selectedConceptSetIds={selectedConceptSetIds}
                                           selectedCohortIds={selectedCohortIds}
                                           selectedValues={selectedValues}
                                           workspaceNamespace={namespace}
                                           workspaceId={id}
                                           closeFunction={() => {
                                             this.setState({openSaveModal: false});
                                           }}
        />}
      </React.Fragment>;
    }
  });

export {
  DataSetPage,
  Props as DataSetPageProps
};

@Component({
  template: '<div #root></div>'
})
export class DataSetPageComponent extends ReactWrapperBase {
  constructor() {
    super(DataSetPage, []);
  }
}
